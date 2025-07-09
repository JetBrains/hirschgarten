package org.jetbrains.bazel.languages.projectview.annotator

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.rd.util.firstOrNull
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewAnnotatorTest : BasePlatformTestCase() {

  @Test
  fun `should warn about unsupported sections`() {
    val someUnsupportedSection = ProjectViewSection.KEYWORD_MAP
      .filter { !ProjectViewSection.isSectionSupported(it.value.sectionName) }
      .firstOrNull()?.value?.sectionName ?: return

    val warningMessage = ProjectViewBundle.getMessage("annotator.unsupported.section.warning")

    myFixture.configureByText(".bazelproject", """
      directories:
        directories:
        java/com/google/android/myproject
        javatests/com/google/android/myproject
        -javatests/com/google/android/myproject/not_this
      <warning descr="$warningMessage">$someUnsupportedSection</warning>: someValue
    """.trimIndent())

    myFixture.checkHighlighting()
  }
}
