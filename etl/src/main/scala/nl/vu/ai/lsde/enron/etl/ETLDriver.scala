package nl.vu.ai.lsde.enron.etl

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import nl.vu.ai.lsde.enron._
import nl.vu.ai.lsde.enron.etl.EmailParser.EmailParsingException
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConversions._


object ETLDriver {

    import org.apache.log4j.{Level, Logger}
    val level = Level.WARN
    Logger.getLogger("org").setLevel(level)

    val appName = "ENRON-etl"
    val conf = new SparkConf().setAppName(appName)

    conf.set("spark.memory.offHeap.enabled", "true")
    conf.set("spark.memory.offHeap.size", "2g")

    // enable Kryo serializer for Core NLP
//    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
//    conf.set("spark.kryoserializer.buffer.max", "512m")
//    conf.registerKryoClasses(Array(classOf[Properties], classOf[StanfordCoreNLP]))

    // driver conf
//    conf.set("spark.driver.cores", "2")
//    conf.set("spark.driver.maxResultSize", "2g") // Limit of total size of serialized results of all partitions for each Spark action (e.g. collect)
//    conf.set("spark.driver.memory", "2g")

    // executors conf
//    conf.set("spark.executor.memory", "2g") // heap size
//    conf.set("spark.executor.cores", "2")

    val sc = new SparkContext(conf)

    // scalastyle:off method.length
    def main(args: Array[String]): Unit = {


        // Testing on a sub-sample
        val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT).sample(withReplacement = false, .1, 42L)
        allExtracted.persist(StorageLevel.OFF_HEAP)

        // get custodians from csv file stored in HDFS
        val csv = sc.textFile(Commons.ENRON_CUSTODIANS_CSV_HDFS).map { line => line.split(",") }
        val custodians = sc.broadcast(csv.map { record => Custodian(record(0), record(1), Option(record(2))) }.collect().toSeq)

        // parse emails
        val allParsed: RDD[MailBox] = allExtracted.map { case (mailbox, emails) =>
            val parsedEmails = emails flatMap { email =>
                try Some(EmailParser.parse(email, custodians.value))
                catch {
                    case e: EmailParsingException => None
                }
            }

            MailBox(mailbox, parsedEmails)
        }
        allParsed.persist(StorageLevel.OFF_HEAP)

        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        val dfFull = allParsed.toDF()
        dfFull.persist(StorageLevel.OFF_HEAP)

        println("\n\n\n*******************")
        println("dfFull")
        println("*******************")
        dfFull.printSchema()
        dfFull.show()
        println("*******************")

        // this will print on the executors
        // TODO fix parsing! some emails contain the footer and a LOT have empty body
        dfFull.select('emails).flatMap(r => r.getSeq[Email](0)).sample(withReplacement = false, .1, 42L).foreach(println)

        dfFull.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_FULL_DATAFRAME)
        dfFull.unpersist()
        allExtracted.unpersist()

        // repartition allParsed (increase partitions)
        println(s"N partitions before resize: ${allParsed.partitions.length}")
        val repartitioned = allParsed.repartition(allParsed.partitions.length * 10)
        println(s"N partitions after resize: ${repartitioned.partitions.length}")
        repartitioned.persist(StorageLevel.OFF_HEAP)

        // classify sentiment and save w/o body
        val mailboxesSentiment = repartitioned.map { mailbox =>
            // load sentiment annotator pipeline
            val nlpProps = new Properties
            nlpProps.setProperty("annotators", "tokenize, ssplit, pos, parse, lemma, sentiment")
            val pipeline = new StanfordCoreNLP(nlpProps)

            // annotation
            val emailsWithSentiment = mailbox.emails.map { email =>
                val document = new Annotation(email.body)
                pipeline.annotate(document)

                val sentences = document.get(classOf[SentencesAnnotation])

                val sentiments = sentences.toList.map { sentence =>
                    val tree = sentence.get(classOf[SentimentCoreAnnotations.AnnotatedTree])
                    val sentenceSentiment = RNNCoreAnnotations.getPredictedClass(tree)
                    sentenceSentiment
                }

                // TODO because some emails have empty body, the array is empty, so 0 / 0 = NaN
                val sentiment = sentiments.sum / sentiments.length.toDouble

                println(s"${mailbox.name} - $sentiment - ${email.subject}")
                EmailWithSentiment(email.date, email.from, email.to ++ email.cc ++ email.bcc, email.subject, sentiment)
            }

            MailBoxWithSentiment(mailbox.name, emailsWithSentiment)
        }
        mailboxesSentiment.persist(StorageLevel.OFF_HEAP)

        val dfSentiment = mailboxesSentiment.toDF()
        dfSentiment.persist(StorageLevel.OFF_HEAP)

        println("\n\n\n*******************")
        println("dfSentiment")
        println("*******************")
        dfSentiment.printSchema()
//        dfSentiment.show()
        println("*******************")

        dfSentiment.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_SENTIMENT_DATAFRAME)

    }

}
