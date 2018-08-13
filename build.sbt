name := "lib-log"

organization := "io.flow"

scalaVersion in ThisBuild := "2.12.6"

libraryDependencies ++= Seq(
  "io.flow" %% "lib-util" % "0.0.10",
  "com.rollbar" % "rollbar-java" % "1.3.1",
  "com.google.inject.extensions" % "guice-assistedinject" % "4.2.0",
  "net.codingwell" %% "scala-guice" % "4.2.1",
  "net.logstash.logback" % "logstash-logback-encoder" % "5.2", // structured logging to sumo
  // The following will need to be provided by users of this lib,
  // meaning they can supply their own version (as long as compatible).
  "com.typesafe.play" %% "play-json" % "2.6.9" % Provided
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

version := "0.0.25"
