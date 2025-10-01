package org.jetbrains.bazel.build

import com.intellij.build.output.BuildOutputParser
import com.intellij.build.output.JavacOutputParser
import com.intellij.build.output.KotlincOutputParser
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemOutputParserProvider
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.NotNull
import org.jetbrains.bazel.build.issueparser.BazelIssueOutputParser
import org.jetbrains.bazel.build.output.BazelBuildScriptErrorParser
import org.jetbrains.bazel.config.BazelPluginConstants
import java.io.File

/**
 * Provides a chain of BuildOutputParser implementations for Bazel process output.
 *
 * This mirrors the approach used by Gradle and Android integrations: we reuse stock parsers
 * where possible and add Bazel-specific ones for BUILD/Starlark errors.
 */
class BazelOutputParserProvider : ExternalSystemOutputParserProvider {
  override fun getExternalSystemId(): ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  override fun getBuildOutputParsers(@NotNull taskId: ExternalSystemTaskId): List<BuildOutputParser> {
    return defaultParsers()
  }

  /** Exposed for non-External-System runners. */
  fun getBuildOutputParsersForStandalone(): List<BuildOutputParser> = defaultParsers()

  /** Create parsers with project context for better file resolution. */
  fun getBuildOutputParsersWithContext(project: Project, workspaceRoot: File): List<BuildOutputParser> = listOf(
    // Bazel-specific parsers FIRST - they handle relative paths correctly
    // If they don't match, the built-in parsers will try next
    BazelIssueOutputParser(project, workspaceRoot),
    BazelBuildScriptErrorParser(),
    // Built-in IntelliJ parsers as fallback
    // These expect absolute paths or paths relative to project base
    JavacOutputParser("java", "scala"),
    KotlincOutputParser(),
  )

  private fun defaultParsers(): List<BuildOutputParser> = listOf(
    // Generic compiler outputs first
    JavacOutputParser("java", "scala"),
    KotlincOutputParser(),
    // Bazel-specific: BUILD/WORKSPACE/Starlark parse/runtime errors
    BazelBuildScriptErrorParser(),
  )
}
