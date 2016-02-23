package nl.vu.ai.lsde

import java.sql.Timestamp


package object enron {

    object Commons {

        val SOURCE_ENRON_DATA = "hdfs:///user/hannesm/lsde/enron"

        val LSDE_USER_SPACE = "hdfs:///user/lsde03"
        val LSDE_ENRON = s"$LSDE_USER_SPACE/enron"
        val ENRON_EXTRACTED_TXT = s"$LSDE_ENRON/extracted_txt"
        val ENRON_DATAFRAME = s"$LSDE_ENRON/dataframe.parquet"

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
