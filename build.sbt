name := "lib-log"

organization := "io.flow"

scalaVersion := "2.13.1"

val timeLibSuffix = ""

libraryDependencies ++= Seq(
  "io.flow" %% s"lib-util$timeLibSuffix" % "0.1.44",
  "com.rollbar" % "rollbar-java" % "1.6.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "4.2.3",
  "net.codingwell" %% "scala-guice" % "4.2.6",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.3", // structured logging to sumo
  "org.scalatest" %% "scalatest" % "3.1.1" % Test,
  // The following will need to be provided by users of this lib,
  // meaning they can supply their own version (as long as compatible).
  "com.typesafe.play" %% "play-json" % "2.8.1" % Provided,
  compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.6.0" cross CrossVersion.full),
  "com.github.ghik" %% "silencer-lib" % "1.6.0" % Provided cross CrossVersion.full,
)

resolvers += "Artifactory" at "https://flow.jfrog.io/flow/libs-release/"

credentials += Credentials(
  "Artifactory Realm",
  "flow.jfrog.io",
  System.getenv("ARTIFACTORY_USERNAME"),
  System.getenv("ARTIFACTORY_PASSWORD")
)

publishTo := {
  val host = "https://flow.jfrog.io/flow"
  if (isSnapshot.value) {
    Some("Artifactory Realm" at s"$host/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
  } else {
    Some("Artifactory Realm" at s"$host/libs-release-local")
  }
}

version := "0.1.10"
