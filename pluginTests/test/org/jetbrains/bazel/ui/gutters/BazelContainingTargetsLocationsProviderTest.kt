package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.PsiLocation
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiElement
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.util.ThreeState
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.jvm.JAVA_TEST_KIND
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.buildTarget
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelContainingTargetsLocationsProviderTest {
  private val myFixture by bazelCodeInsightFixture()

  private val library = javaLibrary("//lib:lib") { srcs("Shared.java") }
  private val first = dependentTest("//lib:First")
  private val second = dependentTest("//lib:Second")
  private val third = dependentTest("//lib:Third")

  @Test
  fun `should keep a single dependent target that does not match`() {
    useMatchers(matcher(first.id to ThreeState.NO))

    gutterTargets(library, first) shouldContainExactlyInAnyOrder listOf(library.id, first.id)
  }

  @Test
  fun `should keep all dependent targets when no matcher knows them`() {
    useMatchers(matcher())

    gutterTargets(library, first, second) shouldContainExactlyInAnyOrder listOf(library.id, first.id, second.id)
  }

  @Test
  fun `should keep all dependent targets when no target matches`() {
    useMatchers(matcher(first.id to ThreeState.NO, second.id to ThreeState.NO))

    gutterTargets(library, first, second) shouldContainExactlyInAnyOrder listOf(library.id, first.id, second.id)
  }

  @Test
  fun `should hide a target that does not match and keep a target with an unknown entry point`() {
    useMatchers(matcher(first.id to ThreeState.YES, second.id to ThreeState.NO))

    gutterTargets(library, first, second, third) shouldContainExactlyInAnyOrder listOf(library.id, first.id, third.id)
  }

  @Test
  fun `should not change the result of a matcher when another matcher is unsure`() {
    useMatchers(
      matcher(first.id to ThreeState.YES, second.id to ThreeState.NO),
      matcher(first.id to ThreeState.UNSURE, second.id to ThreeState.UNSURE),
    )

    gutterTargets(library, first, second) shouldContainExactlyInAnyOrder listOf(library.id, first.id)
  }

  @Test
  fun `should keep a target when the matchers disagree about it`() {
    useMatchers(
      matcher(first.id to ThreeState.YES, second.id to ThreeState.NO, third.id to ThreeState.YES),
      matcher(first.id to ThreeState.NO),
    )

    gutterTargets(library, first, second, third) shouldContainExactlyInAnyOrder listOf(library.id, first.id, third.id)
  }

  @Test
  fun `should keep a target that contains the file`() {
    val containing = buildTarget("//lib:Containing", JAVA_TEST_KIND) { srcs("Shared.java") }
    val otherContaining = buildTarget("//lib:OtherContaining", JAVA_TEST_KIND) { srcs("Shared.java") }
    useMatchers(matcher(containing.id to ThreeState.YES, otherContaining.id to ThreeState.NO))

    gutterTargets(containing, otherContaining) shouldContainExactlyInAnyOrder listOf(containing.id, otherContaining.id)
  }

  private fun dependentTest(label: String): TestBuildTarget = buildTarget(label, JAVA_TEST_KIND) { deps("//lib:lib") }

  private fun matcher(vararg matches: Pair<Label, ThreeState>): BazelEntryPointMatcher =
    object : BazelEntryPointMatcher {
      override fun match(element: PsiElement, targets: List<BuildTarget>): Map<Label, ThreeState> = matches.toMap()
    }

  private fun useMatchers(vararg matchers: BazelEntryPointMatcher) {
    ExtensionTestUtil.maskExtensions(BazelEntryPointMatcher.ep, matchers.toList(), myFixture.testBazel.disposable)
  }

  /** Returns the targets that the run gutter offers for an element in `Shared.java`. */
  private fun gutterTargets(vararg targets: TestBuildTarget): List<Label> {
    val file = myFixture.addFileToProject("Shared.java", "public class Shared {}")
    myFixture.testBazel.sync { targets(*targets) }
    val locations = runReadActionBlocking {
      BazelContainingTargetsLocationsProvider().getAlternativeLocations(PsiLocation(file))
    }
    return locations.filterIsInstance<BazelRunLocation>().map { it.target.id }
  }
}
