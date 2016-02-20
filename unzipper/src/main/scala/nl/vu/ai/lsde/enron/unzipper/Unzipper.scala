package nl.vu.ai.lsde.enron.unzipper

import java.io.{IOException, FileOutputStream, File}
import java.util.zip.{ZipEntry, ZipInputStream}

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.input.PortableDataStream

object Unzipper extends App {

    val appName = "ENRON-unzipper"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val outDir = "hdfs:///user/hannesm/lsde/enron/extracted_txt"

    sc.binaryFiles("hdfs:///user/hannesm/lsde/enron/*.zip").foreach { case (fileName, stream) =>
        val zipName = fileName.split('/').last.split(".zip")(0)
        println(s"Unzipping $zipName")

        val mailboxName = zipName.split('_') match {
            case splitted if splitted.length > 1 => splitted(1)
            case splitted => splitted(0)
        }

        unzip(stream, outDir + File.separator + mailboxName)
    }

    def unzip(stream: PortableDataStream,
              outDir: String,
              filter: String = "text_000/") = {

        val buffer = new Array[Byte](1024)

        val zis: ZipInputStream = new ZipInputStream(stream.open)
        var ze: ZipEntry = zis.getNextEntry

        while (ze != null) {
            val fileName = ze.getName

            if (fileName.contains(filter)) {
                val newFile = outDir + File.separator + fileName.split(filter)(1)
                println(s"File unzip: $newFile")

                var fileTxt = ""
                var len: Int = zis.read(buffer)
                while (len > 0) {
                    fileTxt += buffer.map(_.toChar)
                    len = zis.read(buffer)
                }

                sc.parallelize(fileTxt).saveAsTextFile(newFile)
            }

            ze = zis.getNextEntry
        }
    }

}
