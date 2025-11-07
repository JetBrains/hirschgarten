package org.jetbrains.bazel.flow.open

import com.intellij.configurationStore.ProjectStoreDescriptor
import com.intellij.configurationStore.ProjectStorePathCustomizer
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.getProjectCacheFileName
import com.intellij.openapi.project.projectsDataDir
import org.jetbrains.bazel.commons.constants.Constants
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

private class BazelProjectStorePathCustomizer : ProjectStorePathCustomizer {
  override fun getStoreDirectoryPath(projectRoot: Path): ProjectStoreDescriptor? {
    // do not use isDirectory, as we also want to check that the file exists
    if (!Files.isRegularFile(projectRoot)) return null

    if (projectRoot.name !in Constants.SUPPORTED_CONFIG_FILE_NAMES &&
      !Constants.SUPPORTED_EXTENSIONS.contains(projectRoot.extension)
    ) return null

    // we should use `getProjectCacheFileName` API,
    // as in this dir is located a lot of other project-related data, so, location of dir should be in an expected location
    val cacheDirectoryName = getProjectCacheFileName(projectRoot)
    val dotIdea = projectsDataDir.resolve(cacheDirectoryName).resolve("bazel.idea")
    val workspaceXml =
      PathManager
        .getConfigDir()
        .resolve("workspace")
        .resolve("bazel")
        .resolve("$cacheDirectoryName.xml")
    return BazelProjectStoreDescriptor(
      projectIdentityFile = projectRoot,
      dotIdea = dotIdea,
      historicalProjectBasePath = projectRoot.parent,
      workspaceXml = workspaceXml,
    )
  }
}
