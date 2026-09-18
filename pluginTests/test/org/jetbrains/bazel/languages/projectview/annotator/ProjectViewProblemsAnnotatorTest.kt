package org.jetbrains.bazel.languages.projectview.annotator

import com.intellij.openapi.application.EDT
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
class ProjectViewProblemsAnnotatorTest {
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
    val message = BazelProjectViewBundle.message("annotator.unsupported.section.warning")

    checkHighlighting(
      """
      directories:
        java/com/google/android/myproject
        javatests/com/google/android/myproject
        -javatests/com/google/android/myproject/not_this
      <warning descr="$message">not_supported_section</warning>: someValue
      """.trimIndent(),
    )
  }

  @Test
  fun `should report a value that is not one of the section variants`() {
    val message = BazelProjectViewBundle.message("annotator.unknown.variant.error", "not_a_boolean", "true, false")

    checkHighlighting("""shard_sync: <error descr="$message">not_a_boolean</error>""")
  }

  @Test
  fun `should accept a variant whose case differs from the one of the enum entry`() {
    checkHighlighting("""sharding_approach: EXPAND_AND_SHARD""")
  }

  @Test
  fun `should report a value that is not one of the enum variants`() {
    val message =
      BazelProjectViewBundle.message("annotator.unknown.variant.error", "not_an_approach", "expand_and_shard, query_and_shard, shard_only")

    checkHighlighting("""sharding_approach: <error descr="$message">not_an_approach</error>""")
  }

  @Test
  fun `should report an item of a list section that does not parse`() {
    val message = BazelProjectViewBundle.message("annotator.cannot.parse.value", "//...:invalidTarget")

    checkHighlighting(
      """
      targets:
        //valid/target
        <error descr="$message">//...:invalidTarget</error>
      """.trimIndent(),
    )
  }

  @Test
  fun `should warn about an unrecognised flag`() {
    val message = BazelProjectViewBundle.message("annotator.unknown.flag.error", "not_a_flag")

    checkHighlighting("""build_flags: <warning descr="$message">not_a_flag</warning>""")
  }

  @Test
  fun `should warn about a flag that is not applicable to the command of the section`() {
    val message = BazelProjectViewBundle.message("annotator.flag.not.allowed.here.error", "--dump_all", "[build]")

    checkHighlighting("""build_flags: <warning descr="$message">--dump_all</warning>""")
  }

  @Test
  fun `should report problems in dumb mode`() {
    val message = BazelProjectViewBundle.message("annotator.unknown.variant.error", "not_a_boolean", "true, false")

    checkHighlighting("""shard_sync: <error descr="$message">not_a_boolean</error>""", dumbMode = true)
  }

  private fun checkHighlighting(text: String, dumbMode: Boolean = false) {
    timeoutRunBlocking(30.seconds) {
      withContext(Dispatchers.EDT) {
        codeInsightFixture.configureByText(".bazelproject", text)
        if (dumbMode) {
          CodeInsightTestFixtureImpl.mustWaitForSmartMode(false, codeInsightFixture.testRootDisposable)
          DumbModeTestUtils.runInDumbModeSynchronously(codeInsightFixture.project) {
            codeInsightFixture.checkHighlighting()
          }
        }
        else {
          codeInsightFixture.checkHighlighting()
        }
      }
    }
  }
}
