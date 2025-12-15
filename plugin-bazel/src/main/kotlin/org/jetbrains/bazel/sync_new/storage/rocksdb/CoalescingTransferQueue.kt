package org.jetbrains.bazel.sync_new.storage.rocksdb

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class CoalescingTransferQueue<K : Any, V : Any> {
  private data class Node<K, T>(val key: K, val value: T, val live: AtomicBoolean = AtomicBoolean(true))

  private val queue = LinkedTransferQueue<Node<K, V>>()
  private val last = ConcurrentHashMap<K, Node<K, V>>()

  val isEmpty: Boolean get() = queue.isEmpty()
  val size: Int get() = queue.size

  fun offer(key: K, value: V) {
    val node = Node(key, value)
    val prev = last.put(key, node)
    if (prev != null) {
      prev.live.set(false)
    }
    queue.offer(node)
  }

  fun poll(time: Long, unit: TimeUnit): V? {
    while (true) {
      val node = queue.poll(time, unit)
      if (node == null) {
        return null
      }
      if (consume(node)) {
        return node.value
      }
    }
  }

  fun drainTo(collection: MutableCollection<in V>, maxElements: Int): Int {
    var count = 0
    while (count < maxElements) {
      val node = queue.poll() ?: break
      if (consume(node)) {
        collection.add(node.value)
        count++
      }
    }
    return count
  }

  private fun consume(node: Node<K, V>): Boolean {
    if (!node.live.get()) {
      return false
    }
    if (!last.remove(node.key, node)) {
      return false
    }
    return true
  }
}
