package org.jetbrains.bazel.test.framework

import com.intellij.openapi.util.io.NioFiles
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/** Creates a temporary output base and restores directory write permissions before its cleanup. */
internal fun bazelOutputBaseFixture() = testFixture {
  val outputBase = tempPathFixture(prefix = "bazel-output").init()
  initialized(outputBase) {
    withContext(Dispatchers.IO) {
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
  }
}
