name := "lib-log"

organization := "io.flow"

scalaVersion := "2.13.10"

enablePlugins(GitVersioning)
git.useGitDescribe := true

lazy val allScalacOptions = Seq(
  "-feature",
  "-Xfatal-warnings",
  "-unchecked",
  "-Xcheckinit",
  "-Xlint:adapted-args",
  "-Ypatmat-exhaust-depth", "100", // Fixes: Exhaustivity analysis reached max recursion depth, not all missing cases are reported.
  "-Wconf:src=generated/.*:silent",
  "-Wconf:src=target/.*:silent", // silence the unused imports errors generated by the Play Routes
)

libraryDependencies ++= Seq(
  "io.flow" %% s"lib-util" % "0.2.8",
  "com.rollbar" % "rollbar-java" % "1.9.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "4.2.3",
  "org.typelevel" %% "cats-core" % "2.9.0",
  "net.codingwell" %% "scala-guice" % "4.2.11",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.3", // structured logging to sumo
  "org.scalatest" %% "scalatest" % "3.2.14" % Test,
  // The following will need to be provided by users of this lib,
  "com.typesafe.play" %% "play-json" % "2.9.3" % Provided,
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

scalacOptions ++= allScalacOptions
