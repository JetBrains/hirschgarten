package org.jetbrains.bazel.run

import com.intellij.execution.Location
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.testing.BazelTestLocationHintProvider
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test

class BazelTestFinderTest : MockProjectBaseTest() {
  private val scope = EverythingGlobalScope()
  private val unknownProtocol = "java:test"

  private val parentSuites = listOf("SuiteA", "SuiteB")
  private val suiteHint = BazelTestLocationHintProvider.testSuiteLocationHint("SuiteC", parentSuites = parentSuites)
  private val suiteHintSuites = BazelTestLocationHintProvider.parseLocationHint(suiteHint).classNameOrSuites
  private val caseHint = BazelTestLocationHintProvider.testCaseLocationHint("Test", parentSuites = parentSuites)
  private val caseHintSuites = BazelTestLocationHintProvider.parseLocationHint(caseHint).classNameOrSuites

  @Test
  fun `find test suites`() {
    registerFinders()
    val found = locateAndExtract(suiteHint)
    val expected = suiteHintSuites.flatMap { listOf("${it}1", "${it}2") }
    found shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `find test cases`() {
    registerFinders()
    val found = locateAndExtract(caseHint)
    val expected = caseHintSuites.flatMap { listOf("${it}1", "${it}2") }
    found shouldContainExactlyInAnyOrder expected
  }

  @Test
  fun `ignore non-bazel protocols`() {
    registerFinders()
    val modifiedHint = unknownProtocol + "://" + caseHint.substringAfter("://")
    val found = locateAndExtract(modifiedHint)
    found.shouldBeEmpty()
  }

  private fun registerFinders() {
    BazelTestFinder.EP.registerExtension(Finder(1))
    BazelTestFinder.EP.registerExtension(Finder(2))
  }

  private fun locateAndExtract(locationHint: String): List<String> {
    val protocol = locationHint.substringBefore("://")
    val path = locationHint.substringAfter("://")
    val locations = BazelTestFinder.Locator().getLocation(protocol, path, project, scope)
    return locations.mapNotNull { (it as? MockLocation)?.text }
  }
}

private class Finder(private val id: Int) : BazelTestFinder {
  override fun findTestSuite(
    classNameOrSuites: List<String>,
    project: Project,
    scope: GlobalSearchScope,
  ): List<Location<PsiElement>> = classNameOrSuites.map { MockLocation("$it$id") }

  override fun findTestCase(
    classNameOrSuites: List<String>,
    methodName: String,
    project: Project,
    scope: GlobalSearchScope,
  ): List<Location<PsiElement>> = classNameOrSuites.map { MockLocation("$it$id") }
}

private class MockLocation(val text: String) : Location<PsiElement>() {
  override fun getPsiElement(): Nothing = error("Not implemented in this mock")

  override fun getProject(): Nothing = error("Not implemented in this mock")

  override fun <T : PsiElement?> getAncestors(ancestorClass: Class<T?>?, strict: Boolean): Nothing = error("Not implemented in this mock")

  override fun getModule(): Nothing = error("Not implemented in this mock")
}
