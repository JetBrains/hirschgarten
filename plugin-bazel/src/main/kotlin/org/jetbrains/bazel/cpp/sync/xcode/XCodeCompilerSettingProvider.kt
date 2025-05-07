package org.jetbrains.bazel.cpp.sync.xcode

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

interface XCodeCompilerSettingProvider {
  fun fromContext(bazelRunner: BazelRunner, workspaceContext: WorkspaceContext): XCodeCompilerSettings?

  companion object {
    fun getInstance(project: Project): XCodeCompilerSettingProvider? =
      ApplicationManager.getApplication().getService<XCodeCompilerSettingProvider>(XCodeCompilerSettingProvider::class.java)
  }
}
