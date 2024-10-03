name := "lib-log"

organization := "io.flow"

scalaVersion := "2.13.15"
ThisBuild / javacOptions ++= Seq("-source", "17", "-target", "17")

enablePlugins(GitVersioning)
git.useGitDescribe := true
coverageDataDir := file("target/scala-2.13")
coverageHighlighting := true
coverageFailOnMinimum := true
coverageMinimumStmtTotal := 25
coverageMinimumBranchTotal := 15

lazy val allScalacOptions = Seq(
  "-feature",
  "-Xfatal-warnings",
  "-unchecked",
  "-Xcheckinit",
  "-Xlint:adapted-args",
  "-Ypatmat-exhaust-depth",
  "100", // Fixes: Exhaustivity analysis reached max recursion depth, not all missing cases are reported.
  "-Wconf:src=generated/.*:silent",
  "-Wconf:src=target/.*:silent", // silence the unused imports errors generated by the Play Routes
  "-release:17"
)

libraryDependencies ++= Seq(
  "io.flow" %% s"lib-util" % "0.2.47",
  "com.rollbar" % "rollbar-java" % "1.10.3",
  "com.google.inject.extensions" % "guice-assistedinject" % "4.2.3",
  "org.typelevel" %% "cats-core" % "2.10.0",
  "net.codingwell" %% "scala-guice" % "4.2.11",
  "com.google.inject" % "guice" % "5.1.0",
  "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.3", // structured logging to sumo
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "com.typesafe.play" %% "play-json-joda" % "2.9.4"
)

resolvers += "Artifactory" at "https://flow.jfrog.io/flow/libs-release/"
Test / javaOptions ++= Seq(
  "--add-exports=java.base/sun.security.x509=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.ssl=ALL-UNNAMED"
)
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
scalafmtOnCompile := true
