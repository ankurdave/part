name := "part"

version := "0.1"

organization := "com.ankurdave"

scalaVersion := "2.10.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

javaOptions += "-Xmx10G"

fork := true

// Enable assertions
javaOptions in Test += "-ea"

publishTo := Some(Resolver.file("part", new File("/Users/ankurdave/repos/maven-repo")))
