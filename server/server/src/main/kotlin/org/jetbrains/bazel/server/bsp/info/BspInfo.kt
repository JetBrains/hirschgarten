package org.jetbrains.bazel.server.bsp.info

import java.nio.file.Path

class BspInfo(val bspProjectRoot: Path) {
  val bazelBspDir: Path = bspProjectRoot.resolve(".bazelbsp")
}
