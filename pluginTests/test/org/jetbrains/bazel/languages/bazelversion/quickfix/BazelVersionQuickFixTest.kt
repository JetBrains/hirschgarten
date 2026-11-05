package org.jetbrains.bazel.languages.bazelversion.quickfix

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.ExtensionTestUtil
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.languages.bazelversion.inspection.BazelVersionInspection
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.withNewVersionWhenPossible
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionResolver
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelVersionQuickFixTest {
  private val myFixture by bazelCodeInsightFixture()

  @BeforeEach
  fun setUp() {
    myFixture.testDataPath = BazelPathManager.getTestFixture("bazelversion/quickfix")

    ExtensionTestUtil.maskExtensions(
      BazelVersionResolver.ep,
      listOf(CustomBazelVersionResolver()),
      myFixture.testBazel.disposable,
    )

    myFixture.enableInspections(BazelVersionInspection())
  }

  @Test
  fun testQuickfix() {
    val file = myFixture.configureByFile(".bazelversion")
    val currentVersion = runReadActionBlocking { file.text }.let { BazelVersionLiteral.fromString(it) }
    val project = myFixture.project
    runBlocking { project.service<BazelVersionCheckerService>().refreshLatestBazelVersion(project, currentVersion) }
    myFixture.doHighlighting()

    myFixture.launchAction(myFixture.findSingleIntention("Update version"))

    myFixture.checkResultByFile(".bazelversion.after")
  }
}

class CustomBazelVersionResolver : BazelVersionResolver {
  override val id: String = "custom"
  override val name: String = "Custom"

  override suspend fun resolveLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?): BazelVersionLiteral? =
    currentVersion?.withNewVersionWhenPossible("9999.0.0")
}
