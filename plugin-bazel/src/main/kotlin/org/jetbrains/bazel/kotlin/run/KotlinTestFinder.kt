package org.jetbrains.bazel.kotlin.run

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.run.BazelTestFinder
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.findFunctionByName
import kotlin.collections.map
import kotlin.collections.toList

class KotlinTestFinder : BazelTestFinder {
  override fun findTestSuite(
    classNameOrSuites: List<String>,
    project: Project,
    scope: GlobalSearchScope,
  ): List<Location<PsiElement>> = findClasses(classNameOrSuites, project, scope).map { PsiLocation(it) }

  override fun findTestCase(
    classNameOrSuites: List<String>,
    methodName: String,
    project: Project,
    scope: GlobalSearchScope,
  ): List<Location<PsiElement>> =
    findClasses(classNameOrSuites, project, scope)
      .mapNotNull { it.findFunctionByName(methodName) }
      .map { PsiLocation(it) }

  private fun findClasses(
    classNameOrSuites: List<String>,
    project: Project,
    scope: GlobalSearchScope,
  ): List<KtClassOrObject> {
    val fullJVMName = classNameOrSuites.joinToString(".").replace('$', '.')
    val byFullName = KotlinFullClassNameIndex[fullJVMName, project, scope].toList()
    return byFullName.ifEmpty {
      val topLevelClasses =
        KotlinClassShortNameIndex[classNameOrSuites.first(), project, scope].toList()
      if (classNameOrSuites.size == 1) {
        topLevelClasses
      } else {
        val theRest = classNameOrSuites.drop(1).joinToString(".")
        topLevelClasses.mapNotNull { it.kotlinFqName }.flatMap {
          val fullName = "$it.$theRest"
          KotlinFullClassNameIndex[fullName, project, scope].toList()
        }
      }
    }
  }
}
