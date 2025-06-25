package org.jetbrains.bazel.languages.bazelversion.quickfix

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.common.runBlocking
import org.jetbrains.bazel.languages.bazelversion.inspection.BazelVersionInspection
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.withNewVersionWhenPossible
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionResolver
import kotlin.io.path.pathString

class BazelVersionQuickFixTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/bazelversion/quickfix/").pathString

  override fun setUp() {
    super.setUp()

    ExtensionTestUtil.maskExtensions(BazelVersionResolver.ep,
                                     listOf(CustomBazelVersionResolver()),
                                     testRootDisposable)

    myFixture.enableInspections(BazelVersionInspection())
  }

  fun testQuickfix() {
    VfsRootAccess.allowRootAccess(this.testRootDisposable, this.testDataPath)

    val currentVersion = myFixture.configureByFile(".bazelversion").text.toBazelVersionLiteral()
    runBlocking { project.service<BazelVersionCheckerService>().refreshLatestBazelVersion(project, currentVersion) }
    myFixture.doHighlighting()

    myFixture.launchAction(myFixture.findSingleIntention("Update version"))

    myFixture.checkResultByFile(".bazelversion.after")
  }
}

class CustomBazelVersionResolver : BazelVersionResolver {
  override val id: String = "custom"
  override val name: String = "Custom"

  override suspend fun resolveLatestBazelVersion(
    project: Project,
    currentVersion: BazelVersionLiteral?,
  ): BazelVersionLiteral? = currentVersion?.withNewVersionWhenPossible("9999.0.0")
}
