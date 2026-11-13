package org.jetbrains.bazel.run.testResults

import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import org.jetbrains.annotations.ApiStatus

/** The SM runner needs a failure message, and it shows a test with a blank failure message as passed. */
@ApiStatus.Internal
fun testFailed(name: String, message: String?): ServiceMessageBuilder =
  ServiceMessageBuilder.testFailed(name).addAttribute("message", message?.takeIf { it.isNotBlank() } ?: "Failed")
