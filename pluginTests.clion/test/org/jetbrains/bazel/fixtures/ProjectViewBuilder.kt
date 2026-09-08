package org.jetbrains.bazel.fixtures

/** Builds the text of a project view file. */
internal class ProjectViewBuilder {

  private val directories: MutableList<String> = mutableListOf()
  private val targets: MutableList<String> = mutableListOf()
  private val buildFlags: MutableList<String> = mutableListOf()
  private val syncFlags: MutableList<String> = mutableListOf()

  private var deriveTargetsFromDirectories = true

  fun addDirectories(vararg directories: String): ProjectViewBuilder {
    this.directories.addAll(directories)
    return this
  }

  fun addTargets(vararg targets: String): ProjectViewBuilder {
    this.targets.addAll(targets)
    return this
  }

  fun addBuildFlags(vararg buildFlags: String): ProjectViewBuilder {
    this.buildFlags.addAll(buildFlags)
    return this
  }

  fun addSyncFlags(vararg syncFlags: String): ProjectViewBuilder {
    this.syncFlags.addAll(syncFlags)
    return this
  }

  fun deriveTargetsFromDirectories(derive: Boolean): ProjectViewBuilder {
    this.deriveTargetsFromDirectories = derive
    return this
  }

  fun build(): String {
    val builder = StringBuilder()

    if (directories.isNotEmpty()) {
      builder.appendLine("directories:")
      directories.map(::toListElement).forEach(builder::append)
    }

    if (targets.isNotEmpty()) {
      builder.appendLine("targets:")
      targets.map(::toListElement).forEach(builder::append)
    }

    if (buildFlags.isNotEmpty()) {
      builder.appendLine("build_flags:")
      buildFlags.map(::toListElement).forEach(builder::append)
    }

    if (syncFlags.isNotEmpty()) {
      builder.appendLine("sync_flags:")
      syncFlags.map(::toListElement).forEach(builder::append)
    }

    builder.appendLine("derive_targets_from_directories: %b".format(deriveTargetsFromDirectories))

    return builder.toString()
  }
}

private fun toListElement(value: String): String {
  return "  %s%n".format(value)
}
