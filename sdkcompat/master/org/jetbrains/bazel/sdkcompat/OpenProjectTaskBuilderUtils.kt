package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.impl.OpenProjectTaskBuilder
import java.nio.file.Path

fun OpenProjectTaskBuilder.setProjectRootDir(projectRootDir: Path) {
  this.projectRootDir = projectRootDir
}

fun OpenProjectTaskBuilder.createModule(create: Boolean) {
  this.createModule = create
}
