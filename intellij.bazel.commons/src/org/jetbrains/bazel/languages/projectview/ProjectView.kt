package org.jetbrains.bazel.languages.projectview

import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.languages.projectview.imports.Import

/**
 * Immutable representation of a ProjectView: a map of section keys to parsed values.
 */
class ProjectView @Internal constructor(
  @get:Internal val sections: Map<SectionKey<*>, Any>,
  @get:Internal val imports: List<Import>,
) {
  @Internal
  fun isEmpty(): Boolean = sections.isEmpty() && imports.isEmpty()

  @Internal
  fun <T> getSection(key: SectionKey<T>): T {
    val value = sections[key]
    if (value != null) {
      @Suppress("UNCHECKED_CAST")
      return value as T
    }
    return key.default
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ProjectView

    if (sections != other.sections) return false
    if (imports != other.imports) return false

    return true
  }

  override fun hashCode(): Int {
    var result = sections.hashCode()
    result = 31 * result + imports.hashCode()
    return result
  }

  @Internal
  companion object {
    val EMPTY: ProjectView = ProjectView(mapOf(), listOf())
  }
}

/**
 * The required `import` statements (i.e. plain `import`, not `try_import`) whose target file could
 * not be found. A non-empty result means the project view is incomplete and the sync should be
 * stopped: otherwise it proceeds over an effectively-empty project view and fails slowly with
 * misleading errors. See `ReparseProjectViewFilePreSyncHook` (which reports each one) and
 * `ProjectSyncTask` (which aborts the sync).
 */
@Internal
fun ProjectView.unresolvedRequiredImports(): List<Import.Unresolved> =
  imports.filterIsInstance<Import.Unresolved>().filter { it.isRequired }
