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

    val appName = "ENRON-etl"
    val conf = new SparkConf().setAppName(appName)

    conf.set("spark.driver.maxResultSize", "2g")
    conf.set("spark.yarn.executor.memoryOverhead", "12000") //deafult: driverMemory * 0.10, with minimum of 384
    conf.set("spark.yarn.driver.memoryOverhead", "12000")
    conf.set("spark.task.maxFailures", "2") // Number of allowed retries = this value - 1.
    val sc = new SparkContext(conf)
    val storageLvl = StorageLevel.MEMORY_AND_DISK_SER_2

    // scalastyle:off method.length
    def main(args: Array[String]): Unit = {

        // val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)
        val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)//.sample(withReplacement = false, .1, 42L)
        allExtracted.persist(storageLvl)

        // get custodians from csv file stored in HDFS
        val csv = sc.textFile(Commons.ENRON_CUSTODIANS_CSV_HDFS).map { line => line.split(",") }
        val custodians = sc.broadcast(csv.map {
            record => Custodian(record(0), record(1), Option(record(2)))
        }.collect().toSeq)

        // parse emails
        val allParsed: RDD[MailBox] = allExtracted.map { case (mailbox, emails) =>
            val parsedEmails = emails flatMap { email =>
                try Some(EmailParser.parse(email, custodians.value))
                catch {
                    case e: EmailParsingException =>
                        println(e)
                        None
                }
            }

            // try to filter empty/short body emails
            val longEmails = parsedEmails.filter(_.body.length > 10)
            println(s"Using ${longEmails.length} over ${parsedEmails.length}")
            MailBox(mailbox, longEmails)
        }.persist(storageLvl)

        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        val dfFull = allParsed.toDF().persist(storageLvl)

        // this will print on the executors
        // TODO fix parsing! some emails contain the footer and a LOT have empty body
        dfFull.select('emails).flatMap(r => r.getSeq[Email](0)).sample(withReplacement = false, .1, 42L).foreach(println)

        dfFull.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_FULL_DATAFRAME)

        // repartition allParsed (increase partitions)
        println(s"N partitions before resize: ${allParsed.partitions.length}")
        val repartitioned = allParsed.repartition(allParsed.partitions.length * 10).persist(storageLvl)
        println(s"N partitions after resize: ${repartitioned.partitions.length}")

        allExtracted.unpersist()
        allParsed.unpersist()
        dfFull.unpersist()


        // classify sentiment and save w/o body
        val mailboxesSentiment = repartitioned.map { mailbox =>

            // annotation
            val emailsWithSentiment = mailbox.emails.map { email =>

                // load sentiment annotator pipeline
                @transient lazy val nlpProps = new Properties
                nlpProps.setProperty("annotators", "tokenize, ssplit, pos, parse, lemma, sentiment")
                nlpProps.setProperty("tokenize.options", "untokenizable=allDelete")
                nlpProps.setProperty("tokenize.options", "ptb3Escaping=true")
                nlpProps.setProperty("pos.maxlen", "50")
                nlpProps.setProperty("parse.maxlen", "50")
                @transient lazy val pipeline = new StanfordCoreNLP(nlpProps)

                val document = new Annotation(email.body.take(50))

                pipeline.annotate(document)

                val sentences = document.get(classOf[SentencesAnnotation])

                val sentiments = sentences.toList.map { sentence =>
                    val tree = sentence.get(classOf[SentimentCoreAnnotations.AnnotatedTree])
                    val sentenceSentiment = RNNCoreAnnotations.getPredictedClass(tree)
                    sentenceSentiment.toDouble
                }

                // TODO because some emails have empty body, the array is empty, so 0 / 0.0 = NaN
                val sentiment = sentiments.sum / sentiments.length.toDouble
                val sentimentRatio = Commons.pnRatio(sentiments)

                println(s"extraction: ${mailbox.name} \t snt%:$sentiment \t sum:${sentiments.sum} " +
                  s"\t len:${sentiments.length} \t ratio:$sentimentRatio \t subj:${email.subject}")

                EmailWithSentiment(email.date, email.from, email.to ++ email.cc ++ email.bcc, email.subject, sentiment)
            }

            MailBoxWithSentiment(mailbox.name, emailsWithSentiment)
        }.persist(storageLvl)

        val dfSentiment = mailboxesSentiment.toDF().persist(storageLvl)

        dfSentiment.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_SENTIMENT_DATAFRAME)
    }

}
