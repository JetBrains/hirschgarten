package org.jetbrains.bazel.test.framework

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import kotlin.io.path.Path

fun CodeInsightTestFixture.checkHighlighting(
  path: String,
) {
  openFileInEditor(virtualFileOf("$tempDirPath/$path"))
  checkHighlighting()
}

fun CodeInsightTestFixture.virtualFileOf(path: String): VirtualFile {
  val manager = project.workspaceModel.getVirtualFileUrlManager()
  return Path(path)
    .toVirtualFileUrl(manager)
    .virtualFile
    .let { requireNotNull(it) { "Virtual file not found for path: $path" } }
}
