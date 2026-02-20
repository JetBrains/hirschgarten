package org.jetbrains.bazel.languages.starlark.bazel.bzlmod.resolver

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.util.xmlb.annotations.Tag
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.createParentDirectories
import org.h2.mvstore.MVStore

@Service(Service.Level.PROJECT)
class BazelModuleBcrCacheService(project: Project) : Disposable {
  private val gson = Gson()
  private val store: MVStore = openStore(project.getProjectDataPath("bzlmod/bcr-cache.mv"))
  private val map = store.openMap<String, String>("bcrCache")

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

  /**
   * Returns cached values if present.
   * Otherwise, schedules a single asynchronous load per key and awaits it (no thread blocking).
   */
  suspend fun getOrFetch(key: String, supplier: suspend () -> List<String>?): List<String> {
    getCachedValuesIfPresent(key)?.let { return it }

    // To ensure only one thread is loading the value for a given key at a time, we use a mutex per key.
    // Then other threads will wait for the value to be loaded and get it from the cache.
    return keyLocks.computeIfAbsent(key) { Mutex() }.withLock {
      getCachedValuesIfPresent(key)?.let { return@withLock it }
      val loaded = supplier() ?: return@withLock emptyList()
      val value = CachedValue(loaded, System.currentTimeMillis())
      cache.put(key, value)
      map[key] = gson.toJson(value)
      store.commit()
      loaded
    }
  }

  fun invalidate(key: String) {
    cache.invalidate(key)
    keyLocks.remove(key)

    map.remove(key)
    store.commit()
  }

  override fun dispose() {
    runCatching {
      store.close()
    }.onFailure {
      Logger
        .getInstance(BazelModuleBcrCacheService::class.java)
        .warn("Failed to close MVStore for Bazel BCR cache", it)
    }
  }

  private fun getCachedValuesIfPresent(key: String): List<String>? {
    cache.getIfPresent(key)?.let { return it.values }

    val raw = map[key] ?: return null
    val parsed = runCatching {
      gson.fromJson(raw, CachedValue::class.java)
    }.getOrNull() ?: return null
    if (remainingMillis(parsed.lastUpdated) <= 0) {
      map.remove(key)
      store.commit()
      return null
    }

    cache.put(key, parsed)
    return parsed.values
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
      ): Long = remainingNanos(value.lastUpdated)

      override fun expireAfterUpdate(
        key: String,
        value: CachedValue,
        currentTime: Long,
        currentDuration: Long,
      ): Long = remainingNanos(value.lastUpdated)

      override fun expireAfterRead(
        key: String,
        value: CachedValue,
        currentTime: Long,
        currentDuration: Long,
      ): Long = currentDuration
    }

    private fun remainingMillis(lastUpdatedMillis: Long): Long =
      lastUpdatedMillis + CACHE_EVICTION_TIME - System.currentTimeMillis()

    private fun remainingNanos(lastUpdatedMillis: Long): Long {
      val remaining = remainingMillis(lastUpdatedMillis)
      return if (remaining <= 0) 0L else TimeUnit.MILLISECONDS.toNanos(remaining)
    }

    /**
     * Opens an MVStore instance for the given file path.
     * Creates the parent directories if needed.
     */
    private fun openStore(path: Path): MVStore {
      path.createParentDirectories()
      return MVStore.Builder()
        .fileName(path.toAbsolutePath().toString())
        .autoCommitDisabled()
        .cacheSize(128)
        .open()
        .also { it.setVersionsToKeep(0) }
    }
  }
}
