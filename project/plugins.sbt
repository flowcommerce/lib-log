addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.20")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.12")
resolvers += "Flow Plugins" at "https://flow.jfrog.io/flow/plugins-release/"

addSbtPlugin("io.flow" % "sbt-flow-linter" % "0.0.20")
