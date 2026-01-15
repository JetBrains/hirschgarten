package org.jetbrains.bazel.test.framework

object CartesianProduct {
  fun <T, U> make2d(c1: Iterable<T>, c2 : Iterable<U>): Iterable<Pair<T, U>> {
    return c1.flatMap { t1 -> c2.map { t2 -> t1 to t2 } }
  }
}
