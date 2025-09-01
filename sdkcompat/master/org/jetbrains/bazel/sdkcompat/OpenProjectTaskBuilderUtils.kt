package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.impl.OpenProjectTaskBuilder

fun OpenProjectTaskBuilder.createModule(create: Boolean) {
  this.createModule = create
}
