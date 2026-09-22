package org.jetbrains.bazel.test.framework.ext

import org.jetbrains.bazel.test.framework.BazelVersionedTest
import org.jetbrains.bazel.test.framework.annotation.EnabledOnBazel
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils

/** Evaluates [EnabledOnBazel] against the Bazel version of the current test instance. */
class EnabledOnBazelCondition : ExecutionCondition {

  override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
    val annotation = AnnotationUtils.findAnnotation(context.element, EnabledOnBazel::class.java).orElse(null)
      ?: return ConditionEvaluationResult.enabled("No @EnabledOnBazel annotation found")

    val testInstance = context.testInstance.orElse(null)
    check(testInstance is BazelVersionedTest) {
      "@EnabledOnBazel requires the test class to implement ${BazelVersionedTest::class.simpleName}"
    }

    return if (testInstance.bazelVersion in annotation.versions) {
      ConditionEvaluationResult.enabled("Test is enabled on Bazel ${testInstance.bazelVersion}")
    } else {
      ConditionEvaluationResult.disabled("Test is only enabled on Bazel ${testInstance.bazelVersion}")
    }
  }
}
