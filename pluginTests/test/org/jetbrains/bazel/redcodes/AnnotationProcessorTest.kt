package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class AnnotationProcessorTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/annotation_processor")
    fixture.performBazelSync(buildProject = true)
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("JavaLib.java")
      fixture.checkHighlighting("KtLib.kt")
    }
  }
}
