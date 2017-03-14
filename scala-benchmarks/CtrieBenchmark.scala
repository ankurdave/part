object CtrieBenchmark {
  def main(args: Array[String]): Unit = {
    benchmarkCtrie(batchSize = args(0).toInt)
  }

  val r = new util.Random

  def time(f: => Unit): Long = {
    val start = System.nanoTime
    f
    System.nanoTime - start
  }

  def uniform(k: Int): Iterator[Int] = {
    Iterator.fill(k) { r.nextInt() }
  }

  def benchmarkCtrie(m_0: Int = 10000000, T: Int = 1000000, batchSize: Int = 1) {
    val ctrie = collection.concurrent.TrieMap.empty[Int, Int]
    for (i <- 0L until m_0) {
      ctrie.update(r.nextInt, r.nextInt)
    }
    println(s"{'measurement': 'memory', 'datastructure': 'ctrie', " +
      s"'y': ${com.ankurdave.indexedrddbenchmarking.SizeEstimator.estimate(ctrie)}, 'valsize': 4},")

    var total = 0L
    var readTotal = 0L

    val scanTime = time { ctrie.foreach { case (k, v) => total += v } }
    println(s"{'measurement': 'scan', 'datastructure': 'ctrie', " +
      s"'y': ${m_0 / (scanTime / 1e9)}, 'valsize': 4},")

    val readTime = time { uniform(T).foreach { k => readTotal += ctrie.getOrElse(k, 0) } }
    println(s"{'measurement': 'read', 'datastructure': 'ctrie', " +
      s"'y': ${m_0 / (readTime / 1e9)}, 'valsize': 4},")

    var prev = ctrie.readOnlySnapshot()
    var numTrials = 0
    var insertTime = 0L
    while (insertTime < 5L * 1000 * 1000 * 1000) {
      insertTime += time {
        prev = ctrie.readOnlySnapshot()
        uniform(batchSize).foreach { k =>
          ctrie.update(k, if (ctrie.contains(k)) ctrie.lookup(k) + 1 else r.nextInt)
        }
      }
      numTrials += 1
    }
    println(s"{'measurement': 'insert', 'datastructure': 'ctrie', 'x': $batchSize, " +
      s"'y': ${numTrials * batchSize / (insertTime / 1e9)}, 'valsize': 4, 'inplace': False},")

    Console.err.println(total)
    Console.err.println(readTotal)
  }
}
