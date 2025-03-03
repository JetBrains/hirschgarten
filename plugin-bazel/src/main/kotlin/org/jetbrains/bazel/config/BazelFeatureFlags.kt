package org.jetbrains.bazel.config

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bsp.protocol.FeatureFlags

private const val PYTHON_SUPPORT = "bsp.python.support"
private const val ANDROID_SUPPORT = "bsp.android.support"
private const val GO_SUPPORT = "bsp.go.support"
private const val BUILD_PROJECT_ON_SYNC = "bsp.build.project.on.sync"
private const val SHORTEN_MODULE_LIBRARY_NAMES = "bsp.shorten.module.library.names"
private const val RETRIEVE_TARGETS_FOR_FILE_FROM_ANCESTORS = "bsp.retrieve.targets.for.file.from.ancestors"
private const val WRAP_LIBRARIES_INSIDE_MODULES = "bsp.wrap.libraries.inside.modules"
private const val USE_PHASED_SYNC = "bsp.use.phased.sync"
private const val EXECUTE_SECOND_PHASE_ON_SYNC = "bsp.execute.second.phase.on.sync"
private const val ADD_DUMMY_MODULES = "bsp.add.dummy.modules"
private const val ALLOW_DUMMY_MODULES_AT_PROJECT_ROOT = "bsp.allow.dummy.modules.at.project.root"
private const val EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS = "bsp.exclude.compiled.source.code.inside.jars"
private const val ENABLE_PARTIAL_SYNC = "bsp.enable.partial.sync"
private const val SYMLINK_SCAN_MAX_DEPTH = "bazel.symlink.scan.max.depth"
private const val SHUTDOWN_BEFORE_SHARD_BUILD = "bazel.shutdown.before.shard.build"

object BazelFeatureFlags {
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

  val isPhasedSync: Boolean
    get() = Registry.`is`(USE_PHASED_SYNC)

  val executeSecondPhaseOnSync: Boolean
    get() = isPhasedSync && Registry.`is`(EXECUTE_SECOND_PHASE_ON_SYNC)

  val addDummyModules: Boolean
    get() = Registry.`is`(ADD_DUMMY_MODULES)

  val allowDummyModulesAtRootDir: Boolean
    get() = Registry.`is`(ALLOW_DUMMY_MODULES_AT_PROJECT_ROOT)

  val excludeCompiledSourceCodeInsideJars: Boolean
    get() = Registry.`is`(EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS)

  val enablePartialSync: Boolean
    get() = Registry.`is`(ENABLE_PARTIAL_SYNC)

  val symlinkScanMaxDepth: Int
    get() = Registry.intValue(SYMLINK_SCAN_MAX_DEPTH)

  val shutDownBeforeShardBuild: Boolean
    get() = Registry.`is`(SHUTDOWN_BEFORE_SHARD_BUILD)
}

object FeatureFlagsProvider {
  fun getFeatureFlags(): FeatureFlags =
    with(BazelFeatureFlags) {
      FeatureFlags(
        isPythonSupportEnabled = isPythonSupportEnabled,
        isAndroidSupportEnabled = isAndroidSupportEnabled,
        isGoSupportEnabled = isGoSupportEnabled,
        isRustSupportEnabled = false, // No corresponding registry key for now
        isPropagateExportsFromDepsEnabled = !isWrapLibrariesInsideModulesEnabled,
        bazelSymlinksScanMaxDepth = symlinkScanMaxDepth,
        bazelShutDownBeforeShardBuild = shutDownBeforeShardBuild,
      )
    }
}
