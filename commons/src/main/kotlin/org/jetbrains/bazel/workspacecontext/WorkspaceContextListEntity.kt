package org.jetbrains.bazel.workspacecontext

/**
 * Base list-based `WorkspaceContext` entity class - you need to extend it if you want to
 * create your list-based entity.
 */
abstract class WorkspaceContextListEntity<T> : WorkspaceContextEntity() {
  abstract val values: List<T>
}

data class BuildFlagsSpec(override val values: List<String>) : WorkspaceContextListEntity<String>()

data class EnabledRulesSpec(override val values: List<String>) : WorkspaceContextListEntity<String>() {
  fun isNotEmpty(): Boolean = values.isNotEmpty()
}

data class ImportRunConfigurationsSpec(override val values: List<String>) : WorkspaceContextListEntity<String>()

data class NoPruneTransitiveCompileTimeJarsPatternsSpec(override val values: List<String>) : WorkspaceContextListEntity<String>()

data class PrioritizeLibrariesOverModulesTargetKindsSpec(override val values: List<String>) : WorkspaceContextListEntity<String>()

data class PythonCodeGeneratorRuleNamesSpec(override val values: List<String>) : WorkspaceContextListEntity<String>()

data class SyncFlagsSpec(override val values: List<String>) : WorkspaceContextListEntity<String>()

data class TransitiveCompileTimeJarsTargetKindsSpec(override val values: List<String>) : WorkspaceContextListEntity<String>()
