package org.jetbrains.bazel.server.bsp.utils

import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.info.BspInfo

class InternalAspectsResolver(
  val bspInfo: BspInfo,
  val bazelRelease: BazelRelease,
  val shouldUseInjectRepository: Boolean,
) {
  private val prefix: Lazy<String> = lazy { getPrefix() }

  fun resolveLabel(aspect: String): String = prefix.value + aspect

  val bazelBspRoot: String
    get() = bspInfo.bazelBspDir.toString()

  /**
   * With [shouldUseInjectRepository] turned on, apparent name must be used, hence single `@`
   * fyi, https://github.com/bazelbuild/bazel/issues/22691#issuecomment-2472920470
   */
  private fun getPrefix(): String =
    when {
      bazelRelease.major in 0..5 || shouldUseInjectRepository -> "@" + Constants.ASPECT_REPOSITORY + "//aspects:core.bzl%"
      else -> "@@" + Constants.ASPECT_REPOSITORY + "//aspects:core.bzl%"
    }
}
