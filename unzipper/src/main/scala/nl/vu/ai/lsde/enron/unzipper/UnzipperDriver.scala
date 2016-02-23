package nl.vu.ai.lsde.enron.unzipper

import java.util.zip.{ZipEntry, ZipInputStream}

import nl.vu.ai.lsde.enron.Commons
import org.apache.spark.input.PortableDataStream
import org.apache.spark.{SparkConf, SparkContext}

object UnzipperDriver extends App {

    val appName = "ENRON-unzipper"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val dataset = sc.binaryFiles(s"${Commons.SOURCE_ENRON_DATA}/*.zip").map { case (fileName, stream) =>
        val zipName = fileName.split('/').last.split(".zip")(0)

        val mailboxName = zipName.split('_') match {
            case splitted: Array[String] if splitted.length > 1 => splitted(1)
            case splitted: Array[String] => splitted(0)
        }

        val documents = unzip(stream)
        (mailboxName, documents)
    }

    dataset.saveAsObjectFile(Commons.ENRON_EXTRACTED_TXT)

    def unzip(stream: PortableDataStream, filter: String = "text_000/"): Seq[String] = {

        val buffer = new Array[Byte](1024)
        val regexNotAttachments = "\\d+.\\d+.\\w+.txt"

        val zis: ZipInputStream = new ZipInputStream(stream.open)
        var ze: ZipEntry = zis.getNextEntry

        val documents = scala.collection.mutable.ArrayBuffer.empty[String]

        while (ze != null) {
            val fileName = ze.getName

            // consider only plain text files and don't consider attachments
            if (fileName.contains(filter) && fileName.matches(regexNotAttachments)) {

                val fileTxt = new StringBuilder
                var len: Int = zis.read(buffer)
                while (len > 0) {
                    fileTxt ++= buffer.map(_.toChar).mkString
                    len = zis.read(buffer)
                }

                documents += fileTxt.toString
            }

            ze = zis.getNextEntry
        }

        documents.toSeq
    }

}
