import sbt._
import Keys._

object BuildSettings {
    val buildSettings = Defaults.coreDefaultSettings ++ Seq(
        organization := "nl.vu.ai.lsde",
        version := "1.0.0",
        scalaVersion := "2.10.6",
        resolvers += Resolver.sonatypeRepo("snapshots"),
        resolvers += Resolver.sonatypeRepo("releases"),
        scalacOptions ++= Seq()
    )

    val sparkVersion = "1.6.0"
    val sparkCore = "org.apache.spark" %% "spark-core" % sparkVersion

    val hadoopVersion = "2.7.1"
    val hadoopClient = "org.apache.hadoop" % "hadoop-client" % hadoopVersion
}

object Build extends Build {

    import BuildSettings._

    lazy val root = Project(id = "root", base = file(".")).aggregate(unzipper)

    lazy val unzipper = Project(
        id = "unzipper",
        base = file("./unzipper"),
        settings = buildSettings ++ Seq(
            libraryDependencies ++= Seq(sparkCore, hadoopClient)
        )
    )

}
