package org.jetbrains.bazel.clion.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.persistence.type
import org.jetbrains.bsp.protocol.BuildTargetData

data class CcBuildTarget(val ruleContext: RuleContext?, val compilationContext: CompilationContext) : BuildTargetData {

  /**
   * Information collected from rule attributes directly; may not be present
   * for custom rules, since the semantic of their rule attributes is unknown.
   */
  data class RuleContext(
    val headers: List<ArtifactLocation>,
    val textualHeaders: List<ArtifactLocation>,
    val copts: List<String>,
    val conlyopts: List<String>,
    val cxxopts: List<String>,
    val args: List<String>,
    val includePrefix: String,
    val stripIncludePrefix: String,
  )

  /**
   * Information collected from the CcInfoProvider; should always be present.
   */
  data class CompilationContext(
    val headers: List<ArtifactLocation>,
    val defines: List<String>,
    val includes: List<ExecutionRootPath>,
    val quoteIncludes: List<ExecutionRootPath>,
    val systemIncludes: List<ExecutionRootPath>,
  )
}

internal class CcBuildTargetWorkspaceTypeContributor : WorkspaceTypeContributor {

  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList {
    type<CcBuildTarget>()
    type<CcBuildTarget.RuleContext>()
    type<CcBuildTarget.CompilationContext>()
  }
}
