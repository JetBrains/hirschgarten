package org.jetbrains.bazel.test.framework

import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.util.io.NioFiles
import com.intellij.testFramework.junit5.fixture.testFixture
import com.intellij.util.system.OS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.utils.isWindowsJunction
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.io.path.createDirectory
import kotlin.time.Duration.Companion.milliseconds

private const val DELETE_ATTEMPTS = 5
private val DELETE_RETRY_DELAY = 100L.milliseconds

private val LOG = fileLogger()

/**
 * Creates a temporary output base with a short name, and deletes it after the test.
 *
 * The cleanup restores the directory write permissions first. On Windows, the test JVM can hold the files of a JDK in
 * the output base until a garbage collection. The cleanup then runs the collector between its attempts, and it keeps
 * the files that stay open.
 */
internal fun bazelOutputBaseFixture() = testFixture {
  val outputBase = withContext(Dispatchers.IO) { createOutputBase() }
  initialized(outputBase) {
    withContext(Dispatchers.IO) {
      restoreWritePermissions(outputBase)
      deleteOutputBase(outputBase)
    }
  }
}

private fun createOutputBase(): Path {
  val tempRoot = Path.of(System.getProperty("java.io.tmpdir"))
  while (true) {
    try {
      return tempRoot.resolve("ob-" + UUID.randomUUID().toString().take(8)).createDirectory().toRealPath()
    }
    catch (_: FileAlreadyExistsException) {
      // another fixture has this name
    }
  }
}

/**
 * Makes [dir] and its subdirectories writable. It does not enter a symlink or a junction, such as `external/_main`.
 */
private fun restoreWritePermissions(dir: Path) {
  val children = try {
    NioFiles.setReadOnly(dir, false)
    Files.newDirectoryStream(dir).use { it.toList() }
  }
  catch (_: NoSuchFileException) {
    return
  }
  for (child in children) {
    val attrs = try {
      Files.readAttributes(child, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
    }
    catch (_: NoSuchFileException) {
      continue
    }
    if (attrs.isDirectory && !attrs.isWindowsJunction) {
      restoreWritePermissions(child)
    }
  }
}

private suspend fun deleteOutputBase(outputBase: Path) {
  repeat(DELETE_ATTEMPTS) { attempt ->
    try {
      NioFiles.deleteRecursively(outputBase)
      return
    }
    catch (e: FileSystemException) {
      if (OS.CURRENT != OS.Windows)
        throw e

      if (attempt == DELETE_ATTEMPTS - 1) {
        LOG.warn("Keeping the rest of $outputBase, because the test JVM still holds ${e.file}", e)
        return
      }
      LOG.info("Retrying the deletion of $outputBase, because ${e.file} is still open")
      // The JVM closes the `lib/jrt-fs.jar` class loader and unmaps `lib/modules` of a closed JRT file system on a collection.
      System.gc()
      delay(DELETE_RETRY_DELAY)
    }
  }
}
