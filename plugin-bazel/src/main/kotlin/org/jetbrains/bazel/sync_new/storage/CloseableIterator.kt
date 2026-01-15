package org.jetbrains.bazel.sync_new.storage

import java.util.concurrent.atomic.AtomicBoolean

/**
 * CloseableIterator is an iterator that can be closed, ensuring resources are released properly.
 *
 * **Warning:** If you create an empty iterator, there is no guarantee it will be closed automatically.
 * Consumers may check [hasNext] before using the iterator, and if it returns `false`, they might
 * not call [close]. Always ensure proper resource cleanup by using try-with-resources or [use] blocks.
 *
 * **Warning:** For implementation make sure that before calling [hasNext] iterator does not allocate any resources.
 */
interface CloseableIterator<T> : Iterator<T>, AutoCloseable

fun <T> CloseableIterator<T>.useSequence(block: (seq: Sequence<T>) -> Unit) {
  use { block(asSequence()) }
}

fun <T> CloseableIterator<T>.asClosingSequence(): Sequence<T> = sequence {
  use { iter ->
    while (iter.hasNext()) {
      yield(iter.next())
    }
  }
}

fun <T> Iterator<T>.asCloseable(): CloseableIterator<T> = object : CloseableIterator<T> {
  override fun next(): T = this@asCloseable.next()
  override fun hasNext(): Boolean = this@asCloseable.hasNext()
  override fun close() {
    /* noop */
  }
}

fun <T> Sequence<T>.asCloseableIterator(): CloseableIterator<T> = iterator().asCloseable()

interface RefCloser {
  fun register(obj: Any, closer: () -> Unit)
}

abstract class ImplicitCloseableIterator<V, H : AutoCloseable?>(closer: RefCloser): CloseableIterator<V> {
  private val guard = AtomicBoolean(false)

  abstract val handle: H

  init {
    // not using lambda to avoid implicit captures
    closer.register(this, CaptureCloseHandler(handle, guard))
  }

  override fun close() {
    if (guard.compareAndSet(false, true)) {
      handle?.close()
    }
  }

}

private class CaptureCloseHandler(val closeable: AutoCloseable?, val guard: AtomicBoolean) : () -> Unit {
  override fun invoke() {
    if (guard.compareAndSet(false, true)) {
      closeable?.close()
    }
  }
}
