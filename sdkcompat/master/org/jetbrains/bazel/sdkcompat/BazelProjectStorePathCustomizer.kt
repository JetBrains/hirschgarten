@file:Suppress("ReplacePutWithAssignment")

package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectStorePathCustomizer
import com.intellij.openapi.project.getProjectCacheFileName
import com.intellij.openapi.project.projectsDataDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.isDirectory

private val shareableConfigFiles =
  listOf(
    "AIAssistantCustomInstructionsStorage.xml",
    "anchors.xml",
    "ant.xml",
    "checker.xml",
    "codeInsightSettings.xml",
    "codeStyleSettings.xml",
    "compiler.xml",
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

internal class BazelProjectStorePathCustomizer : ProjectStorePathCustomizer {
  override fun getStoreDirectoryPath(projectRoot: Path): ProjectStorePathCustomizer.StoreDescriptor? {
    if (projectRoot.isDirectory()) {
      return null
    } else {
      val historicalProjectBasePath = projectRoot.parent
      // we should use `getProjectCacheFileName` API,
      // as in this dir is located a lot of other project-related data, so, location of dir should be in an expected location
      val cacheDirectoryName = getProjectCacheFileName(projectRoot)
      val dotIdea = projectsDataDir.resolve(cacheDirectoryName).resolve("bazel.idea")
      return object : ProjectStorePathCustomizer.StoreDescriptor(
        projectIdentityDir = projectRoot,
        dotIdea = dotIdea,
        historicalProjectBasePath = historicalProjectBasePath,
      ) {
        override fun customMacros(): Map<String, Path> {
          val rootDotIdea = historicalProjectBasePath.resolve(Project.DIRECTORY_STORE_FOLDER)
          if (Files.notExists(rootDotIdea)) {
            return emptyMap()
          }

          val result = HashMap<String, Path>(shareableConfigFiles.size)
          for (filePath in shareableConfigFiles) {
            result.put(filePath, rootDotIdea.resolve(filePath))
          }
          result.put($$"$PROJECT_FILE$", rootDotIdea.resolve("misc.xml"))
          return result
        }
      }
    }
  }
}
