package nl.vu.ai.lsde.enron.unzipper

import java.util.zip.{ZipEntry, ZipInputStream}

import nl.vu.ai.lsde.enron.Commons
import org.apache.spark.input.PortableDataStream
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.LoggerFactory

object Unzipper extends App {

    val appName = "ENRON-unzipper"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val logger = LoggerFactory.getLogger(getClass)

    val dataset = sc.binaryFiles(s"${Commons.SOURCE_ENRON_DATA}/*.zip").map { case (fileName, stream) =>
        val zipName = fileName.split('/').last.split(".zip")(0)
        if (logger != null) logger.info(s"Unzipping $zipName")

        val mailboxName = zipName.split('_') match {
            case splitted: Any if splitted.length > 1 => splitted(1)
            case splitted: Any => splitted(0)
        }

        val documents = unzip(stream)
        (mailboxName, documents)
    }

    dataset.saveAsTextFile(Commons.ENRON_EXTRACTED_TXT)

    def unzip(stream: PortableDataStream, filter: String = "text_000/"): Seq[String] = {

        val buffer = new Array[Byte](1024)

        val zis: ZipInputStream = new ZipInputStream(stream.open)
        var ze: ZipEntry = zis.getNextEntry

        val documents = scala.collection.mutable.ArraySeq[String]()

        while (ze != null) {
            val fileName = ze.getName

            if (fileName.contains(filter)) {

                val fileTxt = new StringBuilder
                var len: Int = zis.read(buffer)
                while (len > 0) {
                    fileTxt + buffer.map(_.toChar).mkString
                    len = zis.read(buffer)
                }

                documents + fileTxt.toString
            }

            ze = zis.getNextEntry
        }

        documents.toSeq
    }

}
