package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

// FIXME: this interface is used to decouple project view section definitions from targetUtils
//  in the ideal case each section could define reference/key to `CompletionProvider` and then backend
//  can link this reference to the actual completion provider
//  new API would contain data access endpoints for code insight in separate artifact
//
// TODO: I'm going to redesign code insights API for entire plugin in order to provide
//  unified way of interacting with data obtained from bazel and this will allow us to fully
//  decouple IJ code analysis stuff from target utils
@ApiStatus.Internal
interface ProjectViewCodeInsightSupport {
  val allTargetsAndLibrariesLabels: Sequence<String>
  val excludedRoots: Set<VirtualFile>

  fun resolvePsiFromLabel(label: Label, containingFile: VirtualFile? = null, acceptOnlyFileTarget: Boolean = false): PsiElement?
}
