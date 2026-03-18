package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.references.resolveLabel
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.excludedRoots

internal class TargetUtilsProjectViewCodeInsightSupport(private val project: Project) :
  ProjectViewCodeInsightSupport {
  override val allTargetsAndLibrariesLabels: Sequence<String>
    get() = project.targetUtils.allTargetsAndLibrariesLabels.asSequence()
  override val excludedRoots: Set<VirtualFile>
    get() = project.excludedRoots() ?: emptySet()

  override fun resolvePsiFromLabel(
    label: Label,
    containingFile: VirtualFile?,
    acceptOnlyFileTarget: Boolean,
  ): PsiElement? = resolveLabel(project, label, containingFile, acceptOnlyFileTarget)
}
