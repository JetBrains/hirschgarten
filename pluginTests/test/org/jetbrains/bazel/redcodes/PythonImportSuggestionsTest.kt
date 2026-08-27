package org.jetbrains.bazel.redcodes

import com.intellij.bazel.python.backend.updateBazelPythonResolveIndex
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.psi.util.QualifiedName
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.python.inspections.unresolvedReference.PyUnresolvedReferencesInspection
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightTestFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.NonIndexableVirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.modifyBazelProjectDirectoriesEntity
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * End-to-end checks for the resolve-index-driven import suggestions on the `redcodes/python_imports` project.
 *
 * `tools/helper` is a `py_library` without an `imports` attribute, so PyCharm cannot name `tools/helper/op.py` from
 * source roots and its auto-import cannot insert a working import for `op_func`.
 */
class PythonImportSuggestionsTest {

  @Nested
  @BazelTestApplication
  inner class NoImportsTarget {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/python_imports",
      buildProject = true,
      configure = { it.replaceMainFile("op_fun<caret>c()\n") },
    )

    @Test
    fun testImportQuickFixForSymbolInNoImportsTarget(): Unit = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.enableInspections(PyUnresolvedReferencesInspection())
        fixture.configureFromTempProjectFile("main/main.py")
        fixture.doHighlighting()

        fixture.launchSingleImportIntention("tools.helper.op")
        fixture.file.text shouldContain "from tools.helper.op import op_func"
      }
    }
  }

  @Nested
  @BazelTestApplication
  inner class ExternalBazelIndexedSource {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/python_imports",
      buildProject = true,
      configure = { it.replaceMainFile("BaseMod<caret>el()\n") },
    )

    @Test
    fun testImportQuickFixForSymbolInExternalBazelIndexedSource(): Unit = runBlocking(Dispatchers.Default) {
      val pydanticMain = Path(fixture.tempDirPath)
        .resolveSibling("external-pydantic")
        .resolve("site-packages")
        .resolve("pydantic")
        .resolve("main.py")
      pydanticMain.parent.createDirectories()
      pydanticMain.writeText("class BaseModel:\n    pass\n")
      val pydanticMainFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(pydanticMain)!!
      fixture.project.updateBazelPythonResolveIndex(
        mapOf(QualifiedName.fromDottedString("pydantic.main") to pydanticMain),
      )
      fixture.registerAdditionalIndexedFile(pydanticMainFile)
      FileBasedIndex.getInstance().requestReindex(pydanticMainFile)
      IndexingTestUtil.waitUntilIndexesAreReady(fixture.project)

      withContext(Dispatchers.EDT) {
        fixture.enableInspections(PyUnresolvedReferencesInspection())
        fixture.configureFromTempProjectFile("main/main.py")
        fixture.doHighlighting()

        fixture.launchSingleImportIntention("pydantic.main")
        fixture.file.text shouldContain "from pydantic.main import BaseModel"
      }
    }
  }
}

private fun BazelSyncCodeInsightTestFixture.launchSingleImportIntention(moduleQName: String) {
  val intentions = availableIntentions.filter { it.text.contains(moduleQName) }
  intentions shouldHaveSize 1
  launchAction(intentions.single())
}

private fun BazelSyncCodeInsightTestFixture.replaceMainFile(text: String) {
  Path(tempDirPath).resolve("main").resolve("main.py").writeText(text)
}

private fun BazelSyncCodeInsightTestFixture.registerAdditionalIndexedFile(file: VirtualFile) {
  val urlManager = project.workspaceModel.getVirtualFileUrlManager()
  runWriteAction {
    project.workspaceModel.updateProjectModel("register external Python source") { storage ->
      val entity = storage.entities(BazelProjectDirectoriesEntity::class.java).first()
      storage.modifyBazelProjectDirectoriesEntity(entity) {
        indexAdditionalFiles += NonIndexableVirtualFileUrl(file.toVirtualFileUrl(urlManager))
      }
    }
  }
}
