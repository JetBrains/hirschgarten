package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.util.registry.Registry

private const val PYTHON_SUPPORT = "bsp.python.support"
private const val ANDROID_SUPPORT = "bsp.android.support"
private const val GO_SUPPORT = "bsp.go.support"
private const val BUILD_PROJECT_ON_SYNC = "bsp.build.project.on.sync"
private const val SHORTEN_MODULE_LIBRARY_NAMES = "bsp.shorten.module.library.names"
private const val RETRIEVE_TARGETS_FOR_FILE_FROM_ANCESTORS = "bsp.retrieve.targets.for.file.from.ancestors"
private const val WRAP_LIBRARIES_INSIDE_MODULES = "bsp.wrap.libraries.inside.modules"
private const val SCAN_GIT_ROOTS_INSIDE_PROJECT_DIR = "bsp.scan.git.roots.inside.project.dir"

object BspFeatureFlags {
  val isPythonSupportEnabled: Boolean
    get() = Registry.`is`(PYTHON_SUPPORT)

  val isAndroidSupportEnabled: Boolean
    get() = Registry.`is`(ANDROID_SUPPORT)

  val isGoSupportEnabled: Boolean
    get() = Registry.`is`(GO_SUPPORT)

  val isBuildProjectOnSyncEnabled: Boolean
    get() = Registry.`is`(BUILD_PROJECT_ON_SYNC)

  val isShortenModuleLibraryNamesEnabled: Boolean
    get() = Registry.`is`(SHORTEN_MODULE_LIBRARY_NAMES)

  val isRetrieveTargetsForFileFromAncestorsEnabled: Boolean
    get() = Registry.`is`(RETRIEVE_TARGETS_FOR_FILE_FROM_ANCESTORS)

  val isWrapLibrariesInsideModulesEnabled: Boolean
    get() = Registry.`is`(WRAP_LIBRARIES_INSIDE_MODULES) || isKotlinPluginK2Mode

  val isKotlinPluginK2Mode: Boolean
    get() = System.getProperty("idea.kotlin.plugin.use.k2", "false").toBoolean()

  val isScanGitRootsInsideProjectDir: Boolean
    get() = Registry.`is`(SCAN_GIT_ROOTS_INSIDE_PROJECT_DIR)
}
