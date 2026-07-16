// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.bazel.test.integration

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.workspaceModel.ide.impl.jsonDump.WorkspaceModelJsonDumpService
import io.kotest.matchers.collections.shouldNotBeEmpty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterHelper
import org.jetbrains.bazel.sync.workspace.mapper.BazelWorkspaceResolver
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotBuilder
import org.jetbrains.bsp.protocol.TaskGroupId
import org.junit.jupiter.api.fail
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.relativeToOrNull
import kotlin.io.path.writeText

private val filePrefixes = listOf("file://", "jar://", "jrt://")

@OptIn(ExperimentalSerializationApi::class)
private val approvalJson = Json {
  prettyPrint = true
  explicitNulls = true
  prettyPrintIndent = "  "
}

internal suspend fun EntityStorage.toApprovalTestString(
  project: Project,
  relativize: (fileUrl: String) -> String,
): String {
  val jsonArray = project.service<WorkspaceModelJsonDumpService>().getWorkspaceEntitiesAsJsonArray(this)
  val relativized = jsonArray.relativizeFileUrls(relativize)
  return approvalJson.encodeToString(relativized)
}

private fun JsonElement.relativizeFileUrls(relativize: (String) -> String): JsonElement = when (this) {
  is JsonArray -> JsonArray(map { it.relativizeFileUrls(relativize) }.sortedBy { it.toString() })
  is JsonObject -> JsonObject(toSortedMap().mapValues { (_, v) -> v.relativizeFileUrls(relativize) })
  is JsonPrimitive -> {
    val raw = contentOrNull
    val isFilePath = isString
                     && raw != null
                     && filePrefixes.any { raw.startsWith(it) }
    if (isFilePath) {
      JsonPrimitive(relativize(raw))
    }
    else {
      this
    }
  }

  JsonNull -> this
}

internal fun compareOrUpdateGolden(goldenFile: Path, actual: String) {
  val shouldUpdate = !goldenFile.exists() || System.getProperty("update.workspace.model.golden") != null
  if (shouldUpdate) {
    goldenFile.parent.createDirectories()
    goldenFile.writeText(actual)
    fail("Golden file written: $goldenFile")
  }

  val expected = goldenFile.readText()
  if (expected != actual) {
    throw FileComparisonFailedError(
      message = "Workspace model differs from golden file. Rerun with -Dupdate.workspace.model.golden to regenerate.",
      expected = expected,
      actual = actual,
      expectedFilePath = goldenFile.toString(),
    )
  }
}


internal suspend fun doWorkspaceModelTest(
  project: Project,
  testData: WorkspaceImportApprovalTestData,
) {
  val taskId = TaskGroupId.EMPTY.task("main")
  val server = BazelServerService.getInstance(project).connection
  val resolvedWorkspace = BazelWorkspaceResolver.fetchWorkspace(project, scope = SecondPhaseSync, build = false, allKnownTargets = null, taskId = taskId)
  resolvedWorkspace.targets.shouldNotBeEmpty()

  val workspaceSnapshot = WorkspaceSnapshotBuilder.build(
    project = project,
    projectView = ProjectView.EMPTY,
    repoMapping = resolvedWorkspace.repoMapping,
    resolved = resolvedWorkspace,
  )
  val builder = MutableEntityStorage.create()
  reportSequentialProgress { reporter ->
    val helper = WorkspaceImporterHelper(
      project = project,
      taskConsole = project.syncConsole,
      progressReporter = reporter,
      taskId = taskId,
      builder = builder,
    )

    helper.invoke(reporter, workspaceSnapshot)
  }

  val relativizeDirsOrder =
    server.runWithServer { listOf(it.bazelInfo.execRoot, it.bazelInfo.outputBase, it.bazelInfo.bazelBin, project.rootDir.toNioPath()) }

  val expected = testData.expectedWorkspaceModelFile
  val actual = builder.toSnapshot().toApprovalTestString(project) { uri ->
    val normalized = FileUtilRt.toSystemDependentName(uri)
    val scheme = normalized.substringBefore(":")
    val path = normalized.substringAfter("://").toNioPathOrNull() ?: return@toApprovalTestString uri
    if (relativizeDirsOrder.none { path.startsWith(it) }) {
      uri
    }
    else {
      val relative = relativizeDirsOrder.firstNotNullOfOrNull {
        if (!path.startsWith(it)) {
          null
        }
        else {
          path.relativeToOrNull(it)
        }
      } ?: path
      "$scheme://$relative"
    }
  }

  compareOrUpdateGolden(expected, actual)

}

internal fun setupRemoteJdk(projectDir: Path, version: String) {
  projectDir.resolve(".bazelrc").writeText(
    text = """
                ${"\n"}common --java_language_version=$version
                common --java_runtime_version=remotejdk_$version
                common --tool_java_language_version=$version
                common --tool_java_runtime_version=remotejdk_$version
              """.trimIndent(),
    options = arrayOf(StandardOpenOption.APPEND, StandardOpenOption.CREATE),
  )
}

