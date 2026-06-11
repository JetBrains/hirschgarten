package org.jetbrains.bazel.languages.starlark.safeDelete

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntityFixtures.emptyBazelDirectoryWorkspaceEntity
import org.jetbrains.bazel.workspacemodel.entities.NonIndexableVirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.modifyBazelProjectDirectoriesEntity
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
@TestApplication
internal class FileUsagesSafeDeleteTest {

  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture
  private val projectFixture = projectFixture(pathFixture = tempDirFixture, openAfterCreation = true)
  private val project by projectFixture
  private val moduleFixture = projectFixture.moduleFixture()
  private val codeInsightFixture by codeInsightFixture(projectFixture, tempDirFixture)

  @BeforeEach
  fun setUp() {
    initializeBazelProject(project, tempDir)
    replaceDefaultContentRoot(tempDir.resolve("src").createDirectories())
    addBazelDirectoriesEntity()
  }

  @Test
  fun `safe delete should not prevent deleting source file when referred in glob`() {
    val sourceFile = codeInsightFixture.addFileToProject("src/com/example/Foo.kt", "package com.example\nclass Foo")
    addBuildFile("""
      kt_jvm_library(
          name = "lib",
          srcs = glob(["src/**/*.kt"]),
      )
    """.trimIndent())
    runInEdtAndWait {
      SafeDeleteHandler.invoke(project, arrayOf(sourceFile), true)
    }
  }

  @Test
  fun `safe delete should not prevent deleting source file when referred explicitly`() {
    val sourceFile = codeInsightFixture.addFileToProject("src/com/example/Foo.kt", "package com.example\nclass Foo")
    addBuildFile("""
      kt_jvm_library(
          name = "lib",
          srcs = ["src/com/example/Foo.kt"],
      )
    """.trimIndent())
    runInEdtAndWait {
      SafeDeleteHandler.invoke(project, arrayOf(sourceFile), true)
    }
  }

  private fun addBuildFile(content: String) {
    val buildFile = codeInsightFixture.addFileToProject("BUILD.bazel", content)
    registerBuildFile(buildFile.virtualFile)
  }

  private fun registerBuildFile(buildFileVf: VirtualFile) {
    val urlManager = project.workspaceModel.getVirtualFileUrlManager()
    runWriteAction {
      project.workspaceModel.updateProjectModel("register BUILD file") { storage ->
        val entity = storage.entities(BazelProjectDirectoriesEntity::class.java).first()
        storage.modifyBazelProjectDirectoriesEntity(entity) {
          indexAdditionalFiles = mutableListOf(NonIndexableVirtualFileUrl(buildFileVf.toVirtualFileUrl(urlManager)))
        }
      }
    }
    IndexingTestUtil.waitUntilIndexesAreReady(project)
  }

  private fun replaceDefaultContentRoot(path: Path) {
    val module = moduleFixture.get()
    ModuleRootModificationUtil.updateModel(module) { model ->
      model.contentEntries.forEach { model.removeContentEntry(it) }
      model.addContentEntry(VfsUtil.pathToUrl(tempDir.resolve(path).toString()))
    }
  }

  private fun addBazelDirectoriesEntity() {
    runWriteAction {
      project.workspaceModel.updateProjectModel("add BazelProjectDirectoriesEntity") {
        it.addEntity(emptyBazelDirectoryWorkspaceEntity(project))
      }
    }
  }
}
