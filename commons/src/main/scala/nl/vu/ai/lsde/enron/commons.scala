package nl.vu.ai.lsde.enron

import java.sql.Timestamp

import org.apache.hadoop.fs.{Path, FileSystem}

object Commons {

    val SOURCE_ENRON_DATA = "hdfs:///user/hannesm/lsde/enron"

    val LSDE_USER_SPACE = "hdfs:///user/lsde03"
    val LSDE_ENRON = s"$LSDE_USER_SPACE/enron"
    val ENRON_EXTRACTED_TXT = s"$LSDE_ENRON/extracted_txt"
    val ENRON_DATAFRAME = s"$LSDE_ENRON/dataframe.parquet"
    val ENRON_CUSTODIANS_CSV = s"$LSDE_ENRON/custodians_def.csv"
    val ENRON_SPAM_DATA = s"$LSDE_ENRON/spam-dataset"
    val ENRON_SPAM_TF = s"$LSDE_ENRON/spam-tf"
    val ENRON_SPAM_IDF = s"$LSDE_ENRON/spam-idf"
    val ENRON_SPAM_MODEL = s"$LSDE_ENRON/spam-model"
    val ENRON_DATAFRAME_HAM = s"$LSDE_ENRON/dataframe-ham.parquet"

    /**
      * Delete a folder from HDFS.
      * @param path The folder to be removed
      */
    def deleteFolder(path: String): Unit = {
        try { hdfs.delete(new Path(path), true) }
        catch { case _ : Throwable => }
    }

    /**
      * Gets the HDFS filesystem
      * @return The HDFS filesystem
      */
    def hdfs: FileSystem = {
        val hadoopConf = new org.apache.hadoop.conf.Configuration()
        FileSystem.get(new java.net.URI("hdfs:///"), hadoopConf)
    }
}



case class Custodian(dirName: String,
                     completeName: String,
                     role: Option[String])

case class Email(date: Timestamp,
                 from: Seq[Custodian],
                 to: Seq[Custodian],
                 cc: Seq[Custodian],
                 bcc: Seq[Custodian],
                 subject: String,
                 body: String)

case class MailBox(name: String, emails: Seq[Email])
