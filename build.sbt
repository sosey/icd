import sbt._
import sbt.Project.projectToRef
import Dependencies._
import Settings._

def compileScope(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
def testScope(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")

lazy val clients = Seq(icdWebClient)

// Root of the multi-project build
lazy val root = (project in file("."))
  .aggregate(icd, `icd-db`, `icd-git`, icdWebServer)
  .settings(name := "ICD")

// Core project, implements validation of ICD model files against JSON schema files, icd command line tool
lazy val icd = project
  .enablePlugins(DeployApp)
  .settings(defaultSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(jsonSchemaValidator, scopt, scalatags, typesafeConfig, ficus, flexmarkAll, itextpdf, xmlworker, diffson, sprayJson, scalaLogging, logback, jsoup) ++
      testScope(scalaTest)
  ) dependsOn icdWebSharedJvm

// Adds MongoDB database support, ICD versioning, queries, icd-db command line tool
lazy val `icd-db` = project
  .enablePlugins(DeployApp)
  .settings(defaultSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(casbah, scalaCsv) ++
      testScope(scalaTest)
  ) dependsOn icd


// Adds support for working with ICD model file repositories on GitHub, ICD version management, icd-github tool
lazy val `icd-git` = project
  .enablePlugins(DeployApp)
  .settings(defaultSettings: _*)
  .settings(libraryDependencies ++=
    compileScope(jgit) ++
      testScope(scalaTest)
  ) dependsOn(icd, `icd-db`)


// -- Play/ScalaJS parts below --


// a Play framework based web server that goes between icd-db and the web client
lazy val icdWebServer = (project in file("icd-web-server"))
  .settings(defaultSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    scalaJSProjects := clients,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    includeFilter in(Assets, LessKeys.less) := "icd.less",
    libraryDependencies ++=
      compileScope(filters, guice, scalajsScripts, playJson, jqueryUi, webjarsPlay, bootstrap, bootstrapTable) ++
        testScope(specs2)
  )
  .enablePlugins(PlayScala, SbtWeb, DockerPlugin)
  .dependsOn(`icd-db`, `icd-git`)

// a Scala.js based web client that talks to the Play server
lazy val icdWebClient = (project in file("icd-web-client"))
  .settings(commonSettings)
  .settings(
  scalaJSUseMainModuleInitializer := false,
  unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value),
  skip in packageJSDependencies := false,
  jsDependencies ++= clientJsDeps.value,
  libraryDependencies ++= clientDeps.value
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(icdWebSharedJs)


// contains simple case classes used for data transfer that are shared between the client and server
lazy val icdWebShared = (crossProject.crossType(CrossType.Pure) in file("icd-web-shared"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %%% "play-json" % PlayVersion,
      "com.lihaoyi" %%% "scalatags" % ScalaTagsVersion
    )
  )

lazy val icdWebSharedJvm = icdWebShared.jvm
lazy val icdWebSharedJs = icdWebShared.js

// loads the server project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project icdWebServer" :: s}
