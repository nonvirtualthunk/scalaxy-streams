name := "scalaxy-streams"

organization := "com.nativelibs4java"

version := "0.4-SNAPSHOT"

scalaVersion := "2.12.3"

crossScalaVersions := Seq("2.12.3")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies ++= Seq(
  "org.scala-js" %% "scalajs-library" % "0.6.19" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

testOptions in Global += Tests.Argument(TestFrameworks.JUnit, "-v")

fork in Test := true

// Needed to avoid cryptic EOFException crashes in forked tests
// in Travis with `sudo: false`.
// See https://github.com/sbt/sbt/issues/653
// and https://github.com/travis-ci/travis-ci/issues/3775
javaOptions += "-Xmx4G"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation", "-feature", "-unchecked",
  "-optimise",
  "-Xlog-free-types"
)

watchSources <++= baseDirectory map { path => (path / "examples" ** "*.scala").get }

scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)

scalacOptions in console in Compile ++= Seq(
  "-Xplugin-require:scalaxy-streams",
  "-Xprint:scalaxy-streams"
)

// ScalariformKeys.preferences := {
//   import scalariform.formatter.preferences._
//   FormattingPreferences()
//     .setPreference(AlignParameters, true)
//     .setPreference(AlignSingleLineCaseStatements, true)
//     .setPreference(CompactControlReadability, true)
//     .setPreference(DoubleIndentClassDeclaration, true)
//     .setPreference(IndentSpaces, 2)
//     .setPreference(IndentWithTabs, false)
//     .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
//     .setPreference(PreserveDanglingCloseParenthesis, true)
// }
// scalariformSettings

homepage := Some(url("https://github.com/nativelibs4java/scalaxy-streams"))

pomExtra := (
  <scm>
    <url>git@github.com:nativelibs4java/scalaxy-streams.git</url>
    <connection>scm:git:git@github.com:nativelibs4java/scalaxy-streams.git</connection>
  </scm>
  <developers>
    <developer>
      <id>ochafik</id>
      <name>Olivier Chafik</name>
      <url>http://ochafik.com/</url>
    </developer>
  </developers>
)

licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause"))

pomIncludeRepository := { _ => false }

publishMavenStyle := true

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
