package org.jetbrains.bazel.golang.debug

import com.goide.dlv.location.DlvPositionConverter
import com.goide.dlv.location.DlvPositionConverterFactory
import com.goide.sdk.GoSdkService.getInstance
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

class BazelDlvPosConverterFactory : DlvPositionConverterFactory {
  override fun createPositionConverter(
    project: Project,
    module: Module?,
    remotePaths: Set<String>,
  ): DlvPositionConverter = BspDlvPositionConverter(project, remotePaths, getInstance(project).getSdk(module).homePath)
}
