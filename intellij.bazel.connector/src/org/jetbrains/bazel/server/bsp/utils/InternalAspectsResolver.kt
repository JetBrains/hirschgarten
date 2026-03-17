package org.jetbrains.bazel.server.bsp.utils

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import java.nio.file.Path
import kotlin.io.path.name

@ApiStatus.Internal
class InternalAspectsResolver(
  val workspaceRoot: Path,
) {
  private val dotBazelBspDir: Path = workspaceRoot.resolve(Constants.DOT_BAZELBSP_DIR_NAME)
  private val prefix: String = "//${Constants.DOT_BAZELBSP_DIR_NAME}/aspects:core.bzl%"

  val aspectsPath: Path = dotBazelBspDir.resolve(Constants.ASPECTS_ROOT)
  fun resolveLabel(aspect: String): String = prefix + aspect
}
