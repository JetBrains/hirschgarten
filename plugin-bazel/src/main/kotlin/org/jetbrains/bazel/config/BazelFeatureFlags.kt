package org.jetbrains.bazel.config

import com.intellij.codeInsight.multiverse.isSharedSourceSupportEnabled
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bsp.protocol.FeatureFlags

object BazelFeatureFlags {
  private const val PYTHON_SUPPORT = "bsp.python.support"

  @VisibleForTesting
  const val GO_SUPPORT = "bsp.go.support"
  private const val QUERY_TERMINAL_COMPLETION = "bazel.query.terminal.completion"

  @VisibleForTesting
  const val BUILD_PROJECT_ON_SYNC = "bazel.build.project.on.sync"
  private const val SHORTEN_MODULE_LIBRARY_NAMES = "bsp.shorten.module.library.names"
  private const val EXECUTE_SECOND_PHASE_ON_SYNC = "bsp.execute.second.phase.on.sync"
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
  private const val EXCLUDE_SYMLINKS_FROM_FILE_WATCHER_VIA_REFLECTION = "bazel.exclude.symlinks.from.file.watcher.via.reflection"
  private const val FIND_IN_FILES_NON_INDEXABLE = "bazel.find.in.files.non.indexable"
  private const val SYNTHETIC_RUN_ENABLE = "bazel.run.synthetic.enable"
  private const val SYNTHETIC_RUN_DISABLE_VISIBILITY_CHECK = "bazel.run.synthetic.disable.visibility.check"

  @VisibleForTesting
  const val RUN_CONFIG_RUN_WITH_BAZEL = "bazel.run.config.run.with.bazel"
  const val USE_PTY = "bazel.use.pty"

  val isPythonSupportEnabled: Boolean
    get() = isEnabled(PYTHON_SUPPORT)

  val isGoSupportEnabled: Boolean
    get() = isEnabled(GO_SUPPORT)

  val isBuildProjectOnSyncEnabled: Boolean
    get() = isEnabled(BUILD_PROJECT_ON_SYNC)

  val isShortenModuleLibraryNamesEnabled: Boolean
    get() = isEnabled(SHORTEN_MODULE_LIBRARY_NAMES)

  val executeSecondPhaseOnSync: Boolean
    get() = isEnabled(EXECUTE_SECOND_PHASE_ON_SYNC)

  // File-based source root problems fixed here: https://youtrack.jetbrains.com/issue/IDEA-371097
  val fbsrSupportedInPlatform: Boolean
    get() = true

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
    get() = isEnabled(AUTO_OPEN_PROJECT_IF_PRESENT)

  val isQueryTerminalCompletionEnabled: Boolean
    get() = isEnabled(QUERY_TERMINAL_COMPLETION)

  val isBazelQueryTabEnabled: Boolean
    get() = isEnabled(ENABLE_BAZEL_QUERY_TAB)

  val excludeSymlinksFromFileWatcherViaReflection: Boolean
    get() = isEnabled(EXCLUDE_SYMLINKS_FROM_FILE_WATCHER_VIA_REFLECTION)

  val findInFilesNonIndexable: Boolean
    get() = isEnabled(FIND_IN_FILES_NON_INDEXABLE)

  val runConfigRunWithBazel: Boolean
    get() = isEnabled(RUN_CONFIG_RUN_WITH_BAZEL)

  val syntheticRunEnable: Boolean
    get() = isEnabled(SYNTHETIC_RUN_ENABLE)

  val syntheticRunDisableVisibilityCheck: Boolean
    get() = isEnabled(SYNTHETIC_RUN_DISABLE_VISIBILITY_CHECK)

  val usePty: Boolean
    get() = isEnabled(USE_PTY)

  private fun isEnabled(key: String): Boolean {
    System.getProperty(key)?.let { value ->
      return value.toBooleanStrict()
    }
    return Registry.`is`(key)
  }
}

object FeatureFlagsProvider {
  fun getFeatureFlags(project: Project): FeatureFlags =
    with(BazelFeatureFlags) {
      FeatureFlags(
        isPythonSupportEnabled = isPythonSupportEnabled,
        isGoSupportEnabled = isGoSupportEnabled,
        bazelSymlinksScanMaxDepth = symlinkScanMaxDepth,
        bazelShutDownBeforeShardBuild = shutDownBeforeShardBuild,
        isSharedSourceSupportEnabled = isSharedSourceSupportEnabled(project),
        isBazelQueryTabEnabled = isBazelQueryTabEnabled,
      )
    }
}
