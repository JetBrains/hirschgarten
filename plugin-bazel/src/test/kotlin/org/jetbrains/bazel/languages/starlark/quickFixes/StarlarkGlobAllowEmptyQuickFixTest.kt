package org.jetbrains.bazel.languages.starlark.quickFixes

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.pathString

@RunWith(JUnit4::class)
class StarlarkGlobAllowEmptyQuickFixTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/starlark/quickFixes/").pathString

  @Before
  fun beforeEach() {
    project.isBazelProject = true
    VfsRootAccess.allowRootAccess(this.testRootDisposable, this.testDataPath)
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
