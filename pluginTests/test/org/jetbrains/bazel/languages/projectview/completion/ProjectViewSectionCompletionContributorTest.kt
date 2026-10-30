package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAll
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.projectview.ProjectViewSection
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.junit.jupiter.api.Test

@BazelTestApplication
class ProjectViewSectionCompletionContributorTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should suggest section names in top level`() {
    myFixture.configureByText(".bazelproject", "")

    myFixture.type("a")

    val lookups = myFixture.completeBasicLookupStrings()
    val expected = ProjectViewSection.allRegistered
      .filter { it.key.name.contains("a") }
      .map { it.key.name }
      .toList()
    lookups shouldContainAll expected
  }

  @Test
  fun `should suggest section names in dumb mode`() {
    myFixture.configureByText(".bazelproject", "")
    myFixture.type("a")

    val lookups = myFixture.completeBasicLookupStrings(dumbMode = true)
    lookups shouldContainAll listOf("targets", "shard_sync")
  }

  @Test
  fun `should strike out and deprioritise a deprecated section`() {
    myFixture.configureByText(".bazelproject", "")

    myFixture.type("debug")

    val lookups = myFixture.completeBasic().orEmpty()
    val deprecated = lookups.single { it.lookupString == "python_debug_flags" }
    val replacement = lookups.single { it.lookupString == "debug_flags" }

    LookupElementPresentation.renderElement(deprecated).isStrikeout shouldBe true
    LookupElementPresentation.renderElement(replacement).isStrikeout shouldBe false
    deprecated.priority() shouldBeLessThan replacement.priority()
  }

  private fun LookupElement.priority(): Double = `as`(PrioritizedLookupElement.CLASS_CONDITION_KEY)?.priority ?: 0.0

  @Test
  fun `should not suggest anything if not top level`() {
    myFixture.configureByText(".bazelproject", "targets: <caret>")

    myFixture.type("a")

    val lookups = myFixture.completeBasicLookupStrings()
    lookups shouldNotContainAll ProjectViewSection.allRegistered
      .filter { it.key.name.contains("a") }
      .map { it.key.name }
      .toList()
  }

  @Test
  fun `should not suggest section names inside sections`() {
    myFixture.configureByText(
      ".bazelproject",
      """
      targets:
        //some/target
        <caret>
      """.trimIndent(),
    )

    myFixture.type("a")

    val lookups = myFixture.completeBasicLookupStrings()
    lookups shouldNotContainAll ProjectViewSection.allRegistered
      .filter { it.key.name.contains("a") }
      .map { it.key.name }
      .toList()
  }
}
