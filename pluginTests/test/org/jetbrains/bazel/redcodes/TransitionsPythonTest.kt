package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class TransitionsPythonTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testPythonTransition(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    fixture.copyBazelTestProject("redcodes/transitions_python")
    fixture.setBazelVersion("9.1.0")
    fixture.performBazelSync(buildProject = false)
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("AppA.py")
      fixture.checkHighlighting("AppB.py")

      val leafModuleNames = WorkspaceModel.getInstance(fixture.project).currentSnapshot
        .entities(ModuleEntity::class.java)
        .map { it.name }
        .filter { it.contains("leaf") }
        .toSet()
      leafModuleNames shouldHaveSize 2
    }
  }
}
