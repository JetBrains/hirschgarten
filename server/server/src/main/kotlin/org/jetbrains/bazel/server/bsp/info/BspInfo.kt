package org.jetbrains.bazel.server.bsp.info

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import java.nio.file.Path

@ApiStatus.Internal
open class BspInfo(val bspProjectRoot: Path) {
  open val bazelBspDir: Path = bspProjectRoot.resolve(Constants.DOT_BAZELBSP_DIR_NAME)
}
