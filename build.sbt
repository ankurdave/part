name := "part"

version := "0.2"

organization := "com.ankurdave"

scalaVersion := "2.10.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

javaOptions += "-Xmx10G"

fork := true

// Enable assertions
javaOptions in Test += "-ea"
