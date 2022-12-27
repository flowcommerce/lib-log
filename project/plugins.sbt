addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.18")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")
resolvers += "Flow Plugins" at "https://flow.jfrog.io/flow/plugins-release/"

addSbtPlugin("io.flow" % "sbt-flow-linter" % "0.0.35")

// Resolve scala-xml version dependency mismatch, see https://github.com/sbt/sbt/issues/7007
 ThisBuild / libraryDependencySchemes ++= Seq(
   "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
 )

addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")

