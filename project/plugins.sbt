//// Comment to get more information during initialization
//logLevel := Level.Warn
//
//// Resolvers
//resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Sbt plugins
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.20")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.25")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.8-0.6")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.3")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.10")
