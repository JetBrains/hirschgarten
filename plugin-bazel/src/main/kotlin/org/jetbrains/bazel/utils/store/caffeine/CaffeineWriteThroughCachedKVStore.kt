package org.jetbrains.bazel.utils.store.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.jetbrains.bazel.utils.store.KVStore

class CaffeineWriteThroughCachedKVStore<K : Any, V : Any>(
  private val inner: KVStore<K, V>,
  cacheFactory: () -> Cache<K, V?>
) : KVStore<K, V> by inner {
  private val cache = cacheFactory()

  override fun get(key: K): V? = cache.get(key) { inner.get(key) }

  override fun put(key: K, value: V): V? {
    cache.put(key, value)
    return inner.put(key, value)
  }

  override fun remove(key: K): V? {
    cache.invalidate(key)
    return inner.remove(key)
  }

  override fun clear() {
    cache.cleanUp()
    inner.clear()
  }

  companion object {
    fun <K : Any, V : Any> withSoftCache(inner: KVStore<K, V>) = CaffeineWriteThroughCachedKVStore(
      inner = inner,
      cacheFactory = {
        Caffeine.newBuilder()
          .softValues()
          .build()
      }
    )
  }
}
