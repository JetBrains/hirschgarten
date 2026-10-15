package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorOpenRequest
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.common.waitUntil
import com.jetbrains.cidr.execution.testing.CidrTestLanguageSupport
import com.jetbrains.cidr.execution.testing.CidrTestScopeElement
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestElements
import com.jetbrains.cidr.execution.testing.google.GoogleTestLanguageSupport
import com.jetbrains.cidr.execution.testing.tcatch.CatchTestLanguageSupport
import com.jetbrains.cidr.execution.testing.tcatch.CidrCatchTestElements
import com.jetbrains.cidr.execution.testing.tcatch.CidrCatchTestUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.EXECUTION_TIMEOUT
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.fixtures.executeBazelConfiguration
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.run.BazelTestRerunExtension
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.test.framework.BazelVersions
import org.jetbrains.bazel.ui.gutters.StarlarkRunLineMarkerContributor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.time.Duration.Companion.minutes

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledOnOs(OS.WINDOWS)
class GutterTest {

  private val project by clionBazelProjectFixture("clion/execution", bazelVersion = BazelVersions.BAZEL_9) {
    addBuildFlags("--cxxopt=-std=c++17")
  }

  @BeforeAll
  fun waitForTestIndex(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    LanguageEngine.INSTANCE.waitForTestIndex(project)
  }

  private val markerContributor = StarlarkRunLineMarkerContributor()

  @Test
  fun testGutterPresent(): Unit = timeoutRunBlocking {
    assertGutterPresent(Label.parse("//main:echo0")) // cc_binary
    assertGutterPresent(Label.parse("//main:gtest")) // cc_test
    assertGutterPresent(Label.parse("//main:catch")) // cc_test
  }

  /** Asserts that a run/test gutter icon is contributed for the rule [label] in its BUILD file. */
  private suspend fun assertGutterPresent(label: Label) {
    val info = readAction { markerContributor.getInfo(ruleNameLeaf(findRule(label))) }

    assertThat(info).withFailMessage("no gutter run marker for $label").isNotNull()
    assertThat(info!!.icon).isEqualTo(AllIcons.Actions.Execute)
  }

  /** The leaf holding the rule's function name, where [StarlarkRunLineMarkerContributor] anchors. */
  private fun ruleNameLeaf(funcall: StarlarkCallExpression): LeafPsiElement {
    val leaf = PsiTreeUtil.findChildrenOfType(funcall, LeafPsiElement::class.java)
      .firstOrNull { it.parent is StarlarkReferenceExpression && it.parent.parent === funcall }
    return requireNotNull(leaf) { "rule name leaf not found in $funcall" }
  }

  private fun findRule(label: Label): StarlarkCallExpression {
    val buildFile = findPsiFile("${label.packagePath}/BUILD")
    val rule = PsiTreeUtil.findChildrenOfType(buildFile, StarlarkCallExpression::class.java)
      .firstOrNull { it.getNameAttributeValue() == label.targetName }
    return requireNotNull(rule) { "rule $label not found" }
  }

  @Test
  fun testGoogleTestGutterSelectsItsTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val element = testElement(GoogleTestLanguageSupport.getInstance(), "main/gtest.cc", suite = "FilterSuite", test = "FilteredTest")
    val filter = singleBazelConfigurationAt(element.element, "//main:gtest").testFilter.assertNotNull()

