package org.jetbrains.bazel.sync.workspace.languages

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path

interface LanguagePlugin<BuildTarget : BuildTargetData> {
  fun getSupportedLanguages(): Set<LanguageClass>

  fun calculateAdditionalSources(targetInfo: BspTargetInfo.TargetInfo): Sequence<SourceItem> = emptySequence()

  fun resolveAdditionalResources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> = emptySequence()

  fun prepareSync(project: Project, targets: Map<Label, BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {}

  fun transformSources(sources: List<SourceItem>): List<SourceItem> = sources

  suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): BuildTarget?
}
