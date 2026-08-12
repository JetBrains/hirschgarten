package org.jetbrains.bazel.languages.projectview.annotator

import com.intellij.openapi.application.EDT
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
class ProjectViewAnnotatorTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDirFixture = tempPathFixture()
  private val moduleFixture = projectFixture.moduleFixture(tempDirFixture, addPathToSourceRoot = true)
  private val codeInsightFixture by codeInsightFixture(projectFixture, tempDirFixture)

  @BeforeEach
  fun setUp() {
    moduleFixture.get()
  }

  @Test
  fun `should warn about unsupported sections`() {
    val message = ProjectViewBundle.getMessage("annotator.unsupported.section.warning")

    checkHighlighting(
      """
      directories:
        directories:
        java/com/google/android/myproject
        javatests/com/google/android/myproject
        -javatests/com/google/android/myproject/not_this
      <warning descr="$message">iAmNotSupported</warning>: someValue
      """.trimIndent(),
    )
  }

  @Test
  fun `should report a value that is not one of the section variants`() {
    val message = ProjectViewBundle.getMessage("annotator.unknown.variant.error") + " true, false"

    checkHighlighting("""shard_sync: <error descr="$message">not_a_boolean</error>""")
  }

  @Test
  fun `should warn about an unrecognised flag`() {
    val message = ProjectViewBundle.getMessage("annotator.unknown.flag.error", "not_a_flag")

    checkHighlighting("""build_flags: <warning descr="$message">not_a_flag</warning>""")
  }

  @Test
  fun `should warn about a flag that is not applicable to the command of the section`() {
    val message = ProjectViewBundle.getMessage("annotator.flag.not.allowed.here.error", "--dump_all", "[build]")

    checkHighlighting("""build_flags: <warning descr="$message">--dump_all</warning>""")
  }

  private fun checkHighlighting(text: String) {
    timeoutRunBlocking(30.seconds) {
      withContext(Dispatchers.EDT) {
        codeInsightFixture.configureByText(".bazelproject", text)
        codeInsightFixture.checkHighlighting()
      }
    }
  }
}
