package nl.vu.ai.lsde.enron.parser

import java.text.SimpleDateFormat

import nl.vu.ai.lsde.enron.Email


object EmailHeadersParser {

    val datePattern = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z '('z')'")

    def parse(headersTxt: String): Email = {
        val headers = headersTxt.split("\n")

        val date = filterHeader(headers, "Date: ") match {
            case Some(s) => Some(new java.sql.Date(datePattern.parse(s).getTime))
            case _ => None
        }

        val from = filterHeaderList(headers, "From: ")
        val to = filterHeaderList(headers, "To: ")
        val cc = filterHeaderList(headers, "Cc: ")
        val bcc = filterHeaderList(headers, "Bcc: ")

        val subject = filterHeader(headers, "Subject: ")

        Email(date, from, to, cc, bcc, subject, "")
    }

    private def filterHeader(headers: Seq[String], filter: String): Option[String] = {
        headers.filter(h => h.startsWith(filter)) match {
            case Seq() => None
            case x: Seq[String] => Some(x.head.split(filter)(1))
        }
    }

    private def filterHeaderList(headers: Seq[String], filter: String): Option[Seq[String]] = {
        filterHeader(headers, filter) match {
            case Some(s) => Some(s.split(",").map(_.trim))
            case _ => None
        }
    }

}
