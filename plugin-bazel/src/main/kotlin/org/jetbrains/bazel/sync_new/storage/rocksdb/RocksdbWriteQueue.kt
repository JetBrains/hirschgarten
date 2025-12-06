package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.progress.sleepCancellable
import kotlinx.coroutines.isActive
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.RocksDB
import org.rocksdb.WriteOptions
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

// offload serialization and rocksdb write operations to separate thread
// TODO: split into serialization queue and IO queue - this would require keeping order of operations,
//  so we could process serialization and writes in batches
//         <batch of writes>
//          |   |   |   |
//          |   |   |   | serialization threads<1..n>
//          |   |   |   |
//  <serialized sequenced key-value pairs>
//               |
//               |
//       <single writer thread>
//  -- now we are wasting resources by using only single thread for serialization
class RocksdbWriteQueue(
  private val project: Project,
  private val disposable: Disposable,
) : Disposable {
  companion object {
    private const val BUFFER_SHRINK_INTERVAL_MS = 60_000L // check for shrink every 60 seconds
    private const val BUFFER_SHRINK_GAP_THRESHOLD = 1024
    private const val BUFFER_INITIAL_CAPACITY = 1024 * 1024

    // TODO: check how native buffers are handled, can I just discard them after put/remove?
    private val WRITE_OPTIONS: WriteOptions = WriteOptions()
      .setSync(false)
  }

  private val running: AtomicBoolean = AtomicBoolean(true)
  private val exited: AtomicBoolean = AtomicBoolean(false)
  private val thread: Thread
  private val latch: CountDownLatch = CountDownLatch(1)
  private val queue: BlockingQueue<WriteOperation> = LinkedBlockingQueue()

  data class ThreadByteBuffer(
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
    while (running.opaque) {
      val operation = queue.poll()
      when (operation) {
        is WriteOperation.WritePlain<*, *> -> {
          handleWriteOperation(operation, threadLocalBuffer)
        }

        is WriteOperation.RemovePlain<*, *> -> {
          handleRemoveOperation(operation, threadLocalBuffer)
        }

        WriteOperation.BufferShrinkTick -> {
          handleBufferShrinkTick(threadLocalBuffer)
        }

        WriteOperation.ShutdownPoison -> {
          break
        }
      }
    }
    latch.countDown()
  }

  private fun handleBufferShrinkTick(threadLocalBuffer: ThreadByteBuffer) {
    val timestamp = System.currentTimeMillis()
    if ((timestamp - threadLocalBuffer.lastShrinkCheck) > BUFFER_SHRINK_INTERVAL_MS) {
      threadLocalBuffer.buffer1.shrinkToSize(threshold = BUFFER_SHRINK_GAP_THRESHOLD)
      threadLocalBuffer.buffer2.shrinkToSize(threshold = BUFFER_SHRINK_GAP_THRESHOLD)
      threadLocalBuffer.lastShrinkCheck = timestamp
    }
  }

  private fun <K, V> handleWriteOperation(
    operation: WriteOperation.WritePlain<K, V>,
    threadLocalBuffer: ThreadByteBuffer,
  ) {
    val desc = operation.desc
    val buffer1 = threadLocalBuffer.buffer1
    val buffer2 = threadLocalBuffer.buffer2

    // write key
    buffer1.reset()
    desc.keyCodec.encode(UnsafeCodecContext, buffer1, operation.key)

    // write value
    buffer2.reset()
    desc.valueCodec.encode(UnsafeCodecContext, buffer2, operation.value)

    desc.db.put(desc.handle, WRITE_OPTIONS, buffer1.buffer, buffer2.buffer)
  }

  private fun <K, V> handleRemoveOperation(operation: WriteOperation.RemovePlain<K, V>, threadLocalBuffer: ThreadByteBuffer) {
    val buffer1 = threadLocalBuffer.buffer1
    val desc = operation.desc

    buffer1.reset()
    desc.keyCodec.encode(UnsafeCodecContext, buffer1, operation.key)

    desc.db.delete(desc.handle, WRITE_OPTIONS, buffer1.buffer)
  }

  fun enqueue(operation: WriteOperation) {
    queue.put(operation)
  }

  fun awaitExit(duration: Duration = Duration.ofMinutes(5)): Boolean {
    if (!exited.compareAndSet(false, true)) {
      return true
    }
    enqueue(WriteOperation.ShutdownPoison)
    return latch.await(duration.toMillis(), TimeUnit.MILLISECONDS)
  }

  override fun dispose() {
    // TODO: handle this case properly
    if (!awaitExit()) {
      error("Failed to exit write queue thread within time constrain")
    }
  }
}

sealed interface WriteOperation {
  data class WritePlain<K, V>(val desc: WriteOperationDesc<K, V>, val key: K, val value: V) : WriteOperation
  data class RemovePlain<K, V>(val desc: WriteOperationDesc<K, V>, val key: K) : WriteOperation
  object BufferShrinkTick : WriteOperation
  object ShutdownPoison : WriteOperation
}

data class WriteOperationDesc<K, V>(
  val db: RocksDB,
  val handle: ColumnFamilyHandle,
  val keyCodec: Codec<K>,
  val valueCodec: Codec<V>,
)
