package org.jetbrains.bazel.test.framework.ext

import org.jetbrains.bazel.test.compat.PluginTestsCompat
import org.jetbrains.bazel.test.framework.annotation.OnlyHirschgertenTest
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils

class HirschgartenOnlyTestCondition : ExecutionCondition {
  override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
    val annotation = AnnotationUtils.findAnnotation(context.element, OnlyHirschgertenTest::class.java)
      .orElse(null)
    return if (annotation == null) {
      ConditionEvaluationResult.enabled("No @OnlyHirschgertenTest annotation found")
    } else {
      return if (PluginTestsCompat.isHirschgarten) {
        ConditionEvaluationResult.enabled("Test is not run in a hirschgarten environment")
      } else {
        ConditionEvaluationResult.disabled("Test is only run in a hirschgarten environment")
      }
    }
  }
}
