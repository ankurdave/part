object CtrieBenchmark {
  def main(args: Array[String]): Unit = {
    val batchSize = args(0).toInt
    if (args.length > 1 && args(1) == "-DBIG_VAL") {
      benchmarkCtrie[Array[Byte]](
        batchSize = batchSize,
        randomVal = new Array[Byte](1024),
        valExtractor = v => v(0).toInt,
        incrementVal = identity,
        valSize = 1024)
    } else {
      benchmarkCtrie[Int](
        batchSize = batchSize,
        randomVal = r.nextInt,
        valExtractor = identity,
        incrementVal = _ + 1,
        valSize = 4)
    }
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

  def benchmarkCtrie[@specialized(Int) ValType](
    valSize: Int,
    randomVal: => ValType,
    valExtractor: ValType => Int,
    incrementVal: ValType => ValType,
    m_0: Int = 10000000,
    T: Int = 1000000,
    batchSize: Int = 1) {
    val ctrie = collection.concurrent.TrieMap.empty[Int, ValType]
    val keys = new Array[Int](m_0)
    for (i <- 0 until m_0) {
      val k = r.nextInt
      ctrie.update(k, randomVal)
      keys(i) = k
    }
    println(s"{'measurement': 'memory', 'datastructure': 'ctrie', " +
      s"'y': ${com.ankurdave.indexedrddbenchmarking.SizeEstimator.estimate(ctrie)}, 'valsize': $valSize},")

    var total = 0L
    var readTotal = 0L

    val scanTime = time { ctrie.foreach { case (k, v) => total += valExtractor(v) } }
    println(s"{'measurement': 'scan', 'datastructure': 'ctrie', " +
      s"'y': ${m_0 / (scanTime / 1e9)}, 'valsize': $valSize},")

    val randomKeys = util.Random.shuffle(keys.toSeq).toArray

    val readTime = time {
      (0 until T).foreach { i => readTotal += valExtractor(ctrie.getOrElse(randomKeys(i), randomVal)) }
    }
    println(s"{'measurement': 'read', 'datastructure': 'ctrie', " +
      s"'y': ${T / (readTime / 1e9)}, 'valsize': $valSize},")

    var prev = ctrie.readOnlySnapshot()
    var numTrials = 0
    var insertTime = 0L
    while (insertTime < 5L * 1000 * 1000 * 1000) {
      insertTime += time {
        prev = ctrie.readOnlySnapshot()
        uniform(batchSize).foreach { k =>
          ctrie.update(k, if (ctrie.contains(k)) incrementVal(ctrie.lookup(k)) else randomVal)
        }
      }
      numTrials += 1
    }
    println(s"{'measurement': 'insert', 'datastructure': 'ctrie', 'x': $batchSize, " +
      s"'y': ${numTrials * batchSize / (insertTime / 1e9)}, 'valsize': $valSize, 'inplace': False},")

    Console.err.println(total)
    Console.err.println(readTotal)
  }
}
