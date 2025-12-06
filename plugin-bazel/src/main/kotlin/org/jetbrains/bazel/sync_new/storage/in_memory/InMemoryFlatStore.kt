package org.jetbrains.bazel.sync_new.storage.in_memory

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.codec.codecBuilderOf
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.FlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.PersistentStoreWithModificationMarker
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle

class InMemoryFlatStore<T>(
  private val owner: PersistentStoreOwner,
  override val name: String,
  private val creator: () -> T,
  private val codec: Codec<T>,
) : FlatStorage<T>, FlatPersistentStore, PersistentStoreWithModificationMarker, Disposable {

  companion object {
    private val VALUE_HANDLE: VarHandle = MethodHandles.lookup()
      .findVarHandle(InMemoryFlatStore::class.java, "value", Any::class.java)
    const val CODEC_VERSION: Int = 1
  }

  init {
    owner.register(this)
  }

  @Suppress("unused")
  private var value: T? = null
  private val lock = Any()
  private var init = false

  override fun get(): T {
    if (!init) {
      synchronized(lock) {
        if (!init) {
          set(creator())
        }
      }
    }
    @Suppress("UNCHECKED_CAST")
    return VALUE_HANDLE.getVolatile(this) as T
  }


  override fun set(value: T) {
    VALUE_HANDLE.setVolatile(this, value)
    init = true
    wasModified = true
  }

  override fun modify(op: (value: T) -> T): T {
    while (true) {
      val current = get()
      val updated = op(current)
      if (updated === current) {
        wasModified = true
        return updated
      }
      if (VALUE_HANDLE.compareAndSet(this, current, updated)) {
        // assume every modify operation ends up modifying the value
        wasModified = true
        return updated
      }
    }
  }

  override fun reset() {
    set(creator())
  }

  override fun mark() {
    wasModified = true
  }

  override fun write(ctx: CodecContext, buffer: CodecBuffer) {
    buffer.writeVarInt(CODEC_VERSION)
    codec.encode(ctx, buffer, get())
    wasModified = false
  }

  override fun read(ctx: CodecContext, buffer: CodecBuffer) {
    check(buffer.readVarInt() == CODEC_VERSION) { "unsupported version version" }
    try {
      set(codec.decode(ctx, buffer))
    } catch (ex: Throwable) {
      logger<InMemoryFlatStore<T>>().warn("failed to read $name, ignoring", ex)
    }
  }

  @field:Volatile
  override var wasModified: Boolean = false
    private set

  override fun dispose() {
    owner.unregister(this)
  }
}

class InMemoryFlatStoreBuilder<T>(
  private val owner: PersistentStoreOwner,
  private val name: String,
) : FlatStoreBuilder<T> {
  private var creator: (() -> T)? = null
  private var codec: Codec<T>? = null

  override fun withCreator(func: () -> T): FlatStoreBuilder<T> {
    creator = func
    return this
  }

  override fun withCodec(codec: CodecBuilder.() -> Codec<T>): FlatStoreBuilder<T> {
    this.codec = codec(codecBuilderOf())
    return this
  }

  override fun build(): FlatStorage<T> {
    return InMemoryFlatStore(
      owner = owner,
      name = name,
      creator = creator ?: error("Creator function must be specified"),
      codec = codec ?: error("Codec must be specified"),
    )
  }

}
