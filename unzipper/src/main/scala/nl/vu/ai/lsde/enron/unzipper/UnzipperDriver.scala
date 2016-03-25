package nl.vu.ai.lsde.enron.unzipper

import java.io.{InputStreamReader, BufferedReader}
import java.util.zip.{ZipEntry, ZipInputStream}

import nl.vu.ai.lsde.enron.Commons
import org.apache.spark.input.PortableDataStream
import org.apache.spark.{SparkConf, SparkContext}

/**
  * This driver program is responsible to extract the textual formatted
  * messages from the ENRON corpus. The program iterates over all zip files
  * and extracts all .txt files in the folder text_000.
  */
object UnzipperDriver extends App {

    val appName = "ENRON-unzipper"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    // iterate over the zip files and map each zip to a tuple (mailboxName, emails)
    val dataset = sc.binaryFiles(s"${Commons.SOURCE_ENRON_DATA}/*.zip").map { case (fileName, stream) =>
        // get the zip filename
        val zipName = fileName.split('/').last.split(".zip")(0)

        // get mailbox name from zip filename
        val mailboxName = zipName.split('_') match {
            case splitted: Array[String] if splitted.length > 1 => splitted(1)
            case splitted: Array[String] => splitted(0)
        }

        // unzip documents
        val documents = unzip(stream)
        (mailboxName, documents)
    }

    dataset.flatMap(t => t._2).sample(withReplacement = false, .00001, 42L).collect().foreach(e => println(s"$e\n\n\n\n------- END EMAIL ----------\n\n\n"))

    // save the collection of mailboxes on disk
    Commons.deleteFolder(Commons.ENRON_EXTRACTED_TXT)
    dataset.saveAsObjectFile(Commons.ENRON_EXTRACTED_TXT)


    /**
      * Extracts files from a zip archive that match a filter.
      *
      * @param stream Zip archive bytes stream
      * @param filter The filename filter
      * @return A collection of extracted documents
      */
    def unzip(stream: PortableDataStream, filter: String = "text_0"): Seq[String] = {

        // regexp that matches non attachment files
        val regexNotAttachments = "[\\W\\w]*/\\d+\\.\\d+\\.\\w+\\.txt"

        val zis: ZipInputStream = new ZipInputStream(stream.open())
        var ze: ZipEntry = zis.getNextEntry

        val documents = scala.collection.mutable.ArrayBuffer.empty[String]

        while (Option(ze).isDefined) {
            val fileName = ze.getName

            // consider only plain text files and don't consider attachments
            if (fileName.contains(filter) && fileName.matches(regexNotAttachments)) {
                val reader = new BufferedReader(new InputStreamReader(zis, "UTF-8"))
                val document = Stream.continually(reader.readLine()).takeWhile(_ != null).toSeq.mkString("\n")

                documents += document
            }

            ze = zis.getNextEntry
        }

        documents.toSeq
    }

}
