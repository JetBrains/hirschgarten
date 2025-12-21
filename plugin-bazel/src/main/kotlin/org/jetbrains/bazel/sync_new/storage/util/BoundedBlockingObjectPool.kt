package org.jetbrains.bazel.sync_new.storage.util

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

internal class BoundedBlockingObjectPool<T : Any>(
  private val maxSize: Int,
  private val creator: () -> T,
) {
  private sealed interface Value<T> {
    data class Cold<T>(val lazy: Lazy<T>) : Value<T>
    data class Hot<T>(val eager: T) : Value<T>
  }

  private val queue = ArrayBlockingQueue<Value<T>>(maxSize)

  init {
    // TODO: use not-synchronized lazy
    repeat(maxSize) { queue.put(Value.Cold(lazy { creator() })) }
  }

  fun acquire(time: Long, unit: TimeUnit): T? {
    val value = if (time <= 0) {
      queue.take()
    }
    else {
      queue.poll(time, unit)
    }
    return when (value) {
      is Value.Cold -> value.lazy.value
      is Value.Hot -> value.eager
    }
  }

  fun release(value: T) {
    queue.put(Value.Hot(value))
  }

  inline fun use(
    time: Long = 0,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    block: (T) -> Unit,
  ) {
    val value = acquire(time, unit) ?: return
    try {
      block(value)
    }
    finally {
      release(value)
    }
  }
}
