package nl.vu.ai.lsde.enron.spam

import nl.vu.ai.lsde.enron.{Custodian, Commons, Email, MailBox}
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.mllib.feature.{HashingTF, IDFModel}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.{Row, SQLContext, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}


/**
  * This program classifies the full ENRON corpus for spam/ham messages.
  * It then saves a spam-filtered copy of the dataframe on disk.
  */
object SpamFilterDriver {

    def main (args: Array[String]) {
        val appName = "ENRON-spam-filter"
        val conf = new SparkConf().setAppName(appName)
        val sc = new SparkContext(conf)

        // load TF, IDF and Naive Bayes models
        val tf = sc.objectFile[HashingTF](Commons.ENRON_SPAM_TF).first()
        val idf = sc.objectFile[IDFModel](Commons.ENRON_SPAM_IDF).first()
        val model = NaiveBayesModel.load(sc, Commons.ENRON_SPAM_MODEL)

        val bTf = sc.broadcast[HashingTF](tf)
        val bIdf = sc.broadcast[IDFModel](idf)


        // maps a SQL Row to a TokenizedMailBox using the Tokenizer
        def tokenizeMailbox(row: Row): TokenizedMailBox = {
            val emails = row.getSeq[Row](1).map { s: Row =>
                Email(s.getTimestamp(0), s.getSeq[Custodian](1),
                    s.getSeq[Custodian](2), s.getSeq[Custodian](3),
                    s.getSeq[Custodian](4), s.getString(5), s.getString(6))
            }
            (row.getString(0), emails.map(e => (e, Tokenizer.tokenize(e.body))))
        }

        // maps a TokenizedMailBox to a VectorizedMailBox where vectors are term frequencies
        def toTF(x: TokenizedMailBox): VectorizedMailBox = x match {
            case tokenizedMailbox: TokenizedMailBox =>
                (tokenizedMailbox._1, tokenizedMailbox._2 map { e => (e._1, bTf.value.transform(e._2)) })
        }

        // maps a VectorizedMailBox of TFs to a VectorizedMailBox of TF-IDFs
        def toTFIDF(x: VectorizedMailBox): VectorizedMailBox = x match {
            case tfMessages: VectorizedMailBox =>
                (tfMessages._1, tfMessages._2 map { e => (e._1, bIdf.value.transform(e._2)) })
        }

        // classifies messages in VectorizedMailBox, filters out spam and returns a
        // collection of MailBox of ham messages
        def filterSpam(x: VectorizedMailBox): MailBox = {
            val emailsClassified = x._2.map(e => EmailClassified(model.predictProbabilities(e._2)(0), e._1))
            val emailsHam = emailsClassified.filter(_.spamProb < .9).map(_.email)
            MailBox(x._1, emailsHam)
        }


        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        // load full dataframe
        val df = sqlContext.read.parquet(Commons.ENRON_DATAFRAME)

        println("Dataset before spam filtering")
        df.select('name, 'emails).map(r => (r.getString(0), r.getSeq(1).length)).collect().foreach(println)
        println("\n")

        // transform full dataframe into only ham dataframe
        val tokenized = df.select('name, 'emails).map(tokenizeMailbox)
        val tfMessages = tokenized.map(toTF)
        val tfidfMessages = tfMessages.map(toTFIDF)
        val hamMessages = tfidfMessages.map(filterSpam)

        val hamDF = hamMessages.toDF()
        println("Dataset after spam filtering")
        hamDF.select('name, 'emails).map(r => (r.getString(0), r.getSeq(1).length)).collect().foreach(println)

        // save ham dataframe on disk
        hamDF.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_DATAFRAME_HAM)

    }

    type TokenizedMailBox = (String, Seq[(Email, Seq[String])])
    type VectorizedMailBox = (String, Seq[(Email, Vector)])
    private case class EmailClassified(spamProb: Double, email: Email)

}
