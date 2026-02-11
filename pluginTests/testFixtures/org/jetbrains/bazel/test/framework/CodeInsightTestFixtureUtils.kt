package org.jetbrains.bazel.test.framework

import com.intellij.codeInsight.multiverse.CodeInsightContextManager
import com.intellij.codeInsight.multiverse.EditorContextManager
import com.intellij.codeInsight.multiverse.ModuleContext
import com.intellij.codeInsight.multiverse.SingleEditorContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import kotlin.io.path.Path

fun CodeInsightTestFixture.checkHighlighting(
  path: String,
  moduleName: String? = null, // defines module from multiverse If not null
  expected: ExpectedHighlightingData? = null, // If null, highlightings are inlined into file text
) {
  val psiFile = configureFromTempProjectFile(path)

  if (moduleName != null) {
    val allContexts = CodeInsightContextManager.getInstance(project).getCodeInsightContexts(psiFile.virtualFile)
    val context = allContexts.find { it is ModuleContext && it.getModule()?.name == moduleName }
                  ?: error("Module $moduleName not found in contexts: $allContexts")

    EditorContextManager.getInstance(project).setEditorContext(editor, SingleEditorContext(context))
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
  }

  if (expected == null) {
    checkHighlighting()
  } else {
    (this as CodeInsightTestFixtureImpl).collectAndCheckHighlighting(expected)
  }
}

fun CodeInsightTestFixture.virtualFileOf(path: String): VirtualFile {
  val manager = project.workspaceModel.getVirtualFileUrlManager()
  return Path(path)
    .toVirtualFileUrl(manager)
    .virtualFile
    .let { requireNotNull(it) { "Virtual file not found for path: $path" } }
}
