package org.jetbrains.bazel.languages.starlark.bazel.bzlmod.resolver

import com.google.gson.Gson
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.bazel.bzlmod.BazelModuleResolver
import java.io.IOException
import java.util.Base64

class BazelCentralRegistryModuleResolver : BazelModuleResolver {
  override val id: String = ID
  override val name: String = "Bazel Central Registry"

  override suspend fun getModuleNames(project: Project): List<String> {
    val bcrCache = project.serviceAsync<BazelModuleBcrCacheService>()
    return bcrCache.getOrFetch(MODULE_NAMES_KEY) {
      fetchModuleNamesFromRegistry()
    }
  }

  override suspend fun getModuleVersions(project: Project, moduleName: String): List<String> {
    val bcrCache = project.serviceAsync<BazelModuleBcrCacheService>()
    return bcrCache.getOrFetch(moduleName) {
      fetchModuleVersionsFromRegistry(moduleName)
    }
  }

  override suspend fun refreshModuleNames(project: Project) {
    val cache = project.service<BazelModuleBcrCacheService>()
    cache.invalidate(MODULE_NAMES_KEY)
    getModuleNames(project)
  }

  private val gson = Gson()
  private val githubApiUrl = "https://api.github.com/repos/bazelbuild/bazel-central-registry/contents/modules"
  private val log = Logger.getInstance(BazelCentralRegistryModuleResolver::class.java)

  private data class GitHubContent(val name: String, val type: String)

  private data class GitHubFileContent(val content: String?, val encoding: String?)

  private data class ModuleMetadata(val versions: List<String>?)

  private suspend fun fetchModuleNamesFromRegistry(): List<String>? =
    try {
      withContext(Dispatchers.IO) {
        val json = performHttpRequest(githubApiUrl)
        gson
          .fromJson(json, Array<GitHubContent>::class.java)
          // Note: the GitHub endpoint can return various item types (dir, file, symlink, submodule).
          // Modules are directories, so we explicitly filter only type == "dir".
          .filter { it.type == "dir" }
          .map { it.name }
          .sorted()
      }
    } catch (_: IOException) {
      null
    }

  private suspend fun fetchModuleVersionsFromRegistry(moduleName: String): List<String>? =
    try {
      withContext(Dispatchers.IO) {
        val url = "$githubApiUrl/$moduleName/metadata.json"
        val json = performHttpRequest(url)
        val file = gson.fromJson(json, GitHubFileContent::class.java)
        val decoded =
          if (file.content != null && file.encoding == "base64") {
            String(Base64.getMimeDecoder().decode(file.content))
          } else {
            log.warn(StarlarkBundle.message("bzlmod.warning.encoding", moduleName))
            return@withContext null
          }
        val metadata = gson.fromJson(decoded, ModuleMetadata::class.java)
        metadata.versions?.toList()?.reversed()
      }
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
