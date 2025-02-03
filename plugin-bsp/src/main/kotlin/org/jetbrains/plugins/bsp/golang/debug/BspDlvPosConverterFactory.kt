package org.jetbrains.plugins.bsp.golang.debug

import com.goide.dlv.location.DlvPositionConverter
import com.goide.dlv.location.DlvPositionConverterFactory
import com.goide.sdk.GoSdkService.getInstance
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

class BspDlvPosConverterFactory : DlvPositionConverterFactory {
  override fun createPositionConverter(
    project: Project,
    module: Module?,
    remotePaths: Set<String>,
  ): DlvPositionConverter = BspDlvPositionConverter(project, remotePaths, getInstance(project).getSdk(module).homePath)
}
