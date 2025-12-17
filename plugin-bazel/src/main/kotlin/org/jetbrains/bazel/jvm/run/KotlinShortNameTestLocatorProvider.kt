package org.jetbrains.bazel.jvm.run

import com.intellij.execution.Location
import com.intellij.execution.testframework.JavaTestLocator
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.NonNls
import org.jetbrains.bazel.run.test.BazelTestLocatorProvider
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex

class KotlinShortNameTestLocatorProvider : BazelTestLocatorProvider {
  override fun getTestLocator(): SMTestLocator = KotlinShortNameTestLocator
}

// Test location hints should be structured as: package.Class$NestedClass/testMethod (or with a dot instead of the $)
// This class is meant to locate Kotlin tests when the package is omitted,
//   and searching by class name only is required.
private object KotlinShortNameTestLocator : SMTestLocator {
  override fun getLocation(
    protocol: @NonNls String,
    path: @NonNls String,
    project: @NonNls Project,
    scope: GlobalSearchScope,
  ): List<Location<*>?> {
    if (protocol != TEST_PROTOCOL && protocol != SUITE_PROTOCOL) {
      return emptyList()
    }
    val defaultJavaResults = JavaTestLocator.INSTANCE.getLocation(protocol, path, project, scope)
    if (defaultJavaResults.isNotEmpty()) {
      return defaultJavaResults
    } else {
      val pathSegments = path.split('/')
      val (className: String, methodName: String) =
        if (protocol == TEST_PROTOCOL && pathSegments.size >= 2) {
          pathSegments.takeLast(2)
        } else if (pathSegments.isNotEmpty()) {
          listOf(pathSegments.last(), "")
        } else {
          return emptyList()
        }
      val cleanClassName = className.substringAfterLast('$')
      val kotlinFqn = findKotlinFqnByShortName(project, scope, cleanClassName, methodName)
      val locationsFound: List<Location<*>?> = invokeDefaultJavaLocator(protocol, kotlinFqn, project, scope)

      return if (locationsFound.isNotEmpty()) {
        locationsFound
      } else if (methodName.getOrNull(0)?.isUpperCase() == true) {
        // Until BAZEL-2051 is done, test locations will not be found for nested classes themselves,
        //   because they are treated as test cases instead of test suites
        // TODO - remove this "if" case once BAZEL-2051 is done
        val nestedClassFqn = kotlinFqn?.replace('/', '$')
        invokeDefaultJavaLocator(SUITE_PROTOCOL, nestedClassFqn, project, scope)
      } else {
        emptyList()
      }
    }
  }
}

private const val TEST_PROTOCOL = "java:test"
private const val SUITE_PROTOCOL = "java:suite"

private fun findKotlinFqnByShortName(project: Project, scope: GlobalSearchScope, className: String, methodName: String = ""): String? {
  val methodNamePostfix = if (methodName.isNotEmpty()) "/$methodName" else ""
  return KotlinClassShortNameIndex[className, project, scope]
    .firstOrNull()
    ?.let { "${it.kotlinFqName}${methodNamePostfix}" }
}

private fun invokeDefaultJavaLocator(protocol: String, fqn: String?, project: Project, scope: GlobalSearchScope): List<Location<*>?> =
  (fqn?.let { JavaTestLocator.INSTANCE.getLocation(protocol, it, project, scope) } ?: emptyList())
