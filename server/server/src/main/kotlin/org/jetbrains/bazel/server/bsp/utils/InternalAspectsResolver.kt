package org.jetbrains.bazel.server.bsp.utils

import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.info.BspInfo

class InternalAspectsResolver(
  val bspInfo: BspInfo,
) {
  private val prefix: Lazy<String> = lazy { getPrefix() }

  fun resolveLabel(aspect: String): String = prefix.value + aspect

  val bazelBspRoot: String
    get() = bspInfo.bazelBspDir.toString()

  private fun getPrefix(): String = "//" + Constants.DOT_BAZELBSP_DIR_NAME  + "/aspects:core.bzl%"
}
