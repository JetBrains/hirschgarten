package org.jetbrains.bazel.languages.projectview.references

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewLabelReferenceTest : BasePlatformTestCase() {
  @Test
  fun targetsShouldBeReferences() {
    myFixture.configureByText(".bazelproject",
    """
      targets:
        //:some_<caret>target
    """.trimIndent())

    assertNotNull(myFixture.getReferenceAtCaretPosition())
  }
}
