package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor
import com.intellij.navigation.PsiElementNavigationItem
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.repomapping.injectCanonicalRepoNameToPath
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@TestApplication
internal class StarlarkTargetRuleLookupTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture
  private val projectFixture = projectFixture(pathFixture = tempDirFixture, openAfterCreation = true)
  private val project by projectFixture
  private val moduleFixture = projectFixture.moduleFixture()
  private val codeInsightFixture by codeInsightFixture(projectFixture, tempDirFixture)

  @BeforeEach
  fun setUp() {
    initializeBazelProject(project, tempDir)
    project.injectCanonicalRepoNameToPath(mapOf("" to tempDir))
  }

  @Test
  fun `lookup keeps the first rule with a duplicate name and misses absent names`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val file = codeInsightFixture.addFileToProject(
        "BUILD",
        """
        first_rule(name = "same")
        second_rule(name = "same")
        other_rule(name = "other")
        ignored_rule(name = variable)
        """.trimIndent(),
      ) as StarlarkFile

      assertSame(file.getTargetRules().first(), file.findTargetRule("same"))
      assertEquals("other", file.findTargetRule("other")?.getNameAttributeValue())
      assertNull(file.findTargetRule("missing"))
      assertNull(file.findTargetRule("variable"))
    }
  }

  @Test
  fun `lookup finds targets in a large build file`(): Unit = timeoutRunBlocking(timeout = 30.seconds) {
    withContext(Dispatchers.EDT) {
      val contents = (0 until 1_500).joinToString("\n") { "rule(name = \"target_$it\")" }
      val file = codeInsightFixture.addFileToProject("BUILD", contents) as StarlarkFile

      for (index in 0 until 1_500) {
        assertEquals("target_$index", file.findTargetRule("target_$index")?.getNameAttributeValue())
      }
      assertNull(file.findTargetRule("target_1500"))
    }
  }

  @Test
  fun `lookup refreshes after a rule name edit`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val file = codeInsightFixture.addFileToProject("BUILD", "rule(name = \"old\")") as StarlarkFile
      val rule = requireNotNull(file.findTargetRule("old"))

      WriteCommandAction.runWriteCommandAction(project) {
        rule.setName("new")
      }

      assertNull(file.findTargetRule("old"))
      assertEquals("new", file.findTargetRule("new")?.getNameAttributeValue())
    }
  }

  @Test
  @Suppress("DEPRECATION", "UNCHECKED_CAST")
  fun `search stops after the consumer rejects the first result`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      codeInsightFixture.addFileToProject(
        "BUILD",
        """
        rule(name = "first_target")
        rule(name = "last_target")
        """.trimIndent(),
      )
    }
    val names = listOf("first_target", "macro_target", "last_target")
    project.targetStorage.setTargets(names.map(::buildTarget))

    val contributor = searchContributor()
    val found = mutableListOf<StarlarkCallExpression>()
    contributor.fetchWeightedElements("target", EmptyProgressIndicator()) { descriptor ->
      val element = (descriptor.item as PsiElementNavigationItem).targetElement
      found.add(element as StarlarkCallExpression)
      false
    }

    assertEquals(1, found.size)
    withContext(Dispatchers.EDT) {
      assertEquals("first_target", found.single().getNameAttributeValue())
    }
  }

  @Test
  @Suppress("DEPRECATION", "UNCHECKED_CAST")
  fun `search keeps the build file fallback for a macro target`(): Unit = timeoutRunBlocking {
    val file = withContext(Dispatchers.EDT) {
      codeInsightFixture.addFileToProject("BUILD", "macro(name = \"other_target\")") as StarlarkFile
    }
    project.targetStorage.setTargets(listOf(buildTarget("macro_target")))

    val found = mutableListOf<Any>()
    searchContributor().fetchWeightedElements("macro_target", EmptyProgressIndicator()) { descriptor ->
      found.add(requireNotNull((descriptor.item as PsiElementNavigationItem).targetElement))
    }

    assertEquals(1, found.size)
    withContext(Dispatchers.EDT) {
      assertSame(file, found.single())
    }
  }

  @Suppress("DEPRECATION", "UNCHECKED_CAST")
  private fun searchContributor(): WeightedSearchEverywhereContributor<Any> {
    val event = TestActionEvent.createTestEvent(
      SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build(),
    )
    val factory = SearchEverywhereContributor.EP_NAME.extensionList.single {
      it.javaClass.name == "org.jetbrains.bazel.ui.widgets.LabelSearchEverywhereContributor\$Factory"
    }
    return factory.createContributor(event) as WeightedSearchEverywhereContributor<Any>
  }

  private fun buildTarget(name: String): TestBuildTarget = TestBuildTarget(
    key = WorkspaceTargetKey(label = Label.parse("//:$name")),
    baseDirectory = tempDir,
  )
}
