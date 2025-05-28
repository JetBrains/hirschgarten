package org.jetbrains.bazel.java.run

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex
import com.intellij.psi.impl.java.stubs.index.JavaShortClassNameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.run.BazelTestFinder

class JavaTestFinder : BazelTestFinder {
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
      .flatMap { it.findMethodsByName(methodName, false).toList() }
      .map { PsiLocation(it) }

  private fun findClasses(
    classNameOrSuites: List<String>,
    project: Project,
    scope: GlobalSearchScope,
  ): List<PsiClass> {
    val fullJVMName = classNameOrSuites.joinToString(".").replace('$', '.')
    val byFullName = JavaFullClassNameIndex.getInstance().getClasses(fullJVMName, project, scope).toList()
    return byFullName.ifEmpty {
      val topLevelClasses =
        JavaShortClassNameIndex.getInstance().getClasses(classNameOrSuites.first(), project, scope).toList()
      if (classNameOrSuites.size == 1) {
        topLevelClasses
      } else {
        val theRest = classNameOrSuites.drop(1).joinToString(".")
        topLevelClasses.mapNotNull { it.qualifiedName }.flatMap {
          val fullName = "$it.$theRest"
          JavaFullClassNameIndex.getInstance().getClasses(fullName, project, scope).toList()
        }
      }
    }
  }
}
