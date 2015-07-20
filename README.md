# Persistent Adaptive Radix Tree (PART) for Java

The Persistent Adaptive Radix Tree (PART) is a trie with a high branching factor and adaptively-sized nodes based on [ART](http://www3.informatik.tu-muenchen.de/~leis/papers/ART.pdf). It provides efficient persistence using path copying and reference counting. In microbenchmarks, PART achieves throughput and space efficiency comparable to a mutable hash table while providing persistence, lower variance in operation latency, and efficient union, intersection, and range scan operations.

This repository contains a Java implementation of PART based on [libart](https://github.com/armon/libart).

# Usage

Add the dependency to your SBT project by adding the following to `build.sbt`:

```scala
resolvers += "Repo at github.com/ankurdave/maven-repo" at "https://github.com/ankurdave/maven-repo/raw/master"

libraryDependencies += "com.ankurdave" %% "part" % "0.1"
```
