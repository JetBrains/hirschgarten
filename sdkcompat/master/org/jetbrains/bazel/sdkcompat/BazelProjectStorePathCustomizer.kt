package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectStorePathCustomizer
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.isDirectory

internal class BazelProjectStorePathCustomizer : ProjectStorePathCustomizer {
  override fun getStoreDirectoryPath(projectRoot: Path): Path? =
    if (projectRoot.isDirectory()) {
      null
    } else {
      projectRoot.parent /
        Project.DIRECTORY_STORE_FOLDER /
        "${Project.DIRECTORY_STORE_FOLDER}.${projectRoot.fileName}}" /
        Project.DIRECTORY_STORE_FOLDER
    }
}
