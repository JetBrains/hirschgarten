package org.jetbrains.bazel.sync_new.storage.rocksdb

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class BlockingMWSRCoalescingQueueTest {
  @Test
  fun `test queue coalescing`() {
    val queue = BlockingMWSRCoalescingQueue<Int>()

    queue.offer(1)
    queue.offer(2)
    queue.offer(3)
    queue.offer(3)
    queue.offer(1)
    queue.offer(4)

    val elements = mutableListOf<Int>()
    queue.drainTo(elements, Int.MAX_VALUE)

    elements shouldBe listOf(2, 3, 1, 4)
  }

  @Test
  fun `test producer consumer`() {
    val queue = BlockingMWSRCoalescingQueue<Int>()
    val producerLatch = CountDownLatch(1)
    val consumerLatch = CountDownLatch(1)
    val consumedItems = mutableListOf<Int>()
    val error = AtomicReference<Throwable>()

    val producer = Thread {
      try {
        queue.offer(1)
        queue.offer(2)
        queue.offer(3)
        queue.offer(3) // duplicate
        queue.offer(1) // duplicate
        queue.offer(4)
        producerLatch.countDown()
      } catch (e: Throwable) {
        error.set(e)
      }
    }

    val consumer = Thread {
      try {
        producerLatch.await()
        while (true) {
          val item = queue.take(100, TimeUnit.MILLISECONDS)
          if (item == null) break
          consumedItems.add(item)
        }
        consumerLatch.countDown()
      } catch (e: Throwable) {
        error.set(e)
      }
    }

    consumer.start()
    producer.start()

    producer.join(5000)
    consumer.join(5000)

    error.get()?.let { throw it }

    consumedItems shouldBe listOf(2, 3, 1, 4)
  }
}
