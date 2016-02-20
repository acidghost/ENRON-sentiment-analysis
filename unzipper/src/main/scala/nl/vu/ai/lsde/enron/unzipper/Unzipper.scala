package nl.vu.ai.lsde.enron.unzipper

import java.io.File
import java.util.zip.{ZipEntry, ZipInputStream}

import org.apache.spark.input.PortableDataStream
import org.apache.spark.{SparkConf, SparkContext}

object Unzipper extends App {

    val appName = "ENRON-unzipper"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    if (sc == null) System.exit(1)

    val outDir = "hdfs:///user/lsde03/enron/extracted_txt"

    sc.binaryFiles("hdfs:///user/hannesm/lsde/enron/*.zip").foreach { case (fileName, stream) =>
        val zipName = fileName.split('/').last.split(".zip")(0)
        println(s"Unzipping $zipName")

        val mailboxName = zipName.split('_') match {
            case splitted if splitted.length > 1 => splitted(1)
            case splitted => splitted(0)
        }

        unzip(sc, stream, outDir + File.separator + mailboxName)
    }

    def unzip(sc: SparkContext, stream: PortableDataStream, mailboxDir: String, filter: String = "text_000/") = {

        val buffer = new Array[Byte](1024)

        val zis: ZipInputStream = new ZipInputStream(stream.open)
        var ze: ZipEntry = zis.getNextEntry

        while (ze != null) {
            val fileName = ze.getName

            if (fileName.contains(filter)) {
                val newFile = mailboxDir + File.separator + fileName.split(filter)(1)
                println(s"File unzip: $newFile")

                var fileTxt = ""
                var len: Int = zis.read(buffer)
                while (len > 0) {
                    fileTxt += buffer.map(_.toChar).mkString
                    len = zis.read(buffer)
                }

                val txt = sc.parallelize(fileTxt)
                txt.saveAsTextFile(newFile)
            }

            ze = zis.getNextEntry
        }
    }

}
