package com.ankurdave.part

import scala.util.Random
import scala.collection.mutable.WrappedArray
import scala.collection.JavaConversions._

import org.scalatest.FunSuite

class PartSuite extends FunSuite {

  val max_n = 10000;
  val max_key_len = 10;
  val r = new Random

  // For sorting Seq[Seq[Byte]] lexicographically and bitwise
  import scala.math.Ordering.Implicits._
  implicit val byteOrdering = Ordering.by[Byte, Int](b => Node.to_uint(b))

  // For sorting the output of ArtTree#iterator
  def toSortable[V](iter: Iterator[(Array[Byte], Object)]): Seq[(Seq[Byte], V)] =
    iter.map { case (k, v) => (k.toSeq, v.asInstanceOf[V]) }.toSeq

  /** Generates random null-terminated keys. */
  def key_list(): Seq[Seq[Byte]] = {
    val n = r.nextInt(max_n)
    Seq.fill(n) {
      val key_len = r.nextInt(max_key_len)
      Seq.fill(key_len) { (r.nextInt(255) + 1).toByte } :+ 0.toByte
    }
  }

  test("create, destroy") {
    val t = new ArtTree
    assert(t.size == 0)
    t.destroy()
    assert(t.size == 0)
  }

  test("insert, search") {
    val t = new ArtTree
    val keys = key_list().toSet.toSeq
    val holdout = 10
    for (i <- 0 until keys.size - holdout) {
      val k = keys(i).toArray
      assert(t.search(k) == null)
      t.insert(k, i)
      assert(t.search(k) == i)
    }
    assert(t.size == keys.size - holdout);
    for (i <- keys.size - holdout until keys.size) {
      assert(t.search(keys(i).toArray) == null)
    }
  }

  test("insert, delete") {
    val t = new ArtTree
    val keys = key_list().toSet.toSeq
    for (i <- 0 until keys.size) {
      val k = keys(i).toArray
      assert(t.search(k) == null)
      t.insert(k, i)
      assert(t.search(k) == i)
    }
    assert(t.size == keys.size);
    for (i <- 0 until keys.size) {
      val k = keys(i).toArray
      assert(t.search(k) == i)
      t.delete(k)
      assert(t.search(k) == null)
    }
    assert(t.size == 0);
  }

  test("iterator") {
    val t = new ArtTree
    val keys = key_list().toSet.toSeq
    for (i <- 0 until keys.size) {
      t.insert(keys(i).toArray, i)
    }
    assert(toSortable[Int](t.iterator) == keys.zipWithIndex.sorted)
  }

  test("prefix iterator") {
    val t = new ArtTree
    val keys = key_list().toSet.toSeq
    for (i <- 0 until keys.size) {
      t.insert(keys(i).toArray, i)
    }
    // Whole tree
    assert(toSortable[Int](t.prefixIterator(Array.empty[Byte])) == keys.zipWithIndex.sorted)
    // Each key
    for (i <- 0 until keys.size) {
      assert(toSortable[Int](t.prefixIterator(keys(i).toArray)) == Seq((keys(i), i)))
    }
    // Random prefixes
    for (trial <- 0 until 100) {
      val prefix_len = r.nextInt(max_key_len)
      val prefix = Seq.fill(prefix_len) { (r.nextInt(255) + 1).toByte }
      assert(toSortable[Int](t.prefixIterator(prefix.toArray)) ==
        keys.zipWithIndex.sorted.filter { case (k, i) => k.startsWith(prefix) })
    }
  }

  test("snapshot") {
    val a = new ArtTree
    val aKeys = key_list().toSet.toSeq
    for (k <- aKeys) {
      a.insert(k.toArray, 1)
    }

    val b = a.snapshot()
    assert(toSortable[Int](b.iterator) == toSortable[Int](a.iterator))

    val bKeys = key_list().toSet.toSeq
    for (k <- bKeys) {
      b.insert(k.toArray, 2)
    }
    def checkB() {
      var i = 0
      for (k <- aKeys ++ bKeys) {
        val bVal = b.search(k.toArray).asInstanceOf[Int]
        assert(bVal == 1 || bVal == 2)
        i += 1
      }
    }
    checkB()

    val c = b.snapshot()
    for (k <- aKeys) {
      c.delete(k.toArray)
    }
    def checkC() {
      assert(toSortable[Int](c.iterator) ==
        (bKeys.toSet -- aKeys.toSet).toSeq.map(x => (x, 2)).sorted)
    }
    checkB(); checkC()

    val d = c.snapshot()
    for (k <- bKeys) {
      d.delete(k.toArray)
    }
    def checkD() {
      assert(d.size == 0)
      assert(d.iterator.toSeq == Seq.empty)
    }
    checkB(); checkC(); checkD()
  }

  test("keys cannot be prefixes of other keys") {
    val t = new ArtTree
    val a = Array[Byte]()
    val b = Array[Byte](0)
    val c = Array[Byte](0, 0)

    t.insert(a, 1)
    assert(t.search(a) == 1)
    assert(t.search(b) == null)

    intercept[UnsupportedOperationException] {
      t.insert(b, 1)
    }
    assert(t.search(a) == 1)
    assert(t.search(b) == null)

    t.delete(a)
    t.insert(c, 1)
    assert(t.search(a) == null)
    assert(t.search(b) == null)
    assert(t.search(c) == 1)

    intercept[UnsupportedOperationException] {
      t.insert(a, 1)
    }
    intercept[UnsupportedOperationException] {
      t.insert(b, 1)
    }
    assert(t.search(a) == null)
    assert(t.search(b) == null)
    assert(t.search(c) == 1)
}

}
