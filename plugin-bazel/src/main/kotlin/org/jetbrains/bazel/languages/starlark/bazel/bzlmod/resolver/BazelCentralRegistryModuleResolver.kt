package org.jetbrains.bazel.languages.starlark.bazel.bzlmod.resolver

import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.bazel.bzlmod.BazelModuleResolver
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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
  private val httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()

  private data class GitHubContent(val name: String, val type: String)

  private data class GitHubFileContent(val content: String?, val encoding: String?)

  private data class ModuleMetadata(val versions: List<String>?)

  private suspend fun fetchModuleNamesFromRegistry(): List<String>? =
    try {
      val json = performHttpRequest(githubApiUrl)
      gson
        .fromJson(json, Array<GitHubContent>::class.java)
        // Note: the GitHub endpoint can return various item types (dir, file, symlink, submodule).
        // Modules are directories, so we explicitly filter only type == "dir".
        .filter { it.type == "dir" }
        .map { it.name }
        .sorted()
    } catch (e: IOException) {
      log.warn(StarlarkBundle.message("bzlmod.warning.fetching.module.names", e))
      null
    }

  private suspend fun fetchModuleVersionsFromRegistry(moduleName: String): List<String>? =
    try {
      val url = "$githubApiUrl/$moduleName/metadata.json"
      val json = performHttpRequest(url)
      val file = gson.fromJson(json, GitHubFileContent::class.java)
      val decoded =
        if (file.content != null && file.encoding == "base64") {
          String(Base64.getMimeDecoder().decode(file.content))
        } else {
          log.warn(StarlarkBundle.message("bzlmod.warning.encoding", moduleName))
          return null
        }
      val metadata = gson.fromJson(decoded, ModuleMetadata::class.java)
      metadata.versions?.toList()?.reversed()
    } catch (e: IOException) {
      log.warn(StarlarkBundle.message("bzlmod.warning.fetching.module.versions", moduleName, e))
      null
    }

  private suspend fun performHttpRequest(url: String): String =
    withContext(Dispatchers.IO) {
      val request = HttpRequest.newBuilder(URI.create(url))
        .header("accept", "application/vnd.github.v3+json")
        .header("User-Agent", ApplicationNamesInfo.getInstance().fullProductName)
        .build()

      val response: HttpResponse<String> =
        try {
          httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        }
        catch (e: Exception) {
          throw IOException(StarlarkBundle.message("bzlmod.exception.http.request", url, e))
        }

      if (response.statusCode() !in 200..299) {
        throw IOException(StarlarkBundle.message("bzlmod.exception.http.status", response.statusCode(), url))
      }

      response.body()
    }

  companion object {
    const val ID: String = "bcr"
    private const val MODULE_NAMES_KEY = "___MODULE_NAMES___"
  }
}
