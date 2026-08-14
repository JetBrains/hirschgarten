package org.jetbrains.bazel.server.bsp.utils

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import java.nio.file.Path

@ApiStatus.Internal
class InternalAspectsResolver(
  val workspaceRoot: Path,
) {
  private val prefix: String = "//${Constants.DOT_BAZELBSP_DIR_NAME}/"

  fun resolveLabel(aspect: String): String = "$prefix$aspect"
}
