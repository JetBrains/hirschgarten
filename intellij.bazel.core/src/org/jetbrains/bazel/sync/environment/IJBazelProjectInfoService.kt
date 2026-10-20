package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.components.impl.stores.IProjectStore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.ProjectStoreOwner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.utils.findVirtualFile

internal class IJBazelProjectInfoService(private val project: Project)
  : BazelProjectInfoService {

  override val isBazelProject: Boolean
    get() = project.stateStoreOrNull?.storeDescriptor is BazelProjectStoreDescriptor

  override val projectRootDir: VirtualFile?
    get() {
      val bazelProjectStoreDescriptor = project.stateStoreOrNull?.storeDescriptor as? BazelProjectStoreDescriptor
      return bazelProjectStoreDescriptor?.historicalProjectBasePath?.findVirtualFile()
             //?: project.base
             // Fallback for running a Bazel run configuration in a non-Bazel project
             ?: project.guessProjectDir()
    }
}

/**
 * Normal [com.intellij.project.stateStore] throws [IllegalStateException] for [com.intellij.openapi.project.impl.DefaultProject],
 * this function returns `null` instead.
 */
@get:ApiStatus.Internal
val Project.stateStoreOrNull: IProjectStore?
  get() = (this as? ProjectStoreOwner)?.componentStore

