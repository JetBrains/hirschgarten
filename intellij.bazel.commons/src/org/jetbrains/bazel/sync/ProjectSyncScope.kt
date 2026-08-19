package org.jetbrains.bazel.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

@ApiStatus.Internal
sealed interface ProjectSyncScope {
  /**
   * Invoke bazel build actions
   */
  val build: Boolean

  /**
   * Perform full resync of the project, discard old state and
   * rebuild it from scratch through bazel invocation over entire target universe
   *
   * @property build Invoke bazel build actions
   * @property phased Execute as phased sync (query + build)
   */
  data class Full(override val build: Boolean, val phased: Boolean) : ProjectSyncScope

  /**
   * Resync subset of targets specified by [patterns].
   * `bazel build` invocation is done over target patterns included in [patterns].
   * Project model update is purely additive, that means target removal is not handled.
   *
   * @property patterns Target patterns used in `bazel build` invocation
   * @property build Invoke bazel build actions
   */
  data class Targets(val patterns: List<Label>, override val build: Boolean) : ProjectSyncScope

  /**
   * Perform project resync based on modified files, [files] list can contain
   * both bazel configuration files (e.g., `BUILD`, `MODULE.bazel`, `.bazelproject`, `.bzl`) and normal source files.
   * Based on modified files sync scope might be demoted to full resync.
   *
   * Same as [ProjectSyncScope.Targets] scope project model update is additive.
   *
   * @property files Modified files
   * @property build Invoke bazel build actions
   */
  data class Files(val files: List<Path>, override val build: Boolean) : ProjectSyncScope

  ///**
  // * Perform incremental sync over changes targets relative to previous sync run.
  // * In contrast to [ProjectSyncScope.Targets] and [ProjectSyncScope.Files] this mode
  // * handle removal of targets from project model.
  // *
  // * @property build Invoke bazel build actions
  // */
  //data class Incremental(val build: Boolean) : ProjectSyncScope
}
