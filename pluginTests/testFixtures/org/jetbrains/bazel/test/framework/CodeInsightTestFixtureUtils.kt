package org.jetbrains.bazel.test.framework

import com.intellij.codeInsight.multiverse.CodeInsightContextManager
import com.intellij.codeInsight.multiverse.EditorContextManager
import com.intellij.codeInsight.multiverse.ModuleContext
import com.intellij.codeInsight.multiverse.SingleEditorContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.Path

suspend fun CodeInsightTestFixture.checkHighlighting(
  path: String,
  moduleName: String? = null, // defines module from multiverse If not null
  expected: ExpectedHighlightingData? = null, // If null, highlightings are inlined into file text
  checkIndexable: Boolean = true,
) {
  val psiFile = configureFromTempProjectFile(path)

  if (moduleName != null) {
    val allContexts = withContext(Dispatchers.Default) {
      readAction {
        CodeInsightContextManager.getInstance(project).getCodeInsightContexts(psiFile.virtualFile)
      }
    }
    val context = allContexts.find { it is ModuleContext && it.getModule()?.name == moduleName }
                  ?: error("Module $moduleName not found in contexts: $allContexts")

    writeAction { EditorContextManager.getInstance(project).setEditorContext(editor, SingleEditorContext(context)) }
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
  }

  if (checkIndexable) {
    withClue("${psiFile.virtualFile} is not indexable. Has the project import succeeded?") {
      val workspaceFileIndex = WorkspaceFileIndex.getInstance(project)
      readAction {
        workspaceFileIndex.isIndexable(psiFile.virtualFile) shouldBe true
      }
    }
  }

  withContext(Dispatchers.EDT) {
    if (expected == null) {
      checkHighlighting()
    }
    else {
      (this@checkHighlighting as CodeInsightTestFixtureImpl).collectAndCheckHighlighting(expected)
    }
  }
}

fun CodeInsightTestFixture.virtualFileOf(path: String): VirtualFile {
  val manager = project.workspaceModel.getVirtualFileUrlManager()
  return Path(path)
    .toVirtualFileUrl(manager)
    .virtualFile
    .let { requireNotNull(it) { "Virtual file not found for path: $path" } }
}
