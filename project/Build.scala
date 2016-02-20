import sbt._
import Keys._

object Build extends Build {

    lazy val root = Project(id = "root", base = file("."))

    lazy val unzipper = Project(id = "unzipper", base = file("./unzipper"))

}
