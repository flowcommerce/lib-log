addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.16")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")
resolvers += "Flow Plugins" at "https://flow.jfrog.io/flow/plugins-release/"

addSbtPlugin("io.flow" % "sbt-flow-linter" % "0.0.34")
