package org.jetbrains.bazel.test.framework

import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.NioFiles
import com.intellij.testFramework.junit5.fixture.testFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.io.path.createDirectory

private const val DELETE_ATTEMPTS = 5
private const val DELETE_RETRY_DELAY_MS = 100L

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
    catch (ignored: FileAlreadyExistsException) {
      // another fixture has this name
    }
  }
}

private fun restoreWritePermissions(outputBase: Path) {
  Files.walkFileTree(outputBase, object : SimpleFileVisitor<Path>() {
    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
      if (attrs.isOther) return FileVisitResult.SKIP_SUBTREE
      NioFiles.setReadOnly(dir, false)
      return FileVisitResult.CONTINUE
    }

    // a dangling junction on Windows, such as `external/_main`
    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
      if (exc is NoSuchFileException) return FileVisitResult.CONTINUE
      throw exc
    }
  })
}

private suspend fun deleteOutputBase(outputBase: Path) {
  repeat(DELETE_ATTEMPTS) { attempt ->
    try {
      NioFiles.deleteRecursively(outputBase)
      return
    }
    catch (e: FileSystemException) {
      if (!SystemInfo.isWindows) throw e
      if (attempt == DELETE_ATTEMPTS - 1) {
        LOG.warn("Keeping the rest of $outputBase, because the test JVM still holds ${e.file}", e)
        return
      }
      LOG.info("Retrying the deletion of $outputBase, because ${e.file} is still open")
      // The JVM closes the `lib/jrt-fs.jar` class loader and unmaps `lib/modules` of a closed JRT file system on a collection.
      System.gc()
      delay(DELETE_RETRY_DELAY_MS)
    }
  }
}
