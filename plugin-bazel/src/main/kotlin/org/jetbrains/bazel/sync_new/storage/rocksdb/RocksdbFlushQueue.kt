package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferObjectPool
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.rocksdb.RocksDB
import org.rocksdb.WriteBatch
import org.rocksdb.WriteOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class RocksdbFlushQueue(
  private val db: RocksDB,
  private val disposable: Disposable,
) : Disposable {
  companion object {
    private const val QUEUE_POLL_TIMEOUT = 100L
    private const val FLUSH_BATCH_SIZE = 1024

    private val WRITE_OPTIONS: WriteOptions = WriteOptions()
      .setSync(false)
      .setDisableWAL(true)
  }

  private val queue: CoalescingTransferQueue<CoalescenceKey, FlushOperation> = CoalescingTransferQueue()
  private val running: AtomicBoolean = AtomicBoolean(true)
  private val shutdownLatch = CountDownLatch(1)
  private val operationId: AtomicLong = AtomicLong(0)

  init {
    val thread = Thread { task() }
    thread.name = "bazel rocksdb flush queue"
    thread.start()
    Disposer.register(disposable, this)
  }

  fun enqueue(operation: FlushOperation) {
    val key = when (operation) {
      is FlushOperation.FlushDirty<*, *> -> CoalescenceKey.Flush(operation.key)
      else -> CoalescenceKey.Propagate(operationId.incrementAndGet())
    }
    queue.offer(key, operation)
  }

  private fun task() {
    val batch = ReferenceArrayList<FlushOperation>(FLUSH_BATCH_SIZE)
    while (running.get()) {
      try {
        val operation = queue.poll(QUEUE_POLL_TIMEOUT, TimeUnit.MILLISECONDS)
        if (operation == null) {
          if (batch.isNotEmpty()) {
            if (handleBatch(batch)) {
              break
            }
            batch.clear()
          }
          continue
        }
        batch.add(operation)
        queue.drainTo(batch, FLUSH_BATCH_SIZE - batch.size)
        if (batch.size >= FLUSH_BATCH_SIZE) {
          if (handleBatch(batch)) {
            break
          }
          batch.clear()
        }
      } catch (ex: Throwable) {
        ex.printStackTrace()
      } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        break
      }
    }
    handleBatch(batch)
    shutdownLatch.countDown()
  }

  private fun handleBatch(batch: ReferenceArrayList<FlushOperation>): Boolean {
    val operationByStore = Reference2ObjectLinkedOpenHashMap<RocksdbKVStore<*, *>, ArrayDeque<FlushOperation>>()
    var shutdown = false
    for (operation in batch) {
      when (operation) {
        is FlushOperation.Barrier<*, *> -> {
          operationByStore.computeIfAbsent(operation.store) { ArrayDeque() }.add(operation)
        }

        is FlushOperation.FlushDirty<*, *> -> {
          operationByStore.computeIfAbsent(operation.store) { ArrayDeque() }.add(operation)
        }

        FlushOperation.Shutdown -> shutdown = true
      }
    }
    for ((store, ops) in operationByStore) {
      handleStoreBatch(store, ops)
    }
    return shutdown
  }

  private fun <K : Any, V : Any> handleStoreBatch(store: RocksdbKVStore<K, V>, operations: ArrayDeque<FlushOperation>) {
    WriteBatch().use { batch ->
      while (operations.isNotEmpty()) {
        val operation = operations.removeFirst()
        when (operation) {
          is FlushOperation.Barrier<*, *> -> {
            operation as FlushOperation.Barrier<K, V>
            handleStoreBatch(operation.store, operations)
            operation.callback()
            break
          }

          is FlushOperation.FlushDirty<*, *> -> {
            operation as FlushOperation.FlushDirty<K, V>
            val action = store.consumeKey(operation.key)
            when (action) {
              is KeyAction.Noop<K, V> -> {}
              is KeyAction.Overwrite<K, V> -> {
                UnsafeByteBufferObjectPool.use { keyBuf, valueBuf ->
                  store.keyCodec.encode(UnsafeCodecContext, keyBuf, operation.key)
                  keyBuf.flip()
                  store.valueCodec.encode(UnsafeCodecContext, valueBuf, action.value)
                  valueBuf.flip()
                  batch.put(keyBuf.buffer, valueBuf.buffer)
                }
              }
              is KeyAction.Remove<K, V> -> {
                UnsafeByteBufferObjectPool.use { keyBuf ->
                  store.keyCodec.encode(UnsafeCodecContext, keyBuf, operation.key)
                  keyBuf.flip()
                  batch.delete(keyBuf.buffer)
                }
              }
            }
          }

          else -> {}
        }
      }
      db.write(WRITE_OPTIONS, batch)
    }
  }

  private fun shutdown(time: Long, unit: TimeUnit): Boolean {
    if (!running.compareAndSet(true, false)) {
      return true
    }
    enqueue(FlushOperation.Shutdown)
    return shutdownLatch.await(time, unit)
  }

  override fun dispose() {
    shutdown(1, TimeUnit.MINUTES)
  }
}

sealed interface CoalescenceKey {
  data class Flush(val key: Any) : CoalescenceKey
  data class Propagate(val id: Long) : CoalescenceKey
}

sealed interface FlushOperation {
  data class FlushDirty<K : Any, V : Any>(
    val store: RocksdbKVStore<K, V>,
    val key: K,
  ) : FlushOperation

  data class Barrier<K : Any, V : Any>(
    val store: RocksdbKVStore<K, V>,
    val callback: () -> Unit,
  ) : FlushOperation

  data object Shutdown : FlushOperation
}
