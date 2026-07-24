package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.components.impl.stores.IProjectStore
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.ProjectStoreOwner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.project.BazelProjectProperties.Companion.bazelProjectProperties
import org.jetbrains.bazel.utils.findVirtualFile

internal class IJBazelProjectContextService(private val project: Project) : BazelProjectContextService {
  override var isBazelProject: Boolean
    get() = project.stateStoreOrNull?.storeDescriptor is BazelProjectStoreDescriptor
    set(_) = throw UnsupportedOperationException()

  private val projectRootDirFallback: VirtualFile? by lazy {
    // Fallback for running a Bazel run configuration in a non-Bazel project
    project.guessProjectDir()
  }

  override var projectRootDir: VirtualFile?
    get() {
      val bazelProjectStoreDescriptor = project.stateStoreOrNull?.storeDescriptor as? BazelProjectStoreDescriptor
      return bazelProjectStoreDescriptor?.historicalProjectBasePath?.findVirtualFile() ?: projectRootDirFallback
    }
    set(_) = throw UnsupportedOperationException()
  override var workspaceName: String?
    get() = project.bazelProjectProperties.workspaceName
    set(value) {
      project.bazelProjectProperties.workspaceName = value
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
}

/**
 * Normal [com.intellij.project.stateStore] throws [IllegalStateException] for [com.intellij.openapi.project.impl.DefaultProject],
 * this function returns `null` instead.
 */
@get:ApiStatus.Internal
val Project.stateStoreOrNull: IProjectStore?
  get() = (this as? ProjectStoreOwner)?.componentStore
