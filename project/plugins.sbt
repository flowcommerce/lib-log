addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.11")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.20")
resolvers += "Flow Plugins" at "https://flow.jfrog.io/flow/plugins-release/"

addSbtPlugin("io.flow" % "sbt-flow-linter" % "0.0.33")
