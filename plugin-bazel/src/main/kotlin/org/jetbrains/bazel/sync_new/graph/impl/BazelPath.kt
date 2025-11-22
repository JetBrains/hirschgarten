package org.jetbrains.bazel.sync_new.graph.impl

import org.jetbrains.bazel.info.BspTargetInfo.FileLocation

sealed interface BazelPath {
  data class Absolute(val path: String) : BazelPath
  data class MainWorkspace(val relative: String) : BazelPath
  data class ExternalWorkspace(
    val rootExecutionPathFragment: String,
    val relative: String,
    val isSource: Boolean = false,
  ) : BazelPath

  companion object {
    fun fromFileLocation(location: FileLocation): BazelPath {
      return when {
        !location.isSource && !location.isExternal && location.rootExecutionPathFragment.isEmpty() ->
          Absolute(location.relativePath)

        location.isSource && !location.isExternal ->
          MainWorkspace(location.relativePath)

        location.isExternal ->
          ExternalWorkspace(
            rootExecutionPathFragment = location.rootExecutionPathFragment,
            relative = location.relativePath,
            isSource = location.isSource,
          )

        else -> MainWorkspace(location.relativePath)
      }
    }
  }
}

fun BazelPath.toFileLocation(): FileLocation = when (this) {
  is BazelPath.Absolute -> FileLocation.newBuilder()
    .setRelativePath(path)
    .setIsSource(false)
    .setIsExternal(false)
    .build()

  is BazelPath.MainWorkspace ->
    FileLocation.newBuilder()
      .setRelativePath(relative)
      .setIsSource(true)
      .setIsExternal(false)
      .setRootExecutionPathFragment("")
      .build()

  is BazelPath.ExternalWorkspace -> FileLocation.newBuilder()
    .setRelativePath(relative)
    .setIsSource(isSource)
    .setIsExternal(true)
    .setRootExecutionPathFragment(rootExecutionPathFragment)
    .build()
}

fun FileLocation.toBazelPath(): BazelPath = BazelPath.fromFileLocation(this)
