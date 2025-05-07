package org.jetbrains.bazel.config

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bsp.protocol.FeatureFlags

private const val PYTHON_SUPPORT_BSP = "bsp.python.support"
private const val PYTHON_SUPPORT = "bazel.python.support"
private const val ANDROID_SUPPORT_BSP = "bsp.android.support"
private const val ANDROID_SUPPORT = "bazel.android.support"
private const val GO_SUPPORT_BSP = "bsp.go.support"
private const val GO_SUPPORT = "bazel.go.support"
private const val BUILD_PROJECT_ON_SYNC_BSP = "bsp.build.project.on.sync"
private const val BUILD_PROJECT_ON_SYNC = "bazel.build.project.on.sync"
private const val SHORTEN_MODULE_LIBRARY_NAMES_BSP = "bsp.shorten.module.library.names"
private const val SHORTEN_MODULE_LIBRARY_NAMES = "bazel.shorten.module.library.names"
private const val WRAP_LIBRARIES_INSIDE_MODULES_BSP = "bsp.wrap.libraries.inside.modules"
private const val WRAP_LIBRARIES_INSIDE_MODULES = "bazel.wrap.libraries.inside.modules"
private const val EXECUTE_SECOND_PHASE_ON_SYNC_BSP = "bsp.execute.second.phase.on.sync"
private const val EXECUTE_SECOND_PHASE_ON_SYNC = "bazel.execute.second.phase.on.sync"
private const val ADD_DUMMY_MODULES_BSP = "bsp.add.dummy.modules"
private const val ADD_DUMMY_MODULES = "bazel.add.dummy.modules"
private const val EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS_BSP = "bsp.exclude.compiled.source.code.inside.jars"
private const val EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS = "bazel.exclude.compiled.source.code.inside.jars"
private const val ENABLE_PARTIAL_SYNC_BSP = "bsp.enable.partial.sync"
private const val ENABLE_PARTIAL_SYNC = "bazel.enable.partial.sync"
private const val SYMLINK_SCAN_MAX_DEPTH = "bazel.symlink.scan.max.depth"
private const val SHUTDOWN_BEFORE_SHARD_BUILD = "bazel.shutdown.before.shard.build"
private const val ENABLE_BAZEL_JAVA_CLASS_FINDER = "bazel.enable.custom.java.class.finder"
private const val MERGE_SOURCE_ROOTS = "bazel.merge.source.roots"

object BazelFeatureFlags {
  val isPythonSupportEnabled: Boolean
    get() = Registry.`is`(PYTHON_SUPPORT) || Registry.`is`(PYTHON_SUPPORT_BSP)

  val isAndroidSupportEnabled: Boolean
    get() = Registry.`is`(ANDROID_SUPPORT) || Registry.`is`(ANDROID_SUPPORT_BSP)

  val isGoSupportEnabled: Boolean
    get() = Registry.`is`(GO_SUPPORT) || Registry.`is`(GO_SUPPORT_BSP)

  val isBuildProjectOnSyncEnabled: Boolean
    get() = Registry.`is`(BUILD_PROJECT_ON_SYNC) || Registry.`is`(BUILD_PROJECT_ON_SYNC_BSP)

  val isShortenModuleLibraryNamesEnabled: Boolean
    get() = Registry.`is`(SHORTEN_MODULE_LIBRARY_NAMES) || Registry.`is`(SHORTEN_MODULE_LIBRARY_NAMES_BSP)

  val isWrapLibrariesInsideModulesEnabled: Boolean
    get() = Registry.`is`(WRAP_LIBRARIES_INSIDE_MODULES) || Registry.`is`(WRAP_LIBRARIES_INSIDE_MODULES_BSP) || isKotlinPluginK2Mode

  val isKotlinPluginK2Mode: Boolean
    get() = System.getProperty("idea.kotlin.plugin.use.k2", "false").toBoolean()

  val executeSecondPhaseOnSync: Boolean
    get() = Registry.`is`(EXECUTE_SECOND_PHASE_ON_SYNC) || Registry.`is`(EXECUTE_SECOND_PHASE_ON_SYNC_BSP)

  val addDummyModules: Boolean
    get() = (Registry.`is`(ADD_DUMMY_MODULES) || Registry.`is`(ADD_DUMMY_MODULES_BSP)) && !enableBazelJavaClassFinder

  val excludeCompiledSourceCodeInsideJars: Boolean
    get() = Registry.`is`(EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS) || Registry.`is`(EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS_BSP)

  val enablePartialSync: Boolean
    get() = Registry.`is`(ENABLE_PARTIAL_SYNC) || Registry.`is`(ENABLE_PARTIAL_SYNC_BSP)

  val symlinkScanMaxDepth: Int
    get() = Registry.intValue(SYMLINK_SCAN_MAX_DEPTH)

  val shutDownBeforeShardBuild: Boolean
    get() = Registry.`is`(SHUTDOWN_BEFORE_SHARD_BUILD)

  val enableBazelJavaClassFinder: Boolean
    get() = Registry.`is`(ENABLE_BAZEL_JAVA_CLASS_FINDER)

  val mergeSourceRoots: Boolean
    get() = Registry.`is`(MERGE_SOURCE_ROOTS)
}

object FeatureFlagsProvider {
  fun getFeatureFlags(): FeatureFlags =
    with(BazelFeatureFlags) {
      FeatureFlags(
        isPythonSupportEnabled = isPythonSupportEnabled,
        isAndroidSupportEnabled = isAndroidSupportEnabled,
        isGoSupportEnabled = isGoSupportEnabled,
        isPropagateExportsFromDepsEnabled = !isWrapLibrariesInsideModulesEnabled,
        bazelSymlinksScanMaxDepth = symlinkScanMaxDepth,
        bazelShutDownBeforeShardBuild = shutDownBeforeShardBuild,
      )
    }
}
