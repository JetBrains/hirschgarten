package org.jetbrains.bazel.languages.starlark.bazel.bzlmod.resolver

import com.github.benmanes.caffeine.cache.AsyncCache
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.component1
import kotlin.collections.component2

@State(name = "BazelBcrCache", storages = [Storage("bazelModuleRegistryCache.xml")])
@Service(Service.Level.PROJECT)
class BazelModuleBcrCacheService(private val coroutineScope: CoroutineScope) :
  PersistentStateComponent<BazelModuleBcrCacheService.CacheState> {
  class CacheState : BaseState() {
    @get:XMap
    var cacheState: MutableMap<String, CachedValueState> by map()
  }

  @Tag("cachedState")
  data class CachedValueState(
    @JvmField var values: List<String> = emptyList(),
    @JvmField var lastUpdated: Long = 0,
  )

  sealed interface CachedValue {
    data class Found(val values: List<String>, val lastUpdated: Long) : CachedValue

    object Unavailable : CachedValue
  }

  internal val cache: AsyncCache<String, CachedValue> =
    Caffeine
      .newBuilder()
      .expireAfter(expiry)
      .executor { command -> ApplicationManager.getApplication().executeOnPooledThread(command) }
      .buildAsync()

  override fun getState(): CacheState =
    CacheState().also { state ->
      val newMap =
        cache
          .synchronous()
          .asMap()
          .mapNotNull { (key, value) ->
            when (value) {
              is CachedValue.Found -> key to CachedValueState(value.values, value.lastUpdated)
              else -> null
            }
          }.toMap()
      state.cacheState.putAll(newMap)
    }

  override fun loadState(state: CacheState) {
    cache.synchronous().invalidateAll()
    val newMap =
      state.cacheState.mapValues { (_, value) ->
        CachedValue.Found(value.values, value.lastUpdated)
      }
    cache.synchronous().putAll(newMap)
  }

  /**
   * Returns cached values if present.
   * Otherwise, schedules a single asynchronous load per key and awaits it (no thread blocking).
   */
  suspend fun getOrFetch(key: String, supplier: suspend () -> List<String>?): List<String> {
    val cached = cache.synchronous().getIfPresent(key)
    if (cached is CachedValue.Found) return cached.values

    val fetched = cache.get(key) { _, _ -> loadAsync(supplier) }.asDeferred().await()
    return (fetched as? CachedValue.Found)?.values ?: emptyList()
  }

  /**
   * Asynchronous cache loader - bridges a suspend supplier into the CompletableFuture model
   * required by Caffeine.
   */
  private fun loadAsync(supplier: suspend () -> List<String>?): CompletableFuture<CachedValue> =
    coroutineScope.future {
      supplier()?.let { CachedValue.Found(it, System.currentTimeMillis()) }
        ?: CachedValue.Unavailable
    }

  fun invalidate(key: String) {
    cache.synchronous().invalidate(key)
  }

  companion object {
    private val CACHE_EVICTION_TIME = TimeUnit.HOURS.toMillis(12L)

    /**
     * Object to expire cached values after 12 hours.
     * Does not reset between ide sessions.
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

    private fun getDuration(value: CachedValue): Long = when (value) {
      is CachedValue.Found -> {
        val remaining = value.lastUpdated + CACHE_EVICTION_TIME - System.currentTimeMillis()
        if (remaining <= 0) 0L
        else TimeUnit.MILLISECONDS.toNanos(remaining)
      }
      is CachedValue.Unavailable -> 0L
    }
  }
}
