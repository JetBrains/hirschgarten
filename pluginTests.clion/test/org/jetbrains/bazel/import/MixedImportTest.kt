package org.jetbrains.bazel.import

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.Test

@BazelTestApplication
class MixedImportTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testImportedTargets(): Unit = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("import/mixed")
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      val importedTargets = fixture.project.targetStorage.allTargetSummaries().asSequence().filter { it.isWorkspace }.map { it.id }.toSet()
      // Verify that also the non-executable targets (that are not imported by default) are present of every language
      importedTargets shouldContain Label.parse("//cpp:library")
      importedTargets shouldContain Label.parse("//kotlin:library")
    }
  }
}
