package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.OpenProjectTaskBuilder

@Suppress("FunctionName")
internal fun OpenProjectTaskCompat(buildAction: OpenProjectTaskBuilder.() -> Unit): OpenProjectTask {
  return OpenProjectTaskCompat.invoke(buildAction)
}
