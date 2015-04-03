name := "part"

version := "0.1-SNAPSHOT"

organization := "com.ankurdave"

scalaVersion := "2.10.4"

javaOptions += "-Xmx10G"

fork := true

// Enable assertions
fork in run := true

javaOptions in run += "-ea"
