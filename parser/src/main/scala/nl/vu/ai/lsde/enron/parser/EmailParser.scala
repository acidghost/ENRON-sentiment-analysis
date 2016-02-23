package nl.vu.ai.lsde.enron.parser

import java.text.SimpleDateFormat
import nl.vu.ai.lsde.enron.Email

object EmailParser {

    val datePattern = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z '('z')'")
    val enronDatasetFooter = "\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\nEDRM Enron Email Data Set has been produced in EML"

    def parse(text: String): Email = {
        val (headers, body) = text.split("\n\n") match {
            case s: Array[String] => (s.head.split("\n"), s.tail.mkString("\n\n"))
        }

        val bodyNoFooter = body.split(enronDatasetFooter) match { case s: Array[String] => s.head }

        val date = filterHeader(headers, "Date: ") match {
            case Some(s) =>
                try Some(new java.sql.Timestamp(datePattern.parse(s).getTime))
                catch { case e: java.text.ParseException => None }
            case _ => None
        }

        if (date.isEmpty) throw new EmailParsingException(s"Unable to parse DATE header in email:\n$text")

        val from = filterHeaderList(headers, "From: ")
        if (from.isEmpty) throw new EmailParsingException(s"Unable to parse FROM header in email:\n$text")

        val to = filterHeaderList(headers, "To: ")

        val cc = filterHeaderList(headers, "Cc: ")
        val bcc = filterHeaderList(headers, "Bcc: ")

        val subject = filterHeader(headers, "Subject: ")
        if (subject.isEmpty) throw new EmailParsingException(s"Unable to parse SUBJECT header in email:\n$text")

        Email(date.get, from.get, to, cc, bcc, subject.get, bodyNoFooter)
    }

    private def filterHeader(headers: Seq[String], filter: String): Option[String] = {
        headers.filter(h => h.startsWith(filter)) match {
            case Seq() => None
            case x: Seq[String] =>
                try Some(x.head.split(filter)(1))
                catch { case e: java.lang.ArrayIndexOutOfBoundsException => None }
        }
    }

    private def filterHeaderList(headers: Seq[String], filter: String): Option[Seq[String]] = {
        filterHeader(headers, filter) match {
            case Some(s) => Some(s.split(",").map(_.trim))
            case _ => None
        }
    }

    type EmailParsingException = RuntimeException

}
