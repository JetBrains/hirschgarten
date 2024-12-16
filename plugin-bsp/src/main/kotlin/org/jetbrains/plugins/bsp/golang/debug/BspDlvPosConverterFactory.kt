package org.jetbrains.plugins.bsp.golang.debug

import com.goide.dlv.location.DlvPositionConverter
import com.goide.dlv.location.DlvPositionConverterFactory
import com.goide.sdk.GoSdkService
import com.google.common.collect.ImmutableList
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.jetbrains.bsp.protocol.GoDebuggerDataResult
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import java.io.File

class BspDlvPosConverterFactory : DlvPositionConverterFactory {
  override fun createPositionConverter(
    project: Project,
    module: Module?,
    remotePaths: Set<String>,
  ): DlvPositionConverter {
    val result: GoDebuggerDataResult =
      runBlocking {
        project.connection.runWithServer { bspServer, bazelBuildServerCapabilities ->
          if (bazelBuildServerCapabilities.goDebuggerDataProvider) {
            bspServer.goDebuggerData().await()
          } else {
            error("Required bazelBuildServerCapabilities are not available")
          }
        }
      }

    val goRoot = GoSdkService.getInstance(project).getSdk(module).homePath
    val workspaceRoot =
      WorkspaceRoot(
        File(
          project.workspaceFile?.let { workspaceFile ->
            if (workspaceFile.extension == "iws") {
              workspaceFile.parent.path
            } else {
              workspaceFile.parent.parent.path
            }
          } ?: error("Workspace file is not available for the project"),
        ),
      )
    val buildArtifactDirectories = ImmutableList.of<String>() // todo: implement
    val resolver =
      ExecutionRootPathResolver(
        buildArtifactDirectories,
        File(result.execRoot),
        File(result.outputBase),
        object : WorkspacePathResolver {
          override fun findPackageRoot(relativePath: String?): File? = workspaceRoot.directory()
        },
      )

    val cgoTrimmedPathsHandler = null // todo: implement
    return BspDlvPositionConverter(workspaceRoot, goRoot, resolver, remotePaths, cgoTrimmedPathsHandler)
  }
}
