package org.jetbrains.bazel.sync_new.storage.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

internal class BoundedBlockingObjectPoolTest {

  @Test
  fun `test simple acquire and release`() {
    val pool = BoundedBlockingObjectPool(maxSize = 2) { "Object" }

    val obj1 = pool.acquire(0, TimeUnit.MILLISECONDS)
    obj1.shouldNotBe(null)
    obj1.shouldBe("Object")

    val obj2 = pool.acquire(0, TimeUnit.MILLISECONDS)
    obj2.shouldNotBe(null)
    obj2.shouldBe("Object")

    pool.release(obj1!!)
    pool.release(obj2!!)

    val obj3 = pool.acquire(0, TimeUnit.MILLISECONDS)
    obj3.shouldNotBe(null)
  }

  @Test
  fun `test lazy initialization`() {
    val creationCount = AtomicInteger(0)
    val pool = BoundedBlockingObjectPool(maxSize = 3) {
      creationCount.incrementAndGet()
      "Object-${creationCount.get()}"
    }

    creationCount.get().shouldBe(0)

    val obj1 = pool.acquire(0, TimeUnit.MILLISECONDS)
    creationCount.get().shouldBe(1)
    obj1.shouldBe("Object-1")

    val obj2 = pool.acquire(0, TimeUnit.MILLISECONDS)
    creationCount.get().shouldBe(2)
    obj2.shouldBe("Object-2")

    pool.release(obj1!!)
    pool.release(obj2!!)

    val obj3 = pool.acquire(0, TimeUnit.MILLISECONDS)
    creationCount.get().shouldBe(2)
  }

  @Test
  fun `test pool exhaustion with timeout`() {
    val pool = BoundedBlockingObjectPool(maxSize = 1) { "Object" }

    val obj1 = pool.acquire(0, TimeUnit.MILLISECONDS)
    obj1.shouldNotBe(null)

    val obj2 = pool.acquire(100, TimeUnit.MILLISECONDS)
    obj2.shouldBe(null)

    pool.release(obj1!!)

    val obj3 = pool.acquire(100, TimeUnit.MILLISECONDS)
    obj3.shouldNotBe(null)
  }

  @Test
  fun `test concurrent access`() {
    val poolSize = 5
    val pool = BoundedBlockingObjectPool(maxSize = poolSize) { AtomicInteger(0) }
    val threadCount = 20
    val latch = CountDownLatch(threadCount)
    val errors = mutableListOf<Throwable>()

    val threads = (1..threadCount).map {
      thread {
        try {
          repeat(10) {
            val obj = pool.acquire(1000, TimeUnit.MILLISECONDS)
            obj.shouldNotBe(null)
            obj!!.incrementAndGet()
            Thread.sleep(10)
            pool.release(obj)
          }
        }
        catch (e: Throwable) {
          synchronized(errors) { errors.add(e) }
        }
        finally {
          latch.countDown()
        }
      }
    }

    latch.await(30, TimeUnit.SECONDS)
    threads.forEach { it.join(1000) }

    errors.shouldBeEmpty()
  }

  @Test
  fun `test use helper function`() {
    val pool = BoundedBlockingObjectPool(maxSize = 1) { StringBuilder("Test") }
    var result = ""

    pool.use { obj ->
      obj.append(" Value")
      result = obj.toString()
    }

    result.shouldBe("Test Value")

    pool.use { obj ->
      obj.toString().shouldBe("Test Value")
    }
  }

  @Test
  fun `test use with exception ensures release`() {
    val pool = BoundedBlockingObjectPool(maxSize = 1) { "Object" }

    shouldThrow<IllegalStateException> {
      pool.use {
        throw IllegalStateException("Test exception")
      }
    }

    val acquired = pool.acquire(100, TimeUnit.MILLISECONDS)
    acquired.shouldNotBe(null)
  }

}
