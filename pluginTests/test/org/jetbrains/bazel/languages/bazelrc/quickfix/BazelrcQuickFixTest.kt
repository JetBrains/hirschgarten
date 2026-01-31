package org.jetbrains.bazel.languages.bazelrc.quickfix

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager

class BazelrcQuickFixTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("bazelrc/quickfix")

  fun testDeleteNoOpFlag() = doTest()

  fun testReplaceDeprecatedFlag() = doTest()

  fun testReplaceNegatedDeprecatedFlag() = doTest()

  private fun doTest() {
    VfsRootAccess.allowRootAccess(this.testRootDisposable, this.testDataPath)

    val name = getTestName(false)

    myFixture.configureByFile("$name.bazelrc")
    myFixture.doHighlighting()

    myFixture.launchAction(myFixture.findSingleIntention(""))

    myFixture.checkResultByFile("${name}Result.bazelrc")
  }
}
