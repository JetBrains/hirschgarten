package org.jetbrains.bazel.protobuf

import com.intellij.openapi.components.PersistentStateComponent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class LazyPersistentCachedStateComponent<T> : PersistentStateComponent<LazyPersistentCachedStateComponent.InnerState> {
  @Volatile
  private var _state: T? = null

  private var lock = ReentrantLock()

  val myState: T
    get() {
      if (_state == null) {
        try {
          lock.lock()
          if (_state == null) {
            _state = createIndex()
          }
        } finally {
          lock.unlock()
        }
      } else {
        return _state!!
      }
      return _state!!
    }


  override fun getState(): InnerState? {
    val state = _state ?: return InnerState(ByteArray(0))
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { stream -> encodeState(stream, state) }
    return InnerState(bytes.toByteArray())
  }

  override fun loadState(state: InnerState) {
    DataInputStream(ByteArrayInputStream(state.blob)).use { stream ->
      try {
        _state = decodeState(stream) ?: return
      } catch (_: Throwable) {
        // ignore exceptions
      }
    }
  }

  fun reindex() = lock.withLock {
    _state = createIndex()
  }

  protected abstract fun createIndex(): T
  protected abstract fun encodeState(stream: DataOutputStream, state: T)
  protected abstract fun decodeState(stream: DataInputStream): T?

  class InnerState(val blob: ByteArray = ByteArray(0))
}
