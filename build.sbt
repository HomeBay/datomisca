organization in ThisBuild := "com.homebay"
licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
scalaVersion in ThisBuild := "2.13.1"

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-language:experimental.macros",
  "-Ymacro-annotations",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)

resolvers in ThisBuild ++= Seq(
  "clojars" at "https://clojars.org/repo"
)

lazy val datomisca = project.
  in(file(".")).
  aggregate(macros, core, tests, integrationTests)

lazy val tests = project.in(file("tests")).
  settings(noPublishSettings).
  settings(
    name := "datomisca-tests",
    libraryDependencies ++= Seq(
      datomic,
      specs2
    ),
    fork in Test := true,
    publishArtifact := false
  ).
  dependsOn(macros, core)

lazy val integrationTests = project.in(file("integration")).
  settings(noPublishSettings).
  settings(Defaults.itSettings).
  settings(
    name := "datomisca-tests",
    libraryDependencies ++= Seq(
      datomic,
      scalatest,
      xmlModule
    ),
    fork in IntegrationTest := true,
    publishArtifact := false
  ).
  dependsOn(macros, core).
  configs(IntegrationTest)

lazy val core = project.in(file("core")).
  settings(noPublishSettings).
  settings(
    name := "datomisca-core",
    libraryDependencies += datomic,
    scalacOptions := compilerOptions,
    (sourceGenerators in Compile) += ((sourceManaged in Compile) map Boilerplate.genCore).taskValue
  ).
  dependsOn(macros)

lazy val macros = project.in(file("macros")).
  settings(noPublishSettings).
  settings(
    name := "datomisca-macros",
    libraryDependencies ++= Seq(
      datomic,
      reflect(scalaVersion.value)
    ),
    scalacOptions := compilerOptions
  )

lazy val docs = project.in(file("docs")).
  settings(
    name := "Datomisca Docs",
    moduleName := "datomisca-docs"
  ).
  settings(noPublishSettings).
  dependsOn(core, macros)

mappings in (Compile, packageBin) ++= (mappings in (macros, Compile, packageBin)).value
mappings in (Compile, packageSrc) ++= (mappings in (macros, Compile, packageSrc)).value

mappings in (Compile, packageBin) ++= (mappings in (core, Compile, packageBin)).value
mappings in (Compile, packageSrc) ++= (mappings in (core, Compile, packageSrc)).value

val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

publishMavenStyle := true

publishTo := Some("S3 Artifacts" at "s3://homebay-artifacts/ext-releases-local")

def datomic = "com.datomic" % "datomic-free" % "0.9.5561" % Provided
def specs2 = "org.specs2" %% "specs2-core" % "4.7.0" % Test
def scalatest = "org.scalatest" %% "scalatest" % "3.0.8" % "it"
def xmlModule = "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
def reflect(vers: String)  = "org.scala-lang" % "scala-reflect" % vers

