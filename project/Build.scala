import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._

object BuildSettings {

    val buildSettings = Defaults.coreDefaultSettings ++ Seq(
        organization := "nl.vu.ai.lsde",
        version := "1.0.0",
        scalaVersion := "2.10.6",
        resolvers += Resolver.sonatypeRepo("snapshots"),
        resolvers += Resolver.sonatypeRepo("releases")
    )

    val sparkVersion = "1.6.0"
    val sparkCore = "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
    val sparkSql = "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
    val sparkMLlib = "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"

    val hadoopVersion = "2.7.1"
    val hadoopClient = "org.apache.hadoop" % "hadoop-client" % hadoopVersion % "provided"

    val coreNLPVersion = "3.4.1"
    val coreNLP = "edu.stanford.nlp" % "stanford-corenlp" % coreNLPVersion
    val coreNLPModels = "edu.stanford.nlp" % "stanford-corenlp" % coreNLPVersion classifier "models"

    val protobuf = "com.google.protobuf" % "protobuf-java" % "2.6.1"
}

object Build extends Build {

    import BuildSettings._


    lazy val rootSettings = buildSettings ++
      sbtassembly.AssemblyPlugin.assemblySettings ++
      net.virtualvoid.sbt.graph.DependencyGraphSettings.graphSettings ++ Seq(
        assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, cacheUnzip = false),
        javaOptions in assembly += "-Xmx2g"
    )

    lazy val commonsSettings = rootSettings ++ Seq(
        libraryDependencies ++= Seq(hadoopClient)
    )

    lazy val unzipperSettings = rootSettings ++ Seq(
        libraryDependencies ++= Seq(sparkCore, hadoopClient),
        assemblyJarName in assembly := "unzipper.jar"
    )

    lazy val etlSettings = rootSettings ++ Seq(
        libraryDependencies ++= Seq(sparkCore, hadoopClient, sparkSql, protobuf, coreNLP, coreNLPModels),
        assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeDependency = false),
        assembly <<= assembly dependsOn assemblyPackageDependency,
        assemblyJarName in assemblyPackageDependency := "etl-deps.jar",
        assemblyJarName in assembly := "etl.jar"
    )

    lazy val spamFilterSettings = rootSettings ++ Seq(
        libraryDependencies ++= Seq(sparkCore, hadoopClient, sparkSql, sparkMLlib, coreNLP, protobuf),
        assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeDependency = false),
        assembly <<= assembly dependsOn assemblyPackageDependency,
        assemblyJarName in assemblyPackageDependency := "spam-filter-deps.jar",
        assemblyJarName in assembly := "spam-filter.jar"
    )


    lazy val root = Project(id = "root", base = file("."), settings = rootSettings).aggregate(unzipper, etl, spamFilter)

    lazy val commons = Project(id = "commons", base = file("./commons"), settings = commonsSettings)

    lazy val unzipper = Project(id = "unzipper", base = file("./unzipper"), settings = unzipperSettings).dependsOn(commons)

    lazy val etl = Project(id = "etl", base = file("./etl"), settings = etlSettings).dependsOn(commons)

    lazy val spamFilter = Project(id = "spam-filter", base = file("./spam-filter"), settings = spamFilterSettings).dependsOn(commons)

}
