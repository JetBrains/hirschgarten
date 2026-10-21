package org.jetbrains.bazel.clion.run

import com.intellij.execution.Location
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.cidr.execution.testing.CidrTestLanguageSupport
import com.jetbrains.cidr.execution.testing.CidrTestScopeElement
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelCcTestFrameworkSupport {
  val framework: CidrTestLanguageSupport

  /** Resolves a test path to its declaration or an empty list if this framework does not know the test. */
  fun locate(path: String, project: Project, scope: GlobalSearchScope): List<Location<*>>

  /**
   * The complete `--test_filter` value selecting exactly [tests], or null if no filter can be built.
   *
   * Joining is up to the implementation, since every framework spells a list of tests differently.
   */
  fun createTestFilter(tests: List<CidrTestScopeElement>): String?

  fun getLocationCacheModificationTracker(project: Project): ModificationTracker

  companion object {
    val EP_NAME: ExtensionPointName<BazelCcTestFrameworkSupport> =
      ExtensionPointName.create("org.jetbrains.bazel.ccTestSupport")

    fun findTestAndFramework(element: PsiElement): Pair<CidrTestScopeElement, BazelCcTestFrameworkSupport>? =
      EP_NAME.computeSafeIfAny { support ->
        val testElement = support.framework.findTestObject(element)
        if (testElement != null) {
          testElement to support
        }
        else {
          null
        }
      }
  }
}
