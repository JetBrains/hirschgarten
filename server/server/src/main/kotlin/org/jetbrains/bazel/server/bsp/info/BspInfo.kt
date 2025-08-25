package org.jetbrains.bazel.server.bsp.info

import java.nio.file.Path

open class BspInfo(val bspProjectRoot: Path) {
  open val bazelBspDir: Path = bspProjectRoot.resolve(".bazelbsp")
}
