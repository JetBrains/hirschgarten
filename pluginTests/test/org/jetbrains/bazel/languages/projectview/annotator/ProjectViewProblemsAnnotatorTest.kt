package org.jetbrains.bazel.languages.projectview.annotator

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.EDT
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.replaceService
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.languages.projectview.quickfix.inspectCodeSuppressActionsAtCaret
import org.jetbrains.bazel.languages.projectview.quickfix.suppressInInspectCode
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
class ProjectViewProblemsAnnotatorTest {
  private val codeInsightFixture by bazelCodeInsightFixture()

  private val rootDir: Path
    get() = codeInsightFixture.project.rootDir.toNioPath()

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
  fun `should accept a dot idea directory inside of the project root`() {
    checkHighlighting("""dot_idea_directory_location: custom/.idea""")
  }

  @Test
  fun `should report a relative dot idea directory outside of the project root`() {
    val outsidePath = rootDir.parent.resolve("outside").resolve(".idea")
    val message = BazelProjectViewBundle.message("annotator.path.outside.of.project.root.error", outsidePath, rootDir)

    checkHighlighting("""dot_idea_directory_location: <error descr="$message">../outside/.idea</error>""")
  }

  @Test
  fun `should report an absolute dot idea directory outside of the project root`() {
    val outsidePath = rootDir.parent.resolve("outside").resolve(".idea")
    val message = BazelProjectViewBundle.message("annotator.path.outside.of.project.root.error", outsidePath, rootDir)

    checkHighlighting("""dot_idea_directory_location: <error descr="$message">$outsidePath</error>""")
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

  @Test
  fun `should not report a suppressed warning`() {
    checkHighlighting(
      """
      # suppress: unsupported_section
      not_supported_section: someValue
      """.trimIndent(),
    )
  }

  @Test
  fun `should report a warning below an indented suppression comment of the section above`() {
    val message = BazelProjectViewBundle.message("annotator.unsupported.section.warning", "not_supported_section")

    checkHighlighting(
      """
      targets:
        //a
        # suppress: unsupported_section
      <warning descr="$message">not_supported_section</warning>: someValue
      """.trimIndent(),
    )
  }

  @Test
  fun `should report a warning again when its suppression ID is edited`() {
    val message = BazelProjectViewBundle.message("annotator.unsupported.section.warning", "not_supported_section")

    timeoutRunBlocking(30.seconds) {
      withContext(Dispatchers.EDT) {
        codeInsightFixture.configureByText(
          ".bazelproject",
          """
          # suppress: unsupported_section<caret>
          not_supported_section: someValue
          """.trimIndent(),
        )
        codeInsightFixture.doHighlighting().map { it.description } shouldNotContain message

        codeInsightFixture.type("\b\b")

        codeInsightFixture.doHighlighting().map { it.description } shouldContain message
      }
    }
  }

  @Test
  fun `should offer the options of a warning without quick fixes`() {
    val options = BazelProjectViewBundle.message("annotator.warning.options", "unsupported_section")

    inEditor("not_supported<caret>_section: someValue") {
      codeInsightFixture.availableIntentions.map { it.text } shouldContain options
    }
  }

  @Test
  fun `should offer only the suppress actions in the options of a warning`() {
    inEditor("not_supported<caret>_section: someValue") {
      suppressActions().map { it.text } shouldContainExactly listOf("section").map {
        BazelProjectViewBundle.message("suppress.for.$it", "unsupported_section")
      }
    }
  }

  @Test
  fun `should offer all suppress actions for an item`() {
    inEditor("build_flags:\n  not_a<caret>_flag") {
      suppressActions().map { it.text } shouldContainExactly listOf("item", "section").map {
        BazelProjectViewBundle.message("suppress.for.$it", "unknown_flag")
      }
    }
  }

  @Test
  fun `should title the options of a warning with the project view name`() {
    val options = BazelProjectViewBundle.message("annotator.warning.options", "unsupported_section")

    inEditor("not_supported<caret>_section: someValue") {
      val info = codeInsightFixture.doHighlighting().single { it.severity == HighlightSeverity.WARNING }
      val title = info.findRegisteredQuickFix { descriptor, _ -> descriptor.displayName.takeIf { descriptor.action.text == options } }

      title shouldBe BazelProjectViewBundle.message("annotator.warning.options.title")
    }
  }

  @Test
  fun `should put the suppress actions of a warning with a quick fix in the submenu of the fix`() {
    val options = BazelProjectViewBundle.message("annotator.warning.options", "deprecation")

    inEditor("index_all<caret>_files_in_directories: true") {
      codeInsightFixture.availableIntentions.map { it.text } shouldNotContain options
      suppressActions().map { it.text } shouldContainExactly listOf("section").map {
        BazelProjectViewBundle.message("suppress.for.$it", "deprecation")
      }
    }
  }

  @Test
  fun `should not offer suppress actions for an error`() {
    inEditor("shard_sync: not_a<caret>_boolean") {
      options().shouldBeEmpty()
    }
  }

  @Test
  fun `should suppress a warning for its section`() {
    suppress(
      before = """
        directories:
          dirA
        not_supported<caret>_section: someValue
      """,
      action = "suppress.for.section",
      id = "unsupported_section",
      after = """
        directories:
          dirA
        # suppress: unsupported_section
        not_supported_section: someValue
      """,
    )
  }

  @Test
  fun `should suppress a warning for its item`() {
    suppress(
      before = """
        build_flags:
          not_a<caret>_flag
      """,
      action = "suppress.for.item",
      id = "unknown_flag",
      after = """
        build_flags:
          # suppress: unknown_flag
          not_a_flag
      """,
    )
  }

  @Test
  fun `should add an ID to the suppression comment above the section`() {
    suppress(
      before = """
        # suppress: unknown_flag the reason
        not_supported<caret>_section: someValue
      """,
      action = "suppress.for.section",
      id = "unsupported_section",
      after = """
        # suppress: unknown_flag, unsupported_section the reason
        not_supported_section: someValue
      """,
    )
  }

  @Test
  fun `should not offer item suppression for a value on the section line`() {
    inEditor("build_flags: not_a<caret>_flag") {
      suppressActions().map { it.text } shouldNotContain BazelProjectViewBundle.message("suppress.for.item", "unknown_flag")
    }
  }

  @Test
  fun `should suppress a warning from Inspect Code`() {
    inEditor("not_supported<caret>_section: someValue") {
      codeInsightFixture.suppressInInspectCode(BazelProjectViewBundle.message("suppress.batch.for.section"))

      codeInsightFixture.checkResult("# suppress: unsupported_section\nnot_supported_section: someValue")
    }
  }

  @Test
  fun `should give each suppress action of Inspect Code its own family name for a selection of several problems`() {
    inEditor("not_supported<caret>_section: someValue") {
      codeInsightFixture.inspectCodeSuppressActionsAtCaret().map { it.familyName } shouldContainExactlyInAnyOrder listOf("section").map {
        BazelProjectViewBundle.message("suppress.batch.for.$it")
      }
    }
  }

  @Test
  fun `should not offer suppress actions for an error in Inspect Code`() {
    inEditor("shard_sync: not_a<caret>_boolean") {
      codeInsightFixture.inspectCodeSuppressActionsAtCaret().shouldBeEmpty()
    }
  }

  private fun suppress(before: String, action: String, id: String, after: String) {
    inEditor(before.trimIndent()) {
      val text = BazelProjectViewBundle.message(action, id)
      codeInsightFixture.launchAction(suppressActions().single { it.text == text })
      codeInsightFixture.checkResult(after.trimIndent())
      codeInsightFixture.doHighlighting().filter { it.severity != HighlightSeverity.INFORMATION }.shouldBeEmpty()
    }
  }

  private fun suppressActions(): List<IntentionAction> =
    options().filter { it.isAvailable(codeInsightFixture.project, codeInsightFixture.editor, codeInsightFixture.file) }

  /**
   * The submenu entries of the quick fixes of the only problem at the caret, as the Alt+Enter popup shows them.
   */
  private fun options(): List<IntentionAction> {
    val info = codeInsightFixture.doHighlighting().single { it.severity != HighlightSeverity.INFORMATION }
    val element = checkNotNull(codeInsightFixture.file.findElementAt(codeInsightFixture.caretOffset))
    val options = mutableListOf<IntentionAction>()
    info.findRegisteredQuickFix { descriptor, _ -> options += descriptor.getOptions(element, codeInsightFixture.editor); null }
    return options
  }

  private fun inEditor(text: String, action: () -> Unit) {
    timeoutRunBlocking(30.seconds) {
      withContext(Dispatchers.EDT) {
        codeInsightFixture.configureByText(".bazelproject", text)
        action()
      }
    }
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
