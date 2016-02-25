package nl.vu.ai.lsde.enron.parser

import java.text.SimpleDateFormat

import nl.vu.ai.lsde.enron.{Custodian, Email}

object EmailParser {

    val datePattern = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z '('z')'")
    val enronDatasetFooter = "\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\nEDRM Enron Email Data Set has been produced in EML"
    val forwardedBy = ("\\s?---------------------- Forwarded by ", "(\\w*\\s)?---------------------------")
    val forwardedByReg = forwardedBy._1 + "(.*)\\n" + forwardedBy._2
    val subjectFwd = "^F[Ww]: [\\w\\W\\s]*$"
    val subjectReply = "^R[Ee]: [\\w\\W\\s]*$"
    val originalMsg = ">?\\s? -----Original Message-----"


    def parse(text: String): Email = {
        // get headers and body
        val (headers, body) = text.split("\n\n") match {
            case s: Array[String] => (s.head.split("\n"), s.tail.mkString("\n\n"))
        }

        // get date
        val date = filterHeader(headers, "Date: ") match {
            case Some(s) =>
                try Some(new java.sql.Timestamp(datePattern.parse(s).getTime))
                catch { case e: java.text.ParseException => None }
            case _ => None
        }
        if (date.isEmpty) throw new EmailParsingException(s"Unable to parse DATE header in email:\n$text")

        // get header entries
        val from = filterHeaderList(headers, "From: ")
        if (from.isEmpty) throw new EmailParsingException(s"Unable to parse FROM header in email:\n$text")
        val to = filterHeaderList(headers, "To: ")
        val cc = filterHeaderList(headers, "Cc: ")
        val bcc = filterHeaderList(headers, "Bcc: ")
        val subject = filterHeader(headers, "Subject: ")
        if (subject.isEmpty) throw new EmailParsingException(s"Unable to parse SUBJECT header in email:\n$text")

        // filter fixed footer for the body
        val bodyNoFooter = body.split(enronDatasetFooter) match { case s: Array[String] => s.head }
        // remove just the "forwarded by" tag
        val bodyNoFwd = bodyNoFooter.replaceAll(forwardedByReg, "")
        // remove all the "original messages" text
        val bodyNoOrig = if (subject.get.matches(subjectFwd)) { // check if email's subject reports something like Re: tag
            bodyNoFwd.replaceAll(originalMsg, "")
        } else {
            // get the part above the originalMsg tag
            try { bodyNoFwd.split(originalMsg).head }
            catch { case e: NoSuchElementException => throw new EmailParsingException(s"Unable to process body of email:\n$text") }
        }

        // clean body from headers
        val bodyNoHeaders = bodyNoOrig.split('\n').map(_
          .replaceAll(">?\\s?From:\\s[^\\n]*$", "")
          .replaceAll(">?\\s?To:\\s[^\\n]*$", "")
          .replaceAll(">?\\s?[Cc]c:\\s[^\\n]*$", "")
          .replaceAll(">?\\s?[Bb]cc:\\s[^\\n]*$", "")
          .replaceAll(">?\\s?Subject:\\s[^\\n]*$", "")
          .replaceAll(">?\\s?Sent:\\s[^\\n]*$", "")
          .replaceAll(">?\\s?Sent by:\\s[^\\n]*$", "")
        ).mkString("\n")

        Email(date.get, from.get, to.getOrElse(Seq()), cc.getOrElse(Seq()), bcc.getOrElse(Seq()), subject.get, bodyNoHeaders)
    }

    /** Returns a header entry from headers.
      *
      * @param headers raw email header
      * @param filter identifier of the entry to retrieve
      * @return the entry related to @filter attribute
      */
    private def filterHeader(headers: Seq[String], filter: String): Option[String] = {
        headers.filter(h => h.startsWith(filter)) match {
            case Seq() => None
            case x: Seq[String] =>
                try Some(x.head.split(filter)(1))
                catch { case e: java.lang.ArrayIndexOutOfBoundsException => None }
        }
    }

    /** Returns all the comma-separated entries for a specific header attribute.
      * Useful for header attributes like To, Cc, etc.
      *
      * @param headers raw email header
      * @param filter identifier of the entry to retrieve
      * @return list of entries related to @filter attribute
      */
    private def filterHeaderList(headers: Seq[String], filter: String): Option[Seq[Custodian]] = {
        filterHeader(headers, filter) match {
            case Some(s) => {
                // TODO: retrieve and return a Seq[Custodian] for this header string
                Some(Seq(Custodian("foo", "foo", "foo", None, None)))
//                Some(s.split(",").map(_.trim))
            }
            case _ => None
        }
    }

    type EmailParsingException = RuntimeException

}
