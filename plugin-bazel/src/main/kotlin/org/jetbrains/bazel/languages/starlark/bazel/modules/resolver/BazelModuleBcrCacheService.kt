package org.jetbrains.bazel.languages.starlark.bazel.modules.resolver

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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@State(name = "BazelBcrCache", storages = [Storage("bazelModuleRegistryCache.xml")])
@Service(Service.Level.PROJECT)
class BazelBcrCacheService(private val coroutineScope: CoroutineScope) : PersistentStateComponent<BazelBcrCacheService.CacheState> {
  class CacheState : BaseState() {
    @get:XMap
    var cacheState by map<String, CachedValueState>()
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

  internal val cache: AsyncCache<String, CachedValue> = Caffeine.newBuilder()
    .expireAfter(object : Expiry<String, CachedValue> {
      private fun getDuration(value: CachedValue): Duration = when (value) {
        is CachedValue.Found -> (value.lastUpdated.milliseconds + CACHE_EVICTION_TIME) - System.currentTimeMillis().milliseconds
        is CachedValue.Unavailable -> Duration.ZERO
      }

      override fun expireAfterCreate(key: String, value: CachedValue, currentTime: Long): Long = getDuration(value).toJavaDuration().toNanos()
      override fun expireAfterUpdate(key: String, value: CachedValue, currentTime: Long, currentDuration: Long): Long = getDuration(value).toJavaDuration().toNanos()
      override fun expireAfterRead(key: String, value: CachedValue, currentTime: Long, currentDuration: Long): Long = currentDuration
    })
    .executor { command -> ApplicationManager.getApplication().executeOnPooledThread(command) }
    .buildAsync()

  override fun getState(): CacheState = CacheState().also { state ->
    val newMap = cache.synchronous().asMap().mapNotNull { (key, value) ->
      when (value) {
        is CachedValue.Found -> key to CachedValueState(value.values, value.lastUpdated)
        else -> null
      }
    }.toMap()
    state.cacheState.putAll(newMap)
  }

  override fun loadState(state: CacheState) {
    cache.synchronous().invalidateAll()
    val newMap = state.cacheState.mapValues { (_, value) ->
      CachedValue.Found(value.values, value.lastUpdated)
    }
    cache.synchronous().putAll(newMap)
  }

  suspend fun getCachedData(key: String, supplier: suspend () -> List<String>?): List<String> {
    val cached = cache.get(key) { _, _ ->
      coroutineScope.future {
        supplier()?.let { CachedValue.Found(it, System.currentTimeMillis()) } ?: CachedValue.Unavailable
      }
    }.asDeferred().await()
    return (cached as? CachedValue.Found)?.values ?: emptyList()
  }

  fun invalidate(key: String) {
    cache.synchronous().invalidate(key)
  }

  fun invalidateAll() {
    cache.synchronous().invalidateAll()
  }

  companion object {
    private val CACHE_EVICTION_TIME = 1.days
  }
}
