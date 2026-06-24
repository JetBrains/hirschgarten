package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class TransitionsJavaTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testJavaTransition() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/transitions_java")
    fixture.setBazelVersion("9.1.0")
    fixture.performBazelSync(buildProject = true)
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
