@file:Suppress("ReplacePutWithAssignment")

package org.jetbrains.bazel.sdkcompat

import com.intellij.configurationStore.FileStorageAnnotation
import com.intellij.configurationStore.ProjectStoreDescriptor
import com.intellij.configurationStore.ProjectStorePathCustomizer
import com.intellij.configurationStore.StateStorageManager
import com.intellij.configurationStore.sortStoragesByDeprecated
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.StateSplitterEx
import com.intellij.openapi.components.StateStorageOperation
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectCacheFileName
import com.intellij.openapi.project.projectsDataDir
import com.intellij.util.PathUtilRt
import java.nio.file.Files
import java.nio.file.Path

private class BazelProjectStorePathCustomizer : ProjectStorePathCustomizer {
  override fun getStoreDirectoryPath(projectRoot: Path): ProjectStoreDescriptor? {
    if (Files.isDirectory(projectRoot)) {
      return null
    } else {
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
}

private class BazelProjectStoreDescriptor(
  override val projectIdentityFile: Path,
  override val dotIdea: Path,
  override val historicalProjectBasePath: Path,
  private val workspaceXml: Path,
) : ProjectStoreDescriptor {
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

  override fun customMacros(): Map<String, Path> {
    val rootDotIdea = historicalProjectBasePath.resolve(Project.DIRECTORY_STORE_FOLDER)
    if (Files.notExists(rootDotIdea)) {
      return emptyMap()
    }

    val result = HashMap<String, Path>(shareableConfigFiles.size)
    for (filePath in shareableConfigFiles) {
      result.put(filePath, rootDotIdea.resolve(filePath))
    }
    result.put(StoragePathMacros.PROJECT_FILE, rootDotIdea.resolve("misc.xml"))
    result.put(StoragePathMacros.WORKSPACE_FILE, workspaceXml)
    return result
  }

  override fun getJpsBridgeAwareStorageSpec(filePath: String, project: Project): Storage =
    FileStorageAnnotation(
      // path =
      PathUtilRt.getFileName(filePath),
      // deprecated =
      false,
      // splitterClass =
      StateSplitterEx::class.java,
    )

  override fun getProjectName(): String = projectIdentityFile.fileName.toString()

  override suspend fun saveProjectName(project: Project) {
  }
}

@Suppress("SpellCheckingInspection")
private val shareableConfigFiles =
  listOf(
    "AIAssistantCustomInstructionsStorage.xml",
    "anchors.xml",
    "ant.xml",
    "checker.xml",
    "codeInsightSettings.xml",
    "codeStyleSettings.xml",
    "csv-editor.xml",
    "dependencyResolver.xml",
    "deployment.xml",
    "encodings.xml",
    "excludeFromValidation.xml",
    "externalDependencies.xml",
    "fileColors.xml",
    "gradle.xml",
    "IntelliLang.xml",
    "jarRepositories.xml",
    "jsLibraryMappings.xml",
    "jsonSchemas.xml",
    "kotlinc.xml",
    "kotlinTestDataPluginTestDataPaths.xml",
    "ktfmt.xml",
    "ktor.xml",
    "php.xml",
    "projectDictionary.xml",
    "remote-targets.xml",
    "scala_settings.xml",
    "sqldialects.xml",
    "terraform.xml",
    "uiDesigner.xml",
    "vcs.xml",
    "misc.xml",
    // scheme manager
    "codeStyles",
    "copyright",
    "dictionaries",
    "fileTemplates",
    "inspectionProfiles",
    "scopes",
  )
