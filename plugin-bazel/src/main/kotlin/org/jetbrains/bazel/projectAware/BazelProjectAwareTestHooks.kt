package org.jetbrains.bazel.projectAware

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly

@ApiStatus.Internal
@TestOnly
object BazelProjectAwareTestHooks {
  @JvmStatic
  fun initializeBazelWorkspaceForTest(project: Project) {
    BazelWorkspace.getInstance(project).initialize()
  }

  @JvmStatic
  fun resetBazelWorkspaceForTest(project: Project) {
    project.getServiceIfCreated(BazelWorkspace::class.java)?.resetForTest()
  }

  @JvmStatic
  fun isBazelWorkspaceInitializedForTest(project: Project): Boolean =
    project.getServiceIfCreated(BazelWorkspace::class.java)?.isInitializedForTest() == true

  @JvmStatic
  fun setLastBuiltByJpsForTest(project: Project, lastBuiltByJps: Boolean) {
    BazelProjectModuleBuildTasksTracker.getInstance(project).lastBuiltByJps = lastBuiltByJps
  }

  @JvmStatic
  fun resetLastBuiltByJpsForTest(project: Project) {
    project.getServiceIfCreated(BazelProjectModuleBuildTasksTracker::class.java)?.lastBuiltByJps = false
  }

  @JvmStatic
  fun wasLastBuildByJpsForTest(project: Project): Boolean =
    project.getServiceIfCreated(BazelProjectModuleBuildTasksTracker::class.java)?.lastBuiltByJps == true
}
