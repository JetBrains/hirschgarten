package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class TransitionsJavaTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/transitions_java",
    buildProject = true,
    bazelVersion = "9.1.0",
  )

  @Test
  fun testJavaTransition() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("AppA.java")
      fixture.checkHighlighting("AppB.java")

      // there should be two distinct modules `leaf-xxxxxxx`
      val leafModuleNames = WorkspaceModel.getInstance(fixture.project).currentSnapshot
        .entities(ModuleEntity::class.java)
        .map { it.name }
        .filter { it.contains("leaf") }
        .toSet()
      check(leafModuleNames.size == 2) { "expected distinct module per configuration" }
    }
  }
}
