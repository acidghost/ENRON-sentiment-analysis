import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._

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
    val sparkCore = "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
    val sparkSql = "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"

    val hadoopVersion = "2.7.1"
    val hadoopClient = "org.apache.hadoop" % "hadoop-client" % hadoopVersion % "provided"
}

object Build extends Build {

    import BuildSettings._


    lazy val rootSettings = buildSettings ++
      sbtassembly.AssemblyPlugin.assemblySettings ++
      net.virtualvoid.sbt.graph.DependencyGraphSettings.graphSettings ++ Seq(
        assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
    )

    lazy val commonsSettings = rootSettings ++ Seq(
        libraryDependencies ++= Seq()
    )

    lazy val unzipperSettings = rootSettings ++ Seq(
        libraryDependencies ++= Seq(sparkCore, hadoopClient),
        assemblyJarName in assembly := "unzipper.jar"
    )

    lazy val parserSettings = rootSettings ++ Seq(
        libraryDependencies ++= Seq(sparkCore, hadoopClient, sparkSql),
        assemblyJarName in assembly := "parser.jar"
    )


    lazy val root = Project(id = "root", base = file("."), settings = rootSettings).aggregate(unzipper, parser)

    lazy val commons = Project(id = "commons", base = file("./commons"), settings = commonsSettings)

    lazy val unzipper = Project(id = "unzipper", base = file("./unzipper"), settings = unzipperSettings).dependsOn(commons)

    lazy val parser = Project(id = "parser", base = file("./parser"), settings = parserSettings).dependsOn(commons)

}
