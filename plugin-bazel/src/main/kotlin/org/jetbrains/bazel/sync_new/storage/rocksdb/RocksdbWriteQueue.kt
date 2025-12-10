package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.progress.sleepCancellable
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import kotlinx.coroutines.isActive
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.RocksDB
import org.rocksdb.WriteBatch
import org.rocksdb.WriteOptions
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RocksdbWriteQueue(
  private val project: Project,
  private val disposable: Disposable,
) : Disposable {
  companion object {
    private const val BUFFER_SHRINK_INTERVAL_MS = 60_000L
    private const val BUFFER_SHRINK_GAP_THRESHOLD = 1024
    private const val BUFFER_INITIAL_CAPACITY = 1024 * 1024
    private const val QUEUE_CAPACITY = 128 * 1024
    private const val WRITE_BATCH_SIZE = 4096

    private val WRITE_OPTIONS: WriteOptions = WriteOptions()
      .setSync(false)
      .setDisableWAL(true)
  }

  private val running: AtomicBoolean = AtomicBoolean(true)
  private val exited: AtomicBoolean = AtomicBoolean(false)
  private val thread: Thread
  private val latch: CountDownLatch = CountDownLatch(1)
  private val queue: BlockingQueue<WriteOperation> = ArrayBlockingQueue(QUEUE_CAPACITY)
  private val tlsWriteBatch = ThreadLocal.withInitial { WriteBatch() }

  class ThreadByteBuffer(
    val buffer1: UnsafeByteBufferCodecBuffer,
    val buffer2: UnsafeByteBufferCodecBuffer,
    var lastShrinkCheck: Long,
  )

  private var buffer: ThreadLocal<ThreadByteBuffer> = ThreadLocal.withInitial {
    ThreadByteBuffer(
      buffer1 = UnsafeByteBufferCodecBuffer(ByteBuffer.allocateDirect(BUFFER_INITIAL_CAPACITY)),
      buffer2 = UnsafeByteBufferCodecBuffer(ByteBuffer.allocateDirect(BUFFER_INITIAL_CAPACITY)),
      lastShrinkCheck = System.currentTimeMillis(),
    )
  }

  init {
    thread = Thread(this::task)
    thread.name = "bazel rocksdb write queue"
    thread.start()

    Disposer.register(disposable, this)

    // shrink ticker
    BazelCoroutineService.getInstance(project)
      .start {
        while (isActive) {
          enqueue(WriteOperation.BufferShrinkTick)
          sleepCancellable(BUFFER_SHRINK_INTERVAL_MS)
        }
      }
  }

  private fun task() {
    val threadLocalBuffer = buffer.get()
    val batch = ArrayList<WriteOperation>(WRITE_BATCH_SIZE)
    while (running.opaque) {
      val operation = queue.poll(100, TimeUnit.MILLISECONDS)
      if (operation == null) {
        if (batch.isNotEmpty()) {
          if (handleOperationsBatch(batch, threadLocalBuffer)) {
            break
          }
          batch.clear()
        }
        continue
      }
      batch.add(operation)
      queue.drainTo(batch, WRITE_BATCH_SIZE - batch.size)
      if (batch.size >= WRITE_BATCH_SIZE) {
        if (handleOperationsBatch(batch, threadLocalBuffer)) {
          break
        }
        batch.clear()
      }
    }
    if (batch.isNotEmpty()) {
      handleOperationsBatch(batch, threadLocalBuffer)
      batch.clear()
    }
    if (queue.drainTo(batch) > 0) {
      handleOperationsBatch(batch, threadLocalBuffer)
    }
    latch.countDown()
  }

  private fun handleOperationsBatch(batch: MutableList<WriteOperation>, threadLocalBuffer: ThreadByteBuffer): Boolean {
    val operationByDb = Reference2ObjectLinkedOpenHashMap<WriteOperationDesc<*, *>, ReferenceArrayList<WriteOperation>>()
    var shutdown = false
    for (operation in batch) {
      when (operation) {
        is WriteOperation.RemovePlain<*, *> -> {
          operationByDb.computeIfAbsent(operation.desc) { ReferenceArrayList() }.add(operation)
        }

        is WriteOperation.WritePlain<*, *> -> {
          operationByDb.computeIfAbsent(operation.desc) { ReferenceArrayList() }.add(operation)
        }

        WriteOperation.BufferShrinkTick -> {
          handleBufferShrinkTick(threadLocalBuffer)
        }

        WriteOperation.ShutdownPoison -> {
          shutdown = true
        }

        is WriteOperation.Barrier -> {
          for ((desc, operation) in operationByDb) {
            handleBatch(desc, operation)
          }
          operationByDb.clear()
          operation.callback()
        }
      }
    }
    for ((desc, operations) in operationByDb) {
      handleBatch(desc, operations)
    }
    return shutdown
  }

  private fun <K, V> handleBatch(desc: WriteOperationDesc<K, V>, operations: ReferenceArrayList<WriteOperation>) {
    val buffer = buffer.get()
    val keyBuffer = buffer.buffer1
    val valueBuffer = buffer.buffer2
    val batch = tlsWriteBatch.get()
    batch.clear()
    
    try {
      for (operation in operations) {
        when (operation) {
          is WriteOperation.RemovePlain<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            operation as WriteOperation.RemovePlain<K, V>

            keyBuffer.clear()
            desc.keyCodec.encode(UnsafeCodecContext, keyBuffer, operation.key)
            keyBuffer.flip()

            batch.delete(desc.handle, keyBuffer.buffer)
            operation.callback(operation.key)
          }

          is WriteOperation.WritePlain<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            operation as WriteOperation.WritePlain<K, V>

            keyBuffer.clear()
            desc.keyCodec.encode(UnsafeCodecContext, keyBuffer, operation.key)
            keyBuffer.flip()

            valueBuffer.clear()
            desc.valueCodec.encode(UnsafeCodecContext, valueBuffer, operation.value)
            valueBuffer.flip()

            batch.put(desc.handle, keyBuffer.buffer, valueBuffer.buffer)
            operation.callback(operation.key, operation.value)
          }

          else -> {
            /* noop */
          }
        }
      }

      desc.db.write(WRITE_OPTIONS, batch)
    } finally {
      batch.clear()
    }
  }

  private fun handleBufferShrinkTick(threadLocalBuffer: ThreadByteBuffer) {
    val timestamp = System.currentTimeMillis()
    if ((timestamp - threadLocalBuffer.lastShrinkCheck) > BUFFER_SHRINK_INTERVAL_MS) {
      threadLocalBuffer.buffer1.shrinkToSize(threshold = BUFFER_SHRINK_GAP_THRESHOLD)
      threadLocalBuffer.buffer2.shrinkToSize(threshold = BUFFER_SHRINK_GAP_THRESHOLD)
      threadLocalBuffer.lastShrinkCheck = timestamp
    }
  }

  fun enqueue(operation: WriteOperation) {
    var backoff = 1L
    while (!queue.offer(operation, backoff, TimeUnit.MILLISECONDS)) {
      if (!running.opaque) {
        error("Write queue is shutting down")
      }
      backoff = maxOf(backoff * 2, 1000L)
    }
  }

  fun awaitExit(duration: Duration = Duration.ofMinutes(5)): Boolean {
    if (!exited.compareAndSet(false, true)) {
      return true
    }
    enqueue(WriteOperation.ShutdownPoison)
    return latch.await(duration.toMillis(), TimeUnit.MILLISECONDS)
  }

  override fun dispose() {
    if (!awaitExit()) {
      error("failed to exit write queue thread within time constrain")
    }
  }
}

sealed interface WriteOperation {
  data class WritePlain<K, V>(
    val desc: WriteOperationDesc<K, V>,
    val key: K,
    val value: V,
    val callback: (key: K, value: V) -> Unit,
  ) : WriteOperation

  data class RemovePlain<K, V>(
    val desc: WriteOperationDesc<K, V>,
    val key: K,
    val callback: (key: K) -> Unit,
  ) : WriteOperation

  data class Barrier(val callback: () -> Unit) : WriteOperation

  object BufferShrinkTick : WriteOperation
  object ShutdownPoison : WriteOperation
}

data class WriteOperationDesc<K, V>(
  val db: RocksDB,
  val handle: ColumnFamilyHandle,
  val keyCodec: Codec<K>,
  val valueCodec: Codec<V>,
)
