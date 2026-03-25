package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.OpenProjectTaskBuilder

// BAZEL-3057: don't call OpenProjectTask directly to avoid 2026.1 API breakage in inlined function
@Suppress("FunctionName")
internal fun OpenProjectTaskCompat(buildAction: OpenProjectTaskBuilder.() -> Unit): OpenProjectTask {
  val openProjectTaskKt = Class.forName("com.intellij.ide.impl.OpenProjectTaskKt")
  val openProjectTaskFunction = openProjectTaskKt.getMethod("OpenProjectTask", kotlin.jvm.functions.Function1::class.java)
  return openProjectTaskFunction.invoke(null, buildAction) as OpenProjectTask
}