    assertThat(matchesGoogleTestFilter(filter, "FilterSuite.FilteredTest")).isTrue()
    assertThat(matchesGoogleTestFilter(filter, "FilterSuite.SkippedTest")).isFalse()
    assertThat(matchesGoogleTestFilter(filter, "SampleSuite.SampleTest")).isFalse()
  }

  @Test
  fun testGoogleTestGutterSelectsAllTypeInstantiations(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val element = testElement(GoogleTestLanguageSupport.getInstance(), "main/gtest.cc", suite = "TypedTestSuite", test = "SampleTest")
    val filter = singleBazelConfigurationAt(element.element, "//main:gtest").testFilter.assertNotNull()

    assertThat(matchesGoogleTestFilter(filter, "TypedTestSuite/0.SampleTest")).isTrue()
    assertThat(matchesGoogleTestFilter(filter, "TypedTestSuite/1.SampleTest")).isTrue()
    assertThat(matchesGoogleTestFilter(filter, "TypedTestSuite/0.AnotherTest")).isFalse()
  }

  @Test
  fun testCatchGutterSelectsItsTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val element = testElement(CatchTestLanguageSupport.getInstance(), "main/catch.cc", suite = null, test = "FilteredTest")
    val configuration = singleBazelConfigurationAt(element.element, "//main:catch")

    assertThat(configuration.testFilter).isEqualTo("FilteredTest")
  }

  @Test
  fun testGoogleTestResultNavigatesToItsTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertRunFromGutterNavigatesBack(
      GoogleTestLanguageSupport.getInstance(),
      "main/gtest.cc",
      "//main:gtest",
      suite = "FilterSuite",
      test = "FilteredTest",
    )
  }

  @Test
  fun testCatchResultNavigatesToItsTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertRunFromGutterNavigatesBack(
      CatchTestLanguageSupport.getInstance(),
      "main/catch.cc",
      "//main:catch",
      suite = null,
      test = "FilteredTest",
    )
  }

  private suspend fun assertRunFromGutterNavigatesBack(
    framework: CidrTestLanguageSupport,
    relativePath: String,
    label: String,
    suite: String?,
    test: String,
  ) {
    val element = testElement(framework, relativePath, suite, test)
    val configuration = singleBazelConfigurationAt(element.element, label)

    val result = project.executeBazelConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
    result.assertSuccess()

    val leaf = result.leaves.singleOrNull()
    assertThat(leaf).withFailMessage("expected only '$test' in the test tree, but got ${result.testNames}").isNotNull()
    assertThat(leaf!!.name).isEqualTo(test)

    val location = readAction { leaf.getLocation(project, GlobalSearchScope.allScope(project)) }
    val locatedElement = location?.psiElement
    checkNotNull(locatedElement)
    assertThat(readAction { locatedElement.textRange }).isEqualTo(readAction { element.element.textRange })
    assertThat(readAction { locatedElement.containingFile.virtualFile }).isEqualTo(readAction { element.element.containingFile.virtualFile })

    val rerunExtension = BazelTestRerunExtension.getInstance(configuration).assertNotNull()
    assertThat(rerunExtension.isRerunOf(configuration, result.leaves)).isTrue()
  }

  private suspend fun testElement(
    framework: CidrTestLanguageSupport,
    relativePath: String,
    suite: String?,
    test: String,
  ): CidrTestScopeElement {
    val expected = if (framework is GoogleTestLanguageSupport) {
      CidrGoogleTestElements.createTestScopeElementForSuiteAndTest(suite, test)
    }
    else {
      CidrCatchTestElements.createTestScopeElement(suite ?: CidrCatchTestUtil.NULL_TAG_PRESENTATION_NAME, test)
    }
    val psiFile = readAction { findPsiFile(relativePath) }
    var elements: Collection<CidrTestScopeElement> = emptyList()
    lateinit var result: CidrTestScopeElement
    // the backend explores the tests of a file only when the file is open in an editor
    withContext(Dispatchers.EDT) { FileEditorManager.getInstance(project).openFile(psiFile.virtualFile, FileEditorOpenRequest.defaults()) }
    try {
      withContext(Dispatchers.Default) {
        waitUntil({ "the test index reports no '$test' in $relativePath, only ${elements.map { it.testName }}" }, 2.minutes) {
          elements = framework.getElementsInFile(psiFile)
          val foundElement = elements.firstOrNull { it.testName == expected.testName && it.suiteName == expected.suiteName }
          if (foundElement != null) {
            result = foundElement
            true
          }
          else {
            false
          }
        }
      }
    }
    finally {
      withContext(Dispatchers.EDT) { FileEditorManager.getInstance(project).closeFile(psiFile.virtualFile) }
    }
    return result
  }

  private suspend fun singleBazelConfigurationAt(element: PsiElement, label: String): BazelRunConfiguration {
    val configurations = readAction {
      ConfigurationContext.createEmptyContextForLocation(PsiLocation.fromPsiElement(element))
        .configurationsFromContext
        .orEmpty()
        .map { it.configuration }
    }

    assertThat(configurations.map { "${it.name} (${it.javaClass.simpleName})" }).hasSize(1)
    val configuration = configurations.single()
    assertThat(configuration).isInstanceOf(BazelRunConfiguration::class.java)
    configuration as BazelRunConfiguration

    assertThat(configuration.targets).containsExactly(Label.parse(label))
    assertThat(configuration.handler?.isTestHandler).isTrue()
    return configuration
  }

  private fun findPsiFile(relativePath: String): PsiFile {
    val file = project.rootDir.findFileByRelativePath(relativePath).assertNotNull()
    return PsiManager.getInstance(project).findFile(file).assertNotNull()
  }
}

private val BazelRunConfiguration.testFilter: String?
  get() = (handler?.state as? HasTestFilter)?.testFilter
