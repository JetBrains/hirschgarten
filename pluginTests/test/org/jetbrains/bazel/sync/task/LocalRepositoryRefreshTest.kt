package org.jetbrains.bazel.sync.task

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.assertLastSyncSucceeded
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.writeProjectView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

/** The sync refreshes the VFS of a local repository outside the project root before the importers read it. */
@OptIn(ExperimentalPathApi::class)
@BazelTestApplication
class LocalRepositoryRefreshTest {
  private val sibling by tempPathFixture()
  private val javaRoot get() = sibling.toRealPath().resolve("qa_external_pkg/src/main/java")

  private val fixture by bazelSyncCodeInsightFixture("redcodes/local_repo_resources", configure = { f ->
    val repo = sibling.toRealPath()
    VfsRootAccess.allowRootAccess(f.project, repo.toString())
    val workspace = Path.of(f.tempDirPath)
    val module = workspace.resolve("MODULE.bazel")
    val original = module.readText()
    check(original.contains("path = \"local_repo\""))
    module.writeText(original.replace("path = \"local_repo\"", "path = \"${repo.invariantSeparatorsPathString}\""))
    writeProjectView(f.project, "targets:\n  @local_repo//qa_external_pkg:app\n")
    workspace.resolve("local_repo").copyToRecursively(repo, followLinks = false, overwrite = true)
    javaRoot.resolve("com/acme").createDirectories().resolve("App.java").writeText("package com.acme; public class App {}")
    javaRoot.resolve("tools").createDirectories()
    repo.resolve("qa_external_pkg/BUILD.bazel").writeText("""
      load("@rules_java//java:java_library.bzl", "java_library")
      java_library(name = "app", srcs = ["src/main/java/com/acme/App.java"])
    """.trimIndent())
  })

  private fun importedRoots(): List<Path> =
    WorkspaceModel.getInstance(fixture.project).currentSnapshot.entities(SourceRootEntity::class.java)
      .filter { it.url.url.startsWith("file:") && it.url.url.contains("/qa_external_pkg/") }
      .map { it.url.toPath().toRealPath() }
      .toList()

  @Test
  fun `resync detects a new BUILD file in a sibling local_path_override`(): Unit = timeoutRunBlocking(timeout = 6.minutes) {
    assertLastSyncSucceeded(fixture.project)
    assertEquals(listOf(javaRoot), importedRoots())
    val tools = javaRoot.resolve("tools")
    check(checkNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(tools)).children.isEmpty())
    // the VFS does not know the new file, so only the refresh of the sync can find it
    tools.resolve("BUILD.bazel").writeText("# A new Bazel package\n")

    fixture.performBazelSync()

    assertLastSyncSucceeded(fixture.project)
    assertEquals(listOf(javaRoot.resolve("com/acme")), importedRoots())
  }
}
