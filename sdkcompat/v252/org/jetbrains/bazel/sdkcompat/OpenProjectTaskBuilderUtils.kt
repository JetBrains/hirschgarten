package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.impl.OpenProjectTaskBuilder
import java.nio.file.Path

fun OpenProjectTaskBuilder.setProjectRootDir(projectRootDir: Path?): Unit = Unit

@Suppress("UnusedReceiverParameter", "unused")
fun OpenProjectTaskBuilder.createModule(create: Boolean): Unit = Unit
