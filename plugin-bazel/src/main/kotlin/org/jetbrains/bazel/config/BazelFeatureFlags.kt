package org.jetbrains.bazel.config

import com.intellij.codeInsight.multiverse.CodeInsightContextManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.PlatformUtils
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bsp.protocol.FeatureFlags

object BazelFeatureFlags {
  private const val PYTHON_SUPPORT = "bsp.python.support"
  private const val ANDROID_SUPPORT = "bsp.android.support"
  private const val GO_SUPPORT = "bsp.go.support"
  private const val QUERY_TERMINAL_COMPLETION = "bazel.query.terminal.completion"

  @VisibleForTesting
  const val BUILD_PROJECT_ON_SYNC = "bsp.build.project.on.sync"
  private const val SHORTEN_MODULE_LIBRARY_NAMES = "bsp.shorten.module.library.names"
  private const val WRAP_LIBRARIES_INSIDE_MODULES = "bsp.wrap.libraries.inside.modules"
  private const val EXECUTE_SECOND_PHASE_ON_SYNC = "bsp.execute.second.phase.on.sync"
  private const val ADD_DUMMY_MODULE_DEPENDENCIES = "bsp.add.dummy.module.dependencies"
  private const val EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS = "bsp.exclude.compiled.source.code.inside.jars"
  private const val ENABLE_PARTIAL_SYNC = "bsp.enable.partial.sync"
  private const val SYMLINK_SCAN_MAX_DEPTH = "bazel.symlink.scan.max.depth"
  private const val SHUTDOWN_BEFORE_SHARD_BUILD = "bazel.shutdown.before.shard.build"
  private const val ENABLE_BAZEL_JAVA_CLASS_FINDER = "bazel.enable.custom.java.class.finder"
  private const val MERGE_SOURCE_ROOTS = "bazel.merge.source.roots"

  @VisibleForTesting
  const val FAST_BUILD_ENABLED = "bazel.enable.jvm.fastbuild"
  private const val CHECK_SHARED_SOURCES = "bazel.check.shared.sources"
  private const val AUTO_OPEN_PROJECT_IF_PRESENT = "bazel.project.auto.open.if.present"
  private const val ENABLE_BAZEL_QUERY_TAB = "bazel.query.tab.enabled"

  val isPythonSupportEnabled: Boolean
    get() = isEnabled(PYTHON_SUPPORT)

  val isAndroidSupportEnabled: Boolean
    get() = isEnabled(ANDROID_SUPPORT)

  val isGoSupportEnabled: Boolean
    get() = isEnabled(GO_SUPPORT)

  val isBuildProjectOnSyncEnabled: Boolean
    get() = isEnabled(BUILD_PROJECT_ON_SYNC)

  val isShortenModuleLibraryNamesEnabled: Boolean
    get() = isEnabled(SHORTEN_MODULE_LIBRARY_NAMES)

  val isWrapLibrariesInsideModulesEnabled: Boolean
    get() = isEnabled(WRAP_LIBRARIES_INSIDE_MODULES) || isKotlinPluginK2Mode

  val isKotlinPluginK2Mode: Boolean
    get() = System.getProperty("idea.kotlin.plugin.use.k2", "false").toBoolean()

  val executeSecondPhaseOnSync: Boolean
    get() = isEnabled(EXECUTE_SECOND_PHASE_ON_SYNC)

  val addDummyModuleDependencies: Boolean
    get() =
      (Registry.stringValue(ADD_DUMMY_MODULE_DEPENDENCIES).toBooleanStrictOrNull() ?: !fbsrSupportedInPlatform) &&
        !enableBazelJavaClassFinder

  // File-based source root problems fixed here: https://youtrack.jetbrains.com/issue/IDEA-371097
  val fbsrSupportedInPlatform: Boolean = org.jetbrains.bazel.sdkcompat.fbsrSupportedInPlatform

  val excludeCompiledSourceCodeInsideJars: Boolean
    get() = isEnabled(EXCLUDE_COMPILED_SOURCE_CODE_INSIDE_JARS)

  val enablePartialSync: Boolean
    get() = isEnabled(ENABLE_PARTIAL_SYNC)

  val symlinkScanMaxDepth: Int
    get() = Registry.intValue(SYMLINK_SCAN_MAX_DEPTH)

  val shutDownBeforeShardBuild: Boolean
    get() = isEnabled(SHUTDOWN_BEFORE_SHARD_BUILD)

  val enableBazelJavaClassFinder: Boolean
    get() = isEnabled(ENABLE_BAZEL_JAVA_CLASS_FINDER)

  val mergeSourceRoots: Boolean
    get() = isEnabled(MERGE_SOURCE_ROOTS)

  val fastBuildEnabled: Boolean
    get() = isEnabled(FAST_BUILD_ENABLED)

  val checkSharedSources: Boolean
    get() = isEnabled(CHECK_SHARED_SOURCES)

  val autoOpenProjectIfPresent: Boolean
    get() = isEnabled(AUTO_OPEN_PROJECT_IF_PRESENT) || ApplicationManager.getApplication().isHeadlessEnvironment && !PlatformUtils.isFleetBackend()

  val isQueryTerminalCompletionEnabled: Boolean
    get() = isEnabled(QUERY_TERMINAL_COMPLETION)

  val isBazelQueryTabEnabled: Boolean
    get() = isEnabled(ENABLE_BAZEL_QUERY_TAB)

  private fun isEnabled(key: String): Boolean = Registry.`is`(key) || System.getProperty(key, "false").toBoolean()
}

object FeatureFlagsProvider {
  fun getFeatureFlags(project: Project): FeatureFlags =
    with(BazelFeatureFlags) {
      FeatureFlags(
        isPythonSupportEnabled = isPythonSupportEnabled,
        isAndroidSupportEnabled = isAndroidSupportEnabled,
        isGoSupportEnabled = isGoSupportEnabled,
        isPropagateExportsFromDepsEnabled = !isWrapLibrariesInsideModulesEnabled,
        bazelSymlinksScanMaxDepth = symlinkScanMaxDepth,
        bazelShutDownBeforeShardBuild = shutDownBeforeShardBuild,
        isSharedSourceSupportEnabled = CodeInsightContextManager.getInstance(project).isSharedSourceSupportEnabled,
        isBazelQueryTabEnabled = isBazelQueryTabEnabled,
      )
    }
}
