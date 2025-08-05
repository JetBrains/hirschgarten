package org.jetbrains.bazel.languages.starlark.quickFixes

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.config.isBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkCreateFileQuickFixTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
    VfsRootAccess.allowRootAccess(this.testRootDisposable, this.testDataPath)
  }

  @Test
  fun testStarlarkCreateFileJavaQuickfix() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = ["Unresolved<caret>.java"],
      )
      """.trimIndent(),
    )
    myFixture.launchAction(myFixture.findSingleIntention("Create new file"))

    val createdFile = myFixture.findFileInTempDir("Unresolved.java")
    assertNotNull("File should have been created in the test's temp directory", createdFile)
  }

  @Test
  fun testStarlarkCreateFileKtQuickfix() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "myLib",
          srcs = ["Unresolved<caret>.kt"],
      )
      """.trimIndent(),
    )
    myFixture.launchAction(myFixture.findSingleIntention("Create new file"))

    val createdFile = myFixture.findFileInTempDir("Unresolved.kt")
    assertNotNull("File should have been created in the test's temp directory", createdFile)
  }
}
