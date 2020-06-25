name := "lib-log"

organization := "io.flow"

scalaVersion := "2.13.1"

val timeLibSuffix = ""

libraryDependencies ++= Seq(
  "io.flow" %% s"lib-util$timeLibSuffix" % "0.1.48",
  "com.rollbar" % "rollbar-java" % "1.7.3",
  "com.google.inject.extensions" % "guice-assistedinject" % "4.2.3",
  "net.codingwell" %% "scala-guice" % "4.2.7",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.4", // structured logging to sumo
  "org.scalatest" %% "scalatest" % "3.2.0" % Test,
  // The following will need to be provided by users of this lib,
  // meaning they can supply their own version (as long as compatible).
  "com.typesafe.play" %% "play-json" % "2.9.0" % Provided,
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

version := "0.1.13"
