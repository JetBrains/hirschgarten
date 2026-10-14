package org.jetbrains.bazel.languages.starlark.quickFixes

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.junit5.TestDisposable
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkGlobAllowEmptyQuickFixTest {
  private val myFixture by bazelCodeInsightFixture()

  @TestDisposable
  lateinit var disposable: Disposable

  @BeforeEach
  fun beforeEach() {
    myFixture.testDataPath = BazelPathManager.getTestFixture("starlark/quickFixes")
    VfsRootAccess.allowRootAccess(disposable, myFixture.testDataPath)
  }

  @Test
  fun testEmptyGlobAllowEmptyQuickfix() {
    myFixture.configureByFile("EmptyGlob.bzl")
    myFixture.launchAction(myFixture.findSingleIntention(StarlarkBundle.message("quickfix.glob.allow.empty")))
    myFixture.checkResultByFile("EmptyGlob.bzl.after")
  }

  @Test
  fun testEmptyGlobNotAllowedAllowEmptyQuickfix() {
    myFixture.addFileToProject("example1.java", "")
    myFixture.addFileToProject("example2.java", "")
    myFixture.configureByFile("EmptyGlobNotAllowed.bzl")
    myFixture.launchAction(myFixture.findSingleIntention(StarlarkBundle.message("quickfix.glob.allow.empty")))
    myFixture.checkResultByFile("EmptyGlobNotAllowed.bzl.after")
  }

  @Test
  fun testUnresolvedGlobAllowEmptyQuickfix() {
    myFixture.configureByFile("UnresolvedGlob.bzl")
    myFixture.launchAction(myFixture.findSingleIntention(StarlarkBundle.message("quickfix.glob.allow.empty")))
    myFixture.checkResultByFile("UnresolvedGlob.bzl.after")
  }
}
