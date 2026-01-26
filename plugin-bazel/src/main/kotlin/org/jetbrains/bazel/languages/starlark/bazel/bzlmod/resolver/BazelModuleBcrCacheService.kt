package org.jetbrains.bazel.languages.starlark.bazel.bzlmod.resolver

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@State(name = "BazelBcrCache", storages = [Storage("bazelModuleRegistryCache.xml")])
@Service(Service.Level.PROJECT)
class BazelModuleBcrCacheService: PersistentStateComponent<BazelModuleBcrCacheService.CacheState> {
  class CacheState : BaseState() {
    @get:XMap
    var cacheState: MutableMap<String, CachedValue> by map()
  }

  @Tag("cachedState")
  data class CachedValue(
    @JvmField var values: List<String> = emptyList(),
    @JvmField var lastUpdated: Long = 0,
  )

  internal val cache: Cache<String, CachedValue> =
    Caffeine
      .newBuilder()
      .expireAfter(expiry)
      .executor { command -> ApplicationManager.getApplication().executeOnPooledThread(command) }
      .build()

  private val keyLocks = ConcurrentHashMap<String, Mutex>()

  override fun getState(): CacheState = CacheState().also { it.cacheState.putAll(cache.asMap()) }

  override fun loadState(state: CacheState) {
    cache.invalidateAll()
    cache.putAll(state.cacheState)
  }

  /**
   * Returns cached values if present.
   * Otherwise, schedules a single asynchronous load per key and awaits it (no thread blocking).
   */
  suspend fun getOrFetch(key: String, supplier: suspend () -> List<String>?): List<String> {
    cache.getIfPresent(key)?.let { return it.values }

    // To ensure only one thread is loading the value for a given key at a time, we use a mutex per key.
    // Then other threads will wait for the value to be loaded and get it from the cache.
    return keyLocks.computeIfAbsent(key) { Mutex() }.withLock {
      cache.getIfPresent(key)?.let { return@withLock it.values }
      val loaded = supplier() ?: return@withLock emptyList()
      cache.put(key, CachedValue(loaded, System.currentTimeMillis()))
      loaded
    }
  }

  fun invalidate(key: String) {
    cache.invalidate(key)
    keyLocks.remove(key)
  }

  companion object {
    private val CACHE_EVICTION_TIME = TimeUnit.HOURS.toMillis(12L)

    /**
     * Object to expire cached values after 12 hours.
     *
     * Since the cache is persistent, we need a custom Expiry parameter
     * (otherwise, in case of IDE reset, the expiration time would be renewed
     * and the data could remain valid for a very long time).
     * The implementation stores lastUpdated and calculates expiration from that
     * timestamp, so data is kept for 12 hours from the moment it is fetched
     * from BCR, regardless of how often the IDE is restarted.
     */
    private val expiry = object : Expiry<String, CachedValue> {
      override fun expireAfterCreate(
        key: String,
        value: CachedValue,
        currentTime: Long,
      ): Long = getDuration(value)

      override fun expireAfterUpdate(
        key: String,
        value: CachedValue,
        currentTime: Long,
        currentDuration: Long,
      ): Long = getDuration(value)

      override fun expireAfterRead(
        key: String,
        value: CachedValue,
        currentTime: Long,
        currentDuration: Long,
      ): Long = currentDuration
    }

    private fun getDuration(value: CachedValue): Long {
      val remaining = value.lastUpdated + CACHE_EVICTION_TIME - System.currentTimeMillis()
      return if (remaining <= 0) 0L else TimeUnit.MILLISECONDS.toNanos(remaining)
    }
  }
}
