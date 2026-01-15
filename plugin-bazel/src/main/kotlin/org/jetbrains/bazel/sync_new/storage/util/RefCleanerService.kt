package org.jetbrains.bazel.sync_new.storage.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import org.jetbrains.bazel.sync_new.storage.RefCloser
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

@Service(Service.Level.PROJECT)
class RefCleanerService : RefCloser, Disposable {
  class CloseableRef : PhantomReference<Any> {
    internal val closer: () -> Unit

    constructor(obj: Any, queue: ReferenceQueue<Any>, closer: () -> Unit) : super(obj, queue) {
      this.closer = closer
    }
  }

  @field:Volatile
  private var running = true
  private val shutdownLatch = CountDownLatch(1)
  private val refs = mutableSetOf<CloseableRef>()
  private val refQueue = ReferenceQueue<Any>()

  init {
    thread(
      name = "BazelRefCleaner",
      start = true,
      block = this::task
    )
  }

  override fun register(obj: Any, closer: () -> Unit) {
    val ref = CloseableRef(obj, refQueue, closer)
    synchronized(refs) {
      refs += ref
    }
  }

  override fun dispose() {
    running = false
    shutdownLatch.await()
  }

  private fun task() {
    while (running && !Thread.interrupted()) {
      try {
        val ref = refQueue.remove() as? CloseableRef
        if (ref != null) {
          ref.closer()
          synchronized(refs) {
            refs -= ref
          }
        }
      } catch (_: InterruptedException) {
        break
      }
    }
    shutdownLatch.countDown()
  }

  fun processQueue() {
    while (true) {
      val ref = refQueue.poll() as? CloseableRef ?: break
      ref.closer()
      synchronized(refs) {
        refs -= ref
      }
    }
  }
}
