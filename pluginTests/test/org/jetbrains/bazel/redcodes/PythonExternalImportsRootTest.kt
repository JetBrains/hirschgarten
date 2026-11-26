package org.jetbrains.bazel.redcodes

import com.intellij.codeInspection.wrongPackageStatement.WrongPackageStatementInspection
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.workspaceModel.ide.toPath
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.time.Duration.Companion.minutes

/**
 * The `imports = ["."]` of a `py_library` in a local external repository makes a Python source root at the root of
 * that repository, not at the project root. `java/Main.java` keeps a single-file source root, so a Python root at the
 * project root would set the package of its directory and give a false package mismatch error.
 */
@BazelTestApplication
class PythonExternalImportsRootTest {

  private val fixture by bazelSyncCodeInsightFixture("redcodes/python_external_imports_root")

  @Test
  fun testExternalImportsRootDoesNotSetJavaPackage(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    val structure = dumpModuleStructure()
    structure shouldBe """
      |java.main [JAVA_MODULE]
      |  content root java/Main.java
      |    source root java/Main.java [java-source] packageMatchesDirectory=false
      |py.tool [PYTHON_MODULE]
      |  content root py/tool.py
      |    source root py/tool.py [python-source]
      |py_repo.lib [PYTHON_MODULE]
      |  content root py_repo
      |    source root py_repo [python-source]
    """.trimMargin()

    withContext(Dispatchers.EDT) {
      fixture.enableInspections(WrongPackageStatementInspection())
      fixture.checkHighlighting("java/Main.java")
    }
  }

  /**
   * Shows each module with its content roots and source roots, relative to the project root.
   * A source root without Java properties shows no `packageMatchesDirectory` value.
   */
  private fun dumpModuleStructure(): String {
    val modules = WorkspaceModel.getInstance(fixture.project).currentSnapshot
      .entities(ModuleEntity::class.java)
      .sortedBy { it.name }
    return buildString {
      for (module in modules) {
        appendLine("${module.name} [${module.type?.name}]")
        for (contentRoot in module.contentRoots.sortedBy { it.url.url }) {
          appendLine("  content root ${contentRoot.url.relativePath()}")
          for (sourceRoot in contentRoot.sourceRoots.sortedBy { it.url.url }) {
            appendLine("    source root ${sourceRoot.url.relativePath()} [${sourceRoot.rootTypeId.name}]${sourceRoot.packageMatchesDirectoryText()}")
          }
        }
      }
    }.trimEnd()
  }

  private fun VirtualFileUrl.relativePath(): String =
    Path(fixture.tempDirPath).relativize(toPath()).invariantSeparatorsPathString.ifEmpty { "." }

  private fun SourceRootEntity.packageMatchesDirectoryText(): String =
    javaSourceRoots.singleOrNull()?.let { " packageMatchesDirectory=${it.packageMatchesDirectory}" } ?: ""
}
