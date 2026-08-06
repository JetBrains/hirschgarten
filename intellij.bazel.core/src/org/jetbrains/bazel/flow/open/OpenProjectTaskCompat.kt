package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.OpenProjectTaskBuilder

/**
 * Usage of [OpenProjectTask] or [OpenProjectTask.copy] leads to compatibility issues.
 * - [OpenProjectTask] - due to inlining
 * - [OpenProjectTask.copy] - due to default arguments
 */
@Suppress("FunctionName")
internal fun OpenProjectTaskCompat(buildAction: OpenProjectTaskBuilder.() -> Unit): OpenProjectTask =
  OpenProjectTaskCompat.build(buildAction)
