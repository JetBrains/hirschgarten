package org.jetbrains.bazel.flow.open

import com.intellij.configurationStore.FileStorageAnnotation
import com.intellij.configurationStore.ProjectStoreDescriptor
import com.intellij.configurationStore.StateStorageManager
import com.intellij.configurationStore.sortStoragesByDeprecated
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.StateSplitterEx
import com.intellij.openapi.components.StateStorageOperation
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.PathUtilRt
import org.jetbrains.bazel.commons.constants.Constants
import java.nio.file.Files
import java.nio.file.Path

internal class BazelProjectStoreDescriptor(
  override val projectIdentityFile: Path,
  override val dotIdea: Path,
  override val historicalProjectBasePath: Path,
  private val workspaceXml: Path,
) : ProjectStoreDescriptor {
  // https://youtrack.jetbrains.com/issue/BAZEL-2527 - we may have a fake module, that still a persistent module
  override fun getModuleStorageSpecs(
    component: PersistentStateComponent<*>,
    stateSpec: State,
    operation: StateStorageOperation,
    storageManager: StateStorageManager,
    project: Project,
  ): List<Storage> = listOf(FileStorageAnnotation.MODULE_FILE_STORAGE_ANNOTATION)

  override fun <T : Any> getStorageSpecs(
    component: PersistentStateComponent<T>,
    stateSpec: State,
    operation: StateStorageOperation,
    storageManager: StateStorageManager,
  ): List<Storage> {
    val storages = stateSpec.storages
    if (storages.isEmpty()) {
      return listOf(FileStorageAnnotation.PROJECT_FILE_STORAGE_ANNOTATION)
    } else {
      return sortStoragesByDeprecated(storages.asList())
    }
  }

  override fun removeProjectConfigurationAndCaches() {
    super.removeProjectConfigurationAndCaches()
    Files.deleteIfExists(workspaceXml)
  }

  override fun testStoreDirectoryExistsForProjectRoot() = Files.isRegularFile(projectIdentityFile)

  override fun getJpsBridgeAwareStorageSpec(filePath: String, project: Project): Storage =
    FileStorageAnnotation(
      // path =
      PathUtilRt.getFileName(filePath),
      // deprecated =
      false,
      // splitterClass =
      StateSplitterEx::class.java,
    )

  override val projectName: @NlsSafe String
    get() {
      val fileName = projectIdentityFile.fileName.toString()

      val projectViewName =
        if (fileName.endsWith(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME) &&
          fileName != Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME
        ) {
          fileName
            .removeSuffix(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)
            .ifEmpty { null }
        } else {
          null
        }

      val projectName = super.projectName
      return if (projectViewName != null && projectViewName != projectName) {
        "$projectName ($projectViewName)"
      } else {
        projectName
      }
    }
}
