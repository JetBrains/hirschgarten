package org.jetbrains.bazel.ui.notifications

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.snapshot.MissingGeneratedSources
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.generatedSources
import java.nio.file.Path
import kotlin.io.path.exists

@ApiStatus.Internal
abstract class UnresolvedImportChecker {
  abstract fun isApplicable(project: Project, file: VirtualFile): Boolean

  abstract fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean

  protected open fun generatedOutputs(target: BuildTarget): Sequence<OutputLocation> =
    target.generatedSources() + target.findBuildData<MissingGeneratedSources>()?.sources?.getOutputLocations().orEmpty()

  protected open fun owningTargets(project: Project, snapshot: WorkspaceSnapshot, path: Path): List<WorkspaceTargetKey> =
    snapshot.fileToTarget.getTargetsByFile(path)

  /** Checks the generated outputs of the file's targets and their direct dependencies */
  protected fun fileHasMissingGeneratedCode(project: Project, file: VirtualFile?): Boolean {
    val path = file?.toNioPathOrNull() ?: return false
    val snapshot = project.workspaceSnapshotOrNull ?: return false
    val keys = owningTargets(project, snapshot, path)
    if (keys.isEmpty()) return false
    val resolver = DefaultOutputLocationResolver.createExecrootResolving(snapshot.bazelInfo)
    val repositories = snapshot.repoMapping.getLocalRepositories()
    return keys.asSequence()
      .flatMap { key -> sequenceOf(key) + snapshot.targetGraph.findAllSuccessors(key, strict = false) }
      .distinct()
      .any { key ->
        ProgressManager.checkCanceled()
        val target = snapshot.targets.findTargetByKey(key) ?: return@any false
        generatedOutputs(target).any { resolver.resolve(it, repositories)?.exists() == false }
      }
  }

  companion object {
    val ep: ExtensionPointName<UnresolvedImportChecker> = ExtensionPointName.create("org.jetbrains.bazel.unresolvedImportChecker")
  }
}
