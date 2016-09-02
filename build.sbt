name := "part"

version := "0.2"

organization := "com.ankurdave"

crossScalaVersions := Seq("2.10.6", "2.11.8")
scalaVersion := "2.11.8"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"

javaOptions += "-Xmx10G"

fork := true

// Enable assertions
javaOptions in Test += "-ea"
