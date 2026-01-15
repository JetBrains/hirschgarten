package org.jetbrains.bazel.test.framework

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import kotlin.io.path.Path

fun CodeInsightTestFixture.doHighlighting(
  path: String,
  minimalSeverity: HighlightSeverity = HighlightSeverity.INFORMATION,
): List<HighlightInfo> {
  openFileInEditor(virtualFileOf("$tempDirPath/$path"))
  return doHighlighting(minimalSeverity)
}

fun CodeInsightTestFixture.virtualFileOf(path: String): VirtualFile {
  val manager = project.workspaceModel.getVirtualFileUrlManager()
  return Path(path)
    .toVirtualFileUrl(manager)
    .virtualFile
    .let(::checkNotNull)
}
