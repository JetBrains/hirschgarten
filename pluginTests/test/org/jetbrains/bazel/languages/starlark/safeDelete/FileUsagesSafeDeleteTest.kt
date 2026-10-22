package org.jetbrains.bazel.languages.starlark.safeDelete

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import com.intellij.testFramework.runInEdtAndWait
import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.projectDirectories
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * This test ensures that Bazel files are not blocking the safe delete of regular source files.
 * On the Bazel plugin side, it should be enough to respect the effective search scope in [org.jetbrains.bazel.languages.starlark.findusages.StarlarkFileUsageSearcher],
 * because safe delete does not include [com.intellij.psi.search.UseScopeEnlarger]s,
 * so Starlark files should never be a part of the effective search scope.
 *
 * To properly recreate the "real life" scenario, build files must not be under content roots, and must be registered using [BazelProjectDirectoriesEntity].
 */
@BazelTestApplication
internal class FileUsagesSafeDeleteTest {

  private val codeInsightFixture by bazelCodeInsightFixture()
  private val testBazel get() = codeInsightFixture.testBazel

  @BeforeEach
  fun setUp() {
    replaceDefaultContentRoot(testBazel.workspaceRoot.resolve("src").createDirectories())
  }

  @Test
  fun `safe delete should not prevent deleting source file when referred in glob`() = runTest {
    val sourceFile = codeInsightFixture.addFileToProject("src/com/example/Foo.kt", "package com.example\nclass Foo")
    addBuildFile("""
      kt_jvm_library(
          name = "lib",
          srcs = glob(["src/**/*.kt"]),
      )
    """.trimIndent())
    runInEdtAndWait {
      SafeDeleteHandler.invoke(testBazel.project, arrayOf(sourceFile), true)
    }
  }

  @Test
  fun `safe delete should not prevent deleting source file when referred explicitly`() = runTest {
    val sourceFile = codeInsightFixture.addFileToProject("src/com/example/Foo.kt", "package com.example\nclass Foo")
    addBuildFile("""
      kt_jvm_library(
          name = "lib",
          srcs = ["src/com/example/Foo.kt"],
      )
    """.trimIndent())
    runInEdtAndWait {
      SafeDeleteHandler.invoke(testBazel.project, arrayOf(sourceFile), true)
    }
  }

  private fun addBuildFile(content: String) {
    codeInsightFixture.addFileToProject("BUILD.bazel", content)
    testBazel.projectDirectories { include(".") }
  }

  private fun replaceDefaultContentRoot(path: Path) {
    ModuleRootModificationUtil.updateModel(codeInsightFixture.module) { model ->
      model.contentEntries.forEach { model.removeContentEntry(it) }
      model.addContentEntry(VfsUtil.pathToUrl(testBazel.workspaceRoot.resolve(path).toString()))
    }
  }
}
