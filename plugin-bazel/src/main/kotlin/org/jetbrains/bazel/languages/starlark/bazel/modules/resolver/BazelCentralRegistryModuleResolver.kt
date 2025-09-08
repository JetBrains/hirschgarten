package org.jetbrains.bazel.languages.starlark.bazel.modules.resolver

import com.google.gson.Gson
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import org.jetbrains.bazel.languages.starlark.bazel.modules.BazelModuleResolver
import java.io.IOException
import java.util.Base64

class BazelCentralRegistryModuleResolver : BazelModuleResolver {
  override val id: String = ID
  override val name: String = "Bazel Central Registry"

  override suspend fun getModuleNames(project: Project): List<String> {
    val cache = project.service<BazelModuleBcrCacheService>()
    return cache.getCachedData(MODULE_NAMES_KEY) {
      fetchModuleNamesFromRegistry()
    }
  }

  override suspend fun getModuleVersions(project: Project, moduleName: String): List<String> {
    val cache = project.service<BazelModuleBcrCacheService>()
    return cache.getCachedData(moduleName) {
      fetchModuleVersionsFromRegistry(moduleName)
    }
  }

  override suspend fun refreshModuleNames(project: Project) {
    val cache = project.service<BazelModuleBcrCacheService>()
    cache.invalidate(MODULE_NAMES_KEY)
    getModuleNames(project)
  }

  override fun clearCache(project: Project) {
    val cache = project.service<BazelModuleBcrCacheService>()
    cache.invalidateAll()
  }

  override fun getCachedModuleNames(project: Project): List<String> {
    val cache = project.service<BazelModuleBcrCacheService>()
    val cached = cache.cache.synchronous().getIfPresent(MODULE_NAMES_KEY)
    return (cached as? BazelModuleBcrCacheService.CachedValue.Found)?.values ?: emptyList()
  }

  override fun getCachedModuleVersions(project: Project, moduleName: String): List<String> {
    val cache = project.service<BazelModuleBcrCacheService>()
    val cached = cache.cache.synchronous().getIfPresent(moduleName)
    return (cached as? BazelModuleBcrCacheService.CachedValue.Found)?.values ?: emptyList()
  }

  private val gson = Gson()
  private val githubApiUrl = "https://api.github.com/repos/bazelbuild/bazel-central-registry/contents/modules"

  private data class GitHubContent(val name: String, val type: String)
  private data class GitHubFileContent(val content: String?, val encoding: String?)
  private data class ModuleMetadata(val versions: List<String>?)

  private fun fetchModuleNamesFromRegistry(): List<String>? =
    try {
      val json = performHttpRequest(githubApiUrl)
      gson
        .fromJson(json, Array<GitHubContent>::class.java)
        .filter { it.type == "dir" }
        .map { it.name }
        .sorted()
    } catch (_: IOException) {
      null
    }

  private fun fetchModuleVersionsFromRegistry(moduleName: String): List<String>? =
    try {
      val url = "$githubApiUrl/$moduleName/metadata.json"
      val json = performHttpRequest(url)
      val file = gson.fromJson(json, GitHubFileContent::class.java)
      val decoded = if (file.content != null && file.encoding == "base64") {
        String(Base64.getMimeDecoder().decode(file.content))
      } else {
        return null
      }
      val metadata = gson.fromJson(decoded, ModuleMetadata::class.java)
      metadata.versions?.toList()?.reversed()
    } catch (_: IOException) {
      null
    }

  private fun performHttpRequest(url: String): String =
    HttpRequests
      .request(url)
      .accept("application/vnd.github.v3+json")
      .productNameAsUserAgent()
      .readString()

  companion object {
    const val ID: String = "bcr"
    private const val MODULE_NAMES_KEY = "___MODULE_NAMES___"
  }
}
