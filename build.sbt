name := "lib-log"

organization := "io.flow"

scalaVersion in ThisBuild := "2.12.10"

val timeLibSuffix = ""

libraryDependencies ++= Seq(
  "io.flow" %% s"lib-util$timeLibSuffix" % "0.1.32",
  "com.rollbar" % "rollbar-java" % "1.5.2",
  "com.google.inject.extensions" % "guice-assistedinject" % "4.2.2",
  "net.codingwell" %% "scala-guice" % "4.2.6",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.2", // structured logging to sumo
  // The following will need to be provided by users of this lib,
  // meaning they can supply their own version (as long as compatible).
  "com.typesafe.play" %% "play-json" % "2.7.4" % Provided,
  compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.4.2"),
  "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided
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

version := "0.0.86"
