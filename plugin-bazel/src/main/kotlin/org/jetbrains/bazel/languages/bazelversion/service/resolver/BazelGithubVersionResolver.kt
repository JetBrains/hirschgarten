package org.jetbrains.bazel.languages.bazelversion.service.resolver

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.text.SemVer
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionResolver

class BazelGithubVersionResolver : BazelVersionResolver {
  val githubGson = GsonBuilder()
    .setPrettyPrinting()
    .serializeNulls()
    .disableHtmlEscaping()
    .create()
  val httpClient = HttpClient(CIO)

  override val id: String = ID
  override val name: String = "Github"

  override suspend fun resolveLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?): String? {
    val version = project.service<BazelGithubGlobalCacheService>()
      .getCachedVersion(currentVersion?.fork ?: DEFAULT_FORK_NAME, System.currentTimeMillis()) { name, _ ->
        getLatestBazelVersion(project, name)
      }
    return version
  }

  suspend fun getLatestBazelVersion(project: Project, forkName: String): String? {
    val response = httpClient.get("https://api.github.com/repos/$forkName/$DEFAULT_REPO_NAME/releases")
    val cacheService = project.service<BazelGithubGlobalCacheService>()

    if (response.status != HttpStatusCode.OK) {
      return null
    }
    val releases = try {
      githubGson.fromJson(response.bodyAsText(), Array<GithubReleaseResponse>::class.java)
    } catch (_: JsonSyntaxException) {
      return null
    }
    val comparator = compareBy<GithubReleaseResponse> { response -> response.tagName?.let { SemVer.parseFromText(it) } }
    return releases.toList()
      .filter {
        if (!cacheService.state.includePrelease && it.prerelease == true) {
          return@filter false
        }
        if (!cacheService.state.includeReleaseCandidate && it.tagName?.contains("-rc") == true) {
          return@filter false
        }
        true
      }.maxWithOrNull(comparator)?.tagName
  }

  companion object {
    const val ID = "github"
    const val DEFAULT_FORK_NAME = "bazelbuild"
    const val DEFAULT_REPO_NAME = "bazel"
  }

  data class GithubReleaseResponse(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("tag_name") var tagName: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("prerelease") var prerelease: Boolean? = null,
  )
}

const val GITHUB_CACHE_EVICTION_TIME = 24 * 60 * 60 * 1000 // 24 hours

@Service(Service.Level.PROJECT)
@State(name = "BazelGithubResolverVersionCache", storages = [Storage("bazelVersionCache.xml")])
class BazelGithubGlobalCacheService :
  SimplePersistentStateComponent<BazelGithubGlobalCacheService.State>(State()) {

  val lock = Mutex()

  class State : BaseState() {
    @get:XMap
    val forks by map<String, ForkState>()
    var includePrelease by property(false)
    var includeReleaseCandidate by property(false)
  }

  @Tag("fork_state")
  data class ForkState(
    @JvmField var name: String = "",
    @JvmField var version: String = "",
    @JvmField var lastUpdated: Long = 0,
  )

  suspend fun getCachedVersion(
    forkName: String, currentTime: Long,
    factory: suspend (forkName: String, lastUpdated: Long) -> String?,
  ): String? =
    lock.withLock {
      val factoryAdapter: suspend (String, Long) -> ForkState? =
        { name, lastUpdated ->
          factory(name, lastUpdated)?.let { ForkState(name, it, lastUpdated) }
        }
      val fork = if (state.forks.containsKey(forkName)) {
        state.forks[forkName]!!
      } else {
        val newFork = factoryAdapter(forkName, currentTime) ?: return null
        state.forks[forkName] = newFork
        newFork
      }
      if (currentTime - fork.lastUpdated > GITHUB_CACHE_EVICTION_TIME) {
        val newFork = factoryAdapter(forkName, currentTime) ?: return fork.version
        state.forks[forkName] = newFork
        return newFork.version
      }
      return fork.version
    }
}
