ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .enablePlugins(
    JavaAppPackaging
  )
  .settings(
    name := "consume-kafka-raw"
  )
resolvers += "Confluent" at "https://packages.confluent.io/maven/"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.4",

  "com.github.fd4s" %% "fs2-kafka" % "3.5.0",
  "com.github.fd4s" %% "fs2-kafka-vulcan" % "3.5.0",

  "com.github.fd4s" %% "vulcan-generic" % "1.11.0",
  "com.github.fd4s" %% "vulcan" % "1.11.0",

  "com.sksamuel.avro4s" %% "avro4s-core" % "5.0.12",

  "io.circe" %% "circe-core" % "0.15.0-M1",
  "io.circe" %% "circe-generic" % "0.15.0-M1",
  "io.circe" %% "circe-parser" % "0.15.0-M1",

  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)
