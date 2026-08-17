package org.jetbrains.bazel.fus

import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomValidationRule
import com.jetbrains.fus.reporting.api.IEventContext
import com.jetbrains.fus.reporting.api.ValidationResultType
import org.jetbrains.bazel.commons.LanguageClassService

/**
 * Validates the `language` field of [BazelSyncCollector] events against the set of [org.jetbrains.bazel.commons.LanguageClass]
 * names known to the IDE (built-in ones plus any contributed by plugins), so plugin-provided languages are accepted while
 * arbitrary strings are rejected.
 */
internal class BazelSyncLanguageValidationRule : CustomValidationRule() {
  override fun getRuleId(): String = "bazel_language"

  override fun doValidate(data: String, context: IEventContext): ValidationResultType =
    if (LanguageClassService.getInstance().fromName(data) != null) ValidationResultType.ACCEPTED
    else ValidationResultType.REJECTED
}
