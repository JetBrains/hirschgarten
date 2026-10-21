package org.jetbrains.bazel.test.framework

import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.util.io.NioFiles
import com.intellij.util.io.Decompressor
import com.intellij.util.io.sha256Hex
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

private val LOG = fileLogger()

/**
 * A pinned snapshot of the Bazel Central Registry, unpacked once in the test cache root.
 *
 * A test project resolves its modules from local files. So a sync sends no registry request, and it needs no lockfile.
 * The pin is the BCR snapshot of the IntelliJ aspect repository. Every module version of a test project must exist in it.
 *
 * The `BAZEL_PLUGIN_TEST_BCR_URL` environment variable or the `bazel.plugin.test.bcr.url` system property replaces the
 * download URL, for example with a mirror. The archive must still match the pinned checksum.
 */
object BazelRegistrySnapshot {
  private const val COMMIT = "63104ee4f2bd000d5b227cc1b5327512278ff078"
  private const val SHA256 = "ddcd494b74cc75d8dd9eaf19b810c3dc660c08b0f67fe504b4f9d4ceda51e870"
  private const val DEFAULT_URL = "https://github.com/bazelbuild/bazel-central-registry/archive/$COMMIT.zip"

  /** Returns the registry directory in [cacheRoot]. The first call downloads and unpacks the archive. */
  @Synchronized
  fun registry(cacheRoot: Path): Path {
    val registry = cacheRoot.resolve("bcr").resolve(COMMIT)
    if (registry.resolve("bazel_registry.json").exists()) return registry

    val parent = registry.parent.createDirectories()
    // Another test process can unpack the same snapshot. The snapshot appears with one atomic move, so no process
    // reads a partial registry.
    val staging = Files.createTempDirectory(parent, "$COMMIT.")
    try {
      val archive = staging.resolve("bcr.zip")
      val url = archiveUrl()
      LOG.info("Downloading the BCR snapshot $COMMIT from $url")
      download(url, archive)
      val checksum = sha256Hex(archive)
      check(checksum == SHA256) { "The BCR snapshot from $url has the checksum $checksum, expected $SHA256" }

      val unpacked = staging.resolve("registry")
      Decompressor.Zip(archive).removePrefixPath("bazel-central-registry-$COMMIT").extract(unpacked)
      try {
        Files.move(unpacked, registry, StandardCopyOption.ATOMIC_MOVE)
      }
      catch (e: FileSystemException) {
        // another process moved its snapshot first
        if (!registry.resolve("bazel_registry.json").exists()) throw e
      }
      LOG.info("Unpacked the BCR snapshot into $registry")
    }
    finally {
      NioFiles.deleteRecursively(staging)
    }
    return registry
  }

  private fun archiveUrl(): String =
    System.getenv("BAZEL_PLUGIN_TEST_BCR_URL")?.takeIf { it.isNotBlank() }
    ?: System.getProperty("bazel.plugin.test.bcr.url")?.takeIf { it.isNotBlank() }
    ?: DEFAULT_URL

  private fun download(url: String, target: Path) {
    val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
    val response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofFile(target))
    if (response.statusCode() != 200) {
      throw IOException("Cannot download the BCR snapshot from $url: HTTP ${response.statusCode()}")
    }
  }
}
