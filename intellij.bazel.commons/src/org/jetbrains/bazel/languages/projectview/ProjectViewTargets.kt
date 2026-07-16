package org.jetbrains.bazel.languages.projectview

import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

@Internal
val DIRECTORIES_KEY: SectionKey<List<ExcludableValue<Path>>> = SectionKey("directories", emptyList())
val ProjectView.directories: List<ExcludableValue<Path>>
  // Default to workspace root if no directories specified
  get() = getSection(DIRECTORIES_KEY).takeIf { it.isNotEmpty() } ?: listOf(ExcludableValue.included(Path.of(".")))

@Internal
val TARGETS_KEY: SectionKey<List<ExcludableValue<Label>>> = SectionKey("targets", emptyList())

val ProjectView.targets: List<ExcludableValue<Label>>
  @Internal
  get() {
    return if (deriveTargetsFromDirectories) {
      createTargetsFromDirectories(getSection(TARGETS_KEY), directories)
    }
    else {
      getSection(TARGETS_KEY)
    }
  }

private fun createTargetsFromDirectories(
  targets: List<ExcludableValue<Label>>,
  dirs: List<ExcludableValue<Path>>,
): List<ExcludableValue<Label>> {
  fun hasEmptyIncludedAndEmptyExcluded(list: List<ExcludableValue<*>>): Boolean =
    list.none { it.isIncluded() } && list.none { !it.isIncluded() }

  fun hasEmptyIncludedAndNonEmptyExcluded(list: List<ExcludableValue<*>>): Boolean =
    list.none { it.isIncluded() } && list.any { !it.isIncluded() }

  fun mapDirectoryToTarget(buildDirectoryIdentifier: Path): Label? {
    if (buildDirectoryIdentifier.isAbsolute) return null
    val buildDirectoryPath = buildDirectoryIdentifier.invariantSeparatorsPathString
    if (buildDirectoryPath.isEmpty()) return null

    return if (buildDirectoryPath == ".") {
      Label.parse("//...")
    }
    else {
      Label.parse("//$buildDirectoryPath/...")
    }
  }

  when {
    dirs.isEmpty() -> return targets
    hasEmptyIncludedAndEmptyExcluded(dirs) -> return targets
    hasEmptyIncludedAndNonEmptyExcluded(dirs) -> {
      throw IllegalArgumentException("'directories' section has no included targets.")
    }

    else -> {
      val directoriesValues =
        dirs
          .filter { it.isIncluded() }
          .mapNotNull { mapDirectoryToTarget(it.value) }
          .map { ExcludableValue.included(it) }
      val directoriesExcludedValues =
        dirs
          .filter { !it.isIncluded() }
          .mapNotNull { mapDirectoryToTarget(it.value) }
          .map { ExcludableValue.excluded(it) }
      return targets + directoriesValues + directoriesExcludedValues
    }
  }
}
