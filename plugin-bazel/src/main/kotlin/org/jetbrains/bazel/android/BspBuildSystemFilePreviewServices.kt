package org.jetbrains.bazel.android

import com.android.tools.idea.concurrency.createCoroutineScope
import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager.BuildMode
import com.android.tools.idea.projectsystem.getProjectSystem
import com.android.tools.idea.rendering.BuildTargetReference
import com.android.tools.idea.rendering.tokens.BuildSystemFilePreviewServices
import com.android.tools.idea.rendering.tokens.BuildSystemFilePreviewServices.BuildListener
import com.android.tools.idea.rendering.tokens.BuildSystemFilePreviewServices.BuildServices
import com.google.common.util.concurrent.SettableFuture
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.coroutines.BspCoroutineService
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.util.concurrent.atomic.AtomicReference

class BspBuildSystemFilePreviewServices : BuildSystemFilePreviewServices<BspAndroidProjectSystem, BspBuildTargetReference> {
  override fun isApplicable(buildTargetReference: BuildTargetReference): Boolean = buildTargetReference is BspBuildTargetReference

  override fun isApplicable(projectSystem: AndroidProjectSystem): Boolean = projectSystem is BspAndroidProjectSystem

  override val buildTargets: BuildSystemFilePreviewServices.BuildTargets =
    object : BuildSystemFilePreviewServices.BuildTargets {
      override fun from(module: Module, targetFile: VirtualFile): BuildTargetReference = fromModuleOnly(module)

      override fun fromModuleOnly(module: Module): BuildTargetReference = BspBuildTargetReference(module)
    }

  override val buildServices: BuildServices<BspBuildTargetReference> =
    object : BuildServices<BspBuildTargetReference> {
      override fun getLastCompileStatus(buildTarget: BspBuildTargetReference): ProjectSystemBuildManager.BuildStatus =
        ProjectSystemBuildManager.BuildStatus.UNKNOWN

      override fun buildArtifacts(buildTargets: Collection<BspBuildTargetReference>) {
        val project = buildTargets.firstOrNull()?.project ?: return
        BspCoroutineService.getInstance(project).start {
          ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = true)
        }
      }
    }

  override fun subscribeBuildListener(
    project: Project,
    parentDisposable: Disposable,
    listener: BuildListener,
  ) {
    project.getProjectSystem().getBuildManager().addBuildListener(
      parentDisposable,
      object : ProjectSystemBuildManager.BuildListener {
        private val future = AtomicReference<SettableFuture<BuildListener.BuildResult>>()

        override fun buildStarted(mode: BuildMode) {
          val newFuture = SettableFuture.create<BuildListener.BuildResult>()
          if (!future.compareAndSet(null, newFuture)) return
          listener.buildStarted(BuildListener.BuildMode.COMPILE, newFuture)
        }

        override fun buildCompleted(result: ProjectSystemBuildManager.BuildResult) {
          val oldFuture = future.getAndSet(null) ?: return
          parentDisposable.createCoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.EDT) {
              oldFuture.set(BuildListener.BuildResult(result.status, GlobalSearchScope.allScope(project)))
            }
          }
        }
      },
    )
  }
}

data class BspBuildTargetReference(override val module: Module) : BuildTargetReference
