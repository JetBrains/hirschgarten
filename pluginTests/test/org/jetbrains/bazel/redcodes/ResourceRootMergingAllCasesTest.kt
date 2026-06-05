package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.kotlin.idea.i18n.KotlinInvalidBundleOrPropertyInspection
import org.junit.jupiter.api.Test

@BazelTestApplication
class ResourceRootMergingAllCasesTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.enableInspections(KotlinInvalidBundleOrPropertyInspection())
    fixture.copyBazelTestProject("redcodes/resource_merging_all_cases")
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("module/src/main/kotlin/com/example/Module.kt")
    }
  }
}
