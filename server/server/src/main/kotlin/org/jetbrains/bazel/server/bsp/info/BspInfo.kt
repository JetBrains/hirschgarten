package org.jetbrains.bazel.server.bsp.info;

import java.nio.file.Path;

public class BspInfo {

  private final Path bspProjectRoot;

  public BspInfo(Path bspProjectRoot) {
    this.bspProjectRoot = bspProjectRoot;
  }

  public Path bspProjectRoot() {
    return bspProjectRoot;
  }

  public Path bazelBspDir() {
    return bspProjectRoot().resolve(".bazelbsp");
  }
}
