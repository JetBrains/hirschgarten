package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.target.sync.TargetUtilsTargetPersistanceLayer

internal class IJBazelProjectContextService(private val project: Project) : BazelProjectContextService {
  private val propsService
    get() = project.service<BazelProjectProperties>()

  override var isBazelProject: Boolean
    get() = propsService.isBazelProject
    set(value) {
      propsService.isBazelProject = value
    }
  override var projectRootDir: VirtualFile?
    get() = propsService.rootDir
    set(value) {
      propsService.rootDir = value
    }
  override var workspaceName: String?
    get() = propsService.workspaceName
    set(value) {
      propsService.workspaceName = value
    }
  override var bazelBinPath: String?
    get() = project.service<BazelBinPathService>().bazelBinPath
    set(value) {
      project.service<BazelBinPathService>().bazelBinPath = value
    }
  override var bazelExecPath: String?
    get() = project.service<BazelBinPathService>().bazelExecPath
    set(value) {
      project.service<BazelBinPathService>().bazelExecPath = value
    }
  override val avoidExternalSystem: Boolean = false
  override val targetPersistenceLayer: BazelTargetPersistenceLayer = TargetUtilsTargetPersistanceLayer()
}
