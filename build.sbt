organization := "woshilaiceshide"

name := "scala-cflow"

version := "1.0"

description := ""

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

publishMavenStyle := true

enablePlugins(BintrayPlugin)

pomIncludeRepository  := {_ => false}

bintrayRepository := "maven"

bintrayOrganization := None

bintrayVcsUrl := Some(s"git@github.com:woshilaiceshide/${name.value}.git")

bintrayReleaseOnPublish in ThisBuild := false

compileOrder in Compile := CompileOrder.Mixed

transitiveClassifiers := Seq("sources")

EclipseKeys.withSource := true

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation","-optimise", "-encoding", "utf8", "-Yno-adapted-args")

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-g:vars")

//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.7" exclude("com.typesafe", "config")
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.7"

retrieveManaged := false

enablePlugins(JavaAppPackaging)

unmanagedSourceDirectories in Compile <+= baseDirectory( _ / "src" / "java" )

unmanagedSourceDirectories in Compile <+= baseDirectory( _ / "src" / "scala" )
