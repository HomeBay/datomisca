ThisBuild / organization := "com.homebay"
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / scalaVersion := "2.13.5"

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

lazy val testCompilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature"
)

ThisBuild / resolvers ++= Seq(
  "clojars"        at "https://clojars.org/repo",
  "my.datomic.com" at "https://my.datomic.com/repo"
)

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

lazy val datomisca = project.
  in(file(".")).
  aggregate(macros, core, tests, integrationTests)

lazy val tests = project.in(file("tests")).
  settings(noPublishSettings).
  settings(
    name := "datomisca-tests",
    scalacOptions := testCompilerOptions,
    libraryDependencies ++= Seq(
      datomic,
      specs2
    ),
    Test / fork := true,
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
    IntegrationTest / fork := true,
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
    Compile / sourceGenerators += ((Compile / sourceManaged) map Boilerplate.genCore).taskValue
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

Compile / packageBin / mappings ++= (macros / Compile / packageBin / mappings).value
Compile / packageSrc / mappings ++= (macros / Compile / packageSrc / mappings).value

Compile / packageBin / mappings ++= (core / Compile / packageBin / mappings).value
Compile / packageSrc / mappings  ++= (core / Compile / packageSrc / mappings).value

val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

publishMavenStyle := true

 publishTo := Some("S3 Artifacts" at "s3://homebay-artifacts/ext-releases-local")

def datomic   = "com.datomic"                 % "datomic-pro"   % "1.0.6165" % Provided
def specs2    = "org.specs2"                  %% "specs2-core"  % "4.7.0" % Test
def scalatest = "org.scalatest"               %% "scalatest"    % "3.0.8" % "it"
def xmlModule = "org.scala-lang.modules"      %% "scala-xml"    % "1.2.0"
def reflect(vers: String)  = "org.scala-lang" % "scala-reflect" % vers

