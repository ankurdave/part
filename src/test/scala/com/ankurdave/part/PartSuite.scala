package com.ankurdave.part

import scala.util.Random
import scala.collection.mutable.WrappedArray
import scala.collection.JavaConversions._

import org.scalatest.FunSuite

class PartSuite extends FunSuite {

  val max_n = 10000;
  val max_key_len = 100;
  val r = new Random

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
    val tElems: Seq[(Seq[Byte], Int)] = t.iterator.map { case (k, v) => (k.toSeq, v.asInstanceOf[Int]) }.toSeq
    import scala.math.Ordering.Implicits._
    implicit val byteOrdering = Ordering.by[Byte, Int](b => Node.to_uint(b))
    assert(tElems == keys.zipWithIndex.sorted)
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
