package nl.vu.ai.lsde

import java.sql.Timestamp

import org.apache.hadoop.fs.{Path, FileSystem}


package object enron {

    object Commons {

        val SOURCE_ENRON_DATA = "hdfs:///user/hannesm/lsde/enron"

        val LSDE_USER_SPACE = "hdfs:///user/lsde03"
        val LSDE_ENRON = s"$LSDE_USER_SPACE/enron"
        val ENRON_EXTRACTED_TXT = s"$LSDE_ENRON/extracted_txt"
        val ENRON_DATAFRAME = s"$LSDE_ENRON/dataframe.parquet"
        val ENRON_SPAM_DATA = s"$LSDE_ENRON/spam-dataset"
        val ENRON_SPAM_TF = s"$LSDE_ENRON/spam-tf"
        val ENRON_SPAM_IDF = s"$LSDE_ENRON/spam-idf"
        val ENRON_SPAM_MODEL = s"$LSDE_ENRON/spam-model"
        val ENRON_DATAFRAME_HAM = s"$LSDE_ENRON/dataframe-ham.parquet"

        def deleteFolder(path: String): Unit = {
            try { hdfs.delete(new Path(path), true) }
            catch { case _ : Throwable => }
        }

        def hdfs: FileSystem = {
            val hadoopConf = new org.apache.hadoop.conf.Configuration()
            FileSystem.get(new java.net.URI("hdfs:///"), hadoopConf)
        }

    }

    case class Email(date: Timestamp,
                     from: Seq[String],
                     to: Option[Seq[String]],
                     cc: Option[Seq[String]],
                     bcc: Option[Seq[String]],
                     subject: String,
                     body: String)

    case class MailBox(name: String, emails: Seq[Email])

}
