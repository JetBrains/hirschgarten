package org.jetbrains.bazel.workspace.sync.jvm

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.SourceRootOptimizationMode
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.JavaSourceRootPatterns
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.ProjectViewGlobPattern
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaProviderData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JdepsJar
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmDependency
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmOutputs
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.ScalaBuildTarget
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.persistence.sealed
import org.jetbrains.bazel.sync.workspace.persistence.type

internal class JvmWorkspaceTypeContributor : WorkspaceTypeContributor  {
  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList {
    type<JvmBuildTarget>()
    type<KotlinBuildTarget>()
    type<ScalaBuildTarget>()
    type<JvmOutputs>()
    type<JdepsJar>()
    type<JavaProviderData>()
    type<JavaToolchainData>()
    sealed<JvmDependency>()
    type<JavaWorkspaceSyncConfig>()
    sealed<SourceRootOptimizationMode>()
    type<JavaSourceRootPatterns>()
    type<ProjectViewGlobPattern>()
  }
}
