package nl.vu.ai.lsde.enron.parser

import java.text.SimpleDateFormat

import nl.vu.ai.lsde.enron.Email


object EmailParser {

    val datePattern = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z '('z')'")

    def parse(text: String): Email = {
        val (headers, body) = text.split("\n\n") match {
            case s => (s.head.split("\n"), s.tail.mkString("\n\n"))
        }

        val bodyNoFooter = body.split("\n\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*") match { case s => s.head }

        val date = filterHeader(headers, "Date: ") match {
            case Some(s) =>
                try Some(new java.sql.Date(datePattern.parse(s).getTime))
                catch { case e: java.text.ParseException => None }
            case _ => None
        }

        val from = filterHeaderList(headers, "From: ")
        val to = filterHeaderList(headers, "To: ")
        val cc = filterHeaderList(headers, "Cc: ")
        val bcc = filterHeaderList(headers, "Bcc: ")

        val subject = filterHeader(headers, "Subject: ")

        Email(date, from, to, cc, bcc, subject, bodyNoFooter)
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

}
