package nl.vu.ai.lsde.enron.unzipper

import java.io.File
import java.util.zip.{ZipEntry, ZipInputStream}

import org.apache.spark.input.PortableDataStream
import org.apache.spark.{SparkConf, SparkContext}
import scala.util.parsing.combinator.syntactical.StandardTokenParsers

class Parser extends App{

  abstract class Email {
    var date : String
    var from : String
    var to : String
    var cc : String
    var subject : String
    var body : String
  }

  val filesDir = "hdfs:///user/lsde03/enron/extracted_txt"
  val appName = "ENRON-parser"
  val conf = new SparkConf().setAppName(appName)
  val sc = new SparkContext(conf)

  val scalaParser = new StandardTokenParsers()
  scalaParser.lexical.delimiters ++= List("\n")



  sc.wholeTextFiles(filesDir).foreach { case (fileName, fileContent) =>

    parse(fileContent)
  }


  def parse(text: String):Email = {
    
  }

  //  insert SparkSQL table
  def pushEntry() = {

  }
}
