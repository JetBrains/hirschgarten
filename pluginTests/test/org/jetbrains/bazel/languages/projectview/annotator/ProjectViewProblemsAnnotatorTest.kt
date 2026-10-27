package org.jetbrains.bazel.languages.projectview.annotator

import com.intellij.openapi.application.EDT
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
class ProjectViewProblemsAnnotatorTest {
  private val codeInsightFixture by bazelCodeInsightFixture()

  @Test
  fun `should warn about unsupported sections`() {
    val message = BazelProjectViewBundle.message("annotator.unsupported.section.warning", "not_supported_section")

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
    val message = BazelProjectViewBundle.message("annotator.flag.not.allowed.here.error", "--dump_all", "'build'")

    checkHighlighting("""build_flags: <warning descr="$message">--dump_all</warning>""")
  }

  @Test
  fun `should accept a build flag in sync_flags`() {
    checkHighlighting("""sync_flags: --define=foo=bar""")
  }

  @Test
  fun `should warn about deprecated python_debug_flags section and point at its replacement`() {
    val message = BazelProjectViewBundle.message("annotator.deprecated.section.with.replacement.warning", "python_debug_flags", "debug_flags")

    checkHighlighting(
      """
      <weak_warning descr="$message">python_debug_flags</weak_warning>:
        --run_under=valgrind
      """.trimIndent(),
    )
  }

  @Test
  fun `should report problems in dumb mode`() {
    val valueMessage = BazelProjectViewBundle.message("annotator.unknown.variant.error", "not_a_boolean", "true, false")
    val importMessage = BazelProjectViewBundle.message("annotator.import.unresolved.error", "missing.bazelproject")

    checkHighlighting(
      """
      shard_sync: <error descr="$valueMessage">not_a_boolean</error>
      import <error descr="$importMessage">missing.bazelproject</error>
      """.trimIndent(),
      dumbMode = true,
    )
  }

  @Test
  fun `should warn about deprecated test_sources`() {
    val message = BazelProjectViewBundle.message("annotator.deprecated.section.test_sources")

    checkHighlighting(
      """
      <weak_warning descr="$message">test_sources</weak_warning>:
        **/test/**
      """.trimIndent(),
    )
  }

  @Test
  fun `should warn about deprecated index_additional_files_in_directories section and point at its replacement`() {
    val message = BazelProjectViewBundle.message("annotator.deprecated.section.with.replacement.warning", "index_additional_files_in_directories", "index")

    checkHighlighting(
      """
      <weak_warning descr="$message">index_additional_files_in_directories</weak_warning>:
        *.xml
      """.trimIndent(),
    )
  }

  @Test
  fun `should warn about deprecated index_all_files_in_directories section and point at its replacement`() {
    val message = BazelProjectViewBundle.message("annotator.deprecated.section.with.replacement.warning", "index_all_files_in_directories", "index")

    checkHighlighting("""<weak_warning descr="$message">index_all_files_in_directories</weak_warning>: true""")
  }

  @Test
  fun `should report a required import whose file does not exist`() {
    val message = BazelProjectViewBundle.message("annotator.import.unresolved.error", "missing.bazelproject")

    checkHighlighting("""import <error descr="$message">missing.bazelproject</error>""")
  }

  @Test
  fun `should accept a required import whose file exists`() {
    codeInsightFixture.testBazel.writeFile("existing.bazelproject", "shard_sync: true")

    checkHighlighting("""import existing.bazelproject""")
  }

  @Test
  fun `should accept an optional import whose file does not exist`() {
    checkHighlighting("""try_import missing.bazelproject""")
  }

  @Test
  fun `should not check a relative path in a project that is not a Bazel project`() {
    useNonBazelProject()

    checkHighlighting("""bazel_binary: tools/missing_bazel""")
  }

  @Test
  fun `should not check an import in a project that is not a Bazel project`() {
    useNonBazelProject()

    checkHighlighting("""import missing.bazelproject""")
  }

  private fun useNonBazelProject() {
    val nonBazelProjectInfo = object : BazelProjectInfoService {
      override val isBazelProject: Boolean = false
      override val projectRootDir: VirtualFile? = null
    }
    codeInsightFixture.project.replaceService(BazelProjectInfoService::class.java, nonBazelProjectInfo, codeInsightFixture.testRootDisposable)
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
