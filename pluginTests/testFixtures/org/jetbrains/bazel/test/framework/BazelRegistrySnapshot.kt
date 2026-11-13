package org.jetbrains.bazel.test.framework

import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.util.io.NioFiles
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.sha256Hex
import com.intellij.util.system.OS
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.seconds

private val LOG = fileLogger()

/**
 * A pinned snapshot of the Bazel Central Registry, unpacked once in the test cache root.
 *
 * A test project resolves its modules from local files. So a sync sends no registry request, and it needs no lockfile.
 * The pin is the BCR snapshot of the IntelliJ aspect repository. Every module version of a test project must exist in it.
 *
 * The download tries the JetBrains cache redirector first, then GitHub. The `BAZEL_PLUGIN_TEST_BCR_URL` environment
 * variable or the `bazel.plugin.test.bcr.url` system property replaces both URLs, for example with a mirror. The archive
 * must still match the pinned checksum.
 */
object BazelRegistrySnapshot {
  private const val COMMIT = "63104ee4f2bd000d5b227cc1b5327512278ff078"
  private const val SHA256 = "ddcd494b74cc75d8dd9eaf19b810c3dc660c08b0f67fe504b4f9d4ceda51e870"
  private const val GITHUB_URL = "https://github.com/bazelbuild/bazel-central-registry/archive/$COMMIT.zip"
  private const val REDIRECTOR_URL = "https://cache-redirector.jetbrains.com/github.com/bazelbuild/bazel-central-registry/archive/$COMMIT.zip"
  private const val MOVE_ATTEMPTS = 30
  private val MOVE_RETRY_DELAY = 1.seconds

  /** Returns the registry directory in [cacheRoot]. The first call downloads and unpacks the archive. */
  @Synchronized
  fun registry(cacheRoot: Path): Path {
    val registry = cacheRoot.resolve("bcr").resolve(COMMIT)
    if (registry.resolve("bazel_registry.json").exists()) return registry

    val parent = registry.parent.createDirectories()
    // Another test process can unpack the same snapshot, so only the process that has the file lock unpacks it.
    FileChannel.open(parent.resolve("$COMMIT.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { lockFile ->
      lockFile.lock().use {
        if (!registry.resolve("bazel_registry.json").exists()) unpackSnapshot(parent, registry)
      }
    }
    return registry
  }

  private fun unpackSnapshot(parent: Path, registry: Path) {
    // The check in `registry` reads the directory with no lock. The snapshot appears with one atomic move, so no
    // process reads a partial registry.
    val staging = Files.createTempDirectory(parent, "$COMMIT.")
    try {
      val archive = staging.resolve("bcr.zip")
      downloadArchive(archive)

      val unpacked = staging.resolve("registry")
      Decompressor.Zip(archive).removePrefixPath("bazel-central-registry-$COMMIT").extract(unpacked)
      moveSnapshot(unpacked, registry)
      LOG.info("Unpacked the BCR snapshot into $registry")
    }
    finally {
      NioFiles.deleteRecursively(staging)
    }
  }

  private fun moveSnapshot(unpacked: Path, registry: Path) {
    repeat(MOVE_ATTEMPTS) { attempt ->
      try {
        Files.move(unpacked, registry, StandardCopyOption.ATOMIC_MOVE)
        return
      }
      catch (e: FileSystemException) {
        if (OS.CURRENT != OS.Windows || attempt == MOVE_ATTEMPTS - 1) throw e

        // Windows denies the move of a directory while a process has a file of that directory open. On the build
        // agents the move failed in rare cases, immediately after the unpack. The process that has the file open is not known.
        LOG.info("Retrying the move of the BCR snapshot into $registry, attempt ${attempt + 1} failed: $e")
        Thread.sleep(MOVE_RETRY_DELAY.inWholeMilliseconds)
      }
    }
  }

  private fun archiveUrls(): List<String> {
    val url = System.getenv("BAZEL_PLUGIN_TEST_BCR_URL")?.takeIf { it.isNotBlank() }
              ?: System.getProperty("bazel.plugin.test.bcr.url")?.takeIf { it.isNotBlank() }
    return if (url != null) listOf(url) else listOf(REDIRECTOR_URL, GITHUB_URL)
  }

  private fun downloadArchive(archive: Path) {
    val failures = mutableListOf<Exception>()
    for (url in archiveUrls()) {
      try {
        LOG.info("Downloading the BCR snapshot $COMMIT from $url")
        HttpRequests.request(url).saveToFile(archive, null)
        val checksum = sha256Hex(archive)
        if (checksum != SHA256) throw IOException("The BCR snapshot from $url has the checksum $checksum, expected $SHA256")
        return
      }
      catch (e: IOException) {
        LOG.warn("Cannot download the BCR snapshot from $url: ${e.message}")
        failures.add(e)
      }
    }
    throw IOException("Cannot download the BCR snapshot $COMMIT").apply { failures.forEach(::addSuppressed) }
  }
}
