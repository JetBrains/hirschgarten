package org.jetbrains.bazel.redcodes

import com.intellij.bazel.python.backend.findBazelPythonShortestQualifiedName
import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.workspaceModel.ide.toPath
import com.jetbrains.python.inspections.unresolvedReference.PyUnresolvedReferencesInspection
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
 * `app/main.py` imports two `py_library` targets of the local repository `local_lib`.
 * `with_imports` has `imports = ["src"]`, so its source imports as `pkg.mod`.
 * `plain` has no `imports`, so its source imports by its path from the repository root, as `plain.pkg2.other`.
 * The resolve index must name both sources relative to the repository, not relative to the workspace root.
 */
@BazelTestApplication
class PythonLocalRepositoryImportsTest {

  private val fixture by bazelSyncCodeInsightFixture("redcodes/python_local_repo_imports")

  @Test
  fun testHighlighting(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    dumpStructure() shouldBe """
      |app.main [null]
      |  source root app/main.py [python-source]
      |local_lib.plain [null]
      |  source root libs/local_lib/plain/pkg2/other.py [python-source]
      |local_lib.with_imports [null]
      |  source root libs/local_lib/src [python-source]
      |resolve index name of libs/local_lib/src/pkg/mod.py: pkg.mod
      |resolve index name of libs/local_lib/plain/pkg2/other.py: plain.pkg2.other
    """.trimMargin()

    withContext(Dispatchers.EDT) {
      fixture.enableInspections(PyUnresolvedReferencesInspection())
      fixture.checkHighlighting("app/main.py")
    }
  }

  /**
   * Shows the source roots of each module and the resolve index names of the local repository sources,
   * relative to the project root.
   */
  private fun dumpStructure(): String = buildString {
    val modules = WorkspaceModel.getInstance(fixture.project).currentSnapshot
      .entities(ModuleEntity::class.java)
      .sortedBy { it.name }
    for (module in modules) {
      appendLine("${module.name} [${module.type?.name}]")
      for (sourceRoot in module.contentRoots.flatMap { it.sourceRoots }.sortedBy { it.url.url }) {
        appendLine("  source root ${sourceRoot.url.relativePath()} [${sourceRoot.rootTypeId.name}]")
      }
    }
    for (source in listOf("libs/local_lib/src/pkg/mod.py", "libs/local_lib/plain/pkg2/other.py")) {
      val name = fixture.project.findBazelPythonShortestQualifiedName(Path(fixture.tempDirPath).resolve(source))
      appendLine("resolve index name of $source: $name")
    }
  }.trimEnd()

  private fun VirtualFileUrl.relativePath(): String =
    Path(fixture.tempDirPath).relativize(toPath()).invariantSeparatorsPathString.ifEmpty { "." }
}
