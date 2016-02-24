package nl.vu.ai.lsde.enron.spam

import nl.vu.ai.lsde.enron.{Commons, Email, MailBox}
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.mllib.feature.{HashingTF, IDFModel}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.{Row, SQLContext, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}


object SpamFilterDriver {

    def main (args: Array[String]) {
        val appName = "ENRON-spam-filter"
        val conf = new SparkConf().setAppName(appName)
        val sc = new SparkContext(conf)

        val tf = sc.objectFile[HashingTF](Commons.ENRON_SPAM_TF).first()
        val idf = sc.objectFile[IDFModel](Commons.ENRON_SPAM_IDF).first()
        val model = NaiveBayesModel.load(sc, Commons.ENRON_SPAM_MODEL)

        val bTf = sc.broadcast[HashingTF](tf)
        val bIdf = sc.broadcast[IDFModel](idf)


        def tokenizeMailbox(row: Row): (String, Seq[(Email, Seq[String])]) = {
            val emails = row.getSeq[Row](1).map { s: Row =>
                Email(s.getTimestamp(0), s.getSeq[String](1),
                    s.getSeq[String](2), s.getSeq[String](3),
                    s.getSeq[String](4), s.getString(5), s.getString(6))
            }
            (row.getString(0), emails.map(e => (e, Tokenizer.tokenize(e.body))))
        }

        def toTF(x: (String, Seq[(Email, Seq[String])])): (String, Seq[(Email, Vector)]) = x match {
            case tokenizedMailbox: (String, Seq[(Email, Seq[String])]) =>
                (tokenizedMailbox._1, tokenizedMailbox._2 map { e => (e._1, bTf.value.transform(e._2)) })
        }

        def toTFIDF(x: (String, Seq[(Email, Vector)])): (String, Seq[(Email, Vector)]) = x match {
            case tfMessages: (String, Seq[(Email, Vector)]) =>
                (tfMessages._1, tfMessages._2 map { e => (e._1, bIdf.value.transform(e._2)) })
        }

        def filterSpam(x: (String, Seq[(Email, Vector)])): MailBox = {
            val emailsClassified = x._2.map(e => EmailLabeledVectorized(model.predict(e._2), e._2, e._1))
            val emailsHam = emailsClassified.filter(_.label == 1).map(_.email)
            MailBox(x._1, emailsHam)
        }


        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        val df = sqlContext.read.parquet(Commons.ENRON_DATAFRAME)
        val tokenized = df.select('name, 'emails).map(tokenizeMailbox)
        val tfMessages = tokenized.map(toTF)
        val tfidfMessages = tfMessages.map(toTFIDF)
        val hamMessages = tfidfMessages.map(filterSpam)

        val hamDF = hamMessages.toDF()
        hamDF.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_DATAFRAME_HAM)

    }

    private case class EmailLabeledVectorized(label: Double, vector: Vector, email: Email)

}
