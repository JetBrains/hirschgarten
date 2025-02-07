package org.jetbrains.bsp.bazel.server.sync;

import org.jetbrains.bsp.bazel.server.model.AspectSyncProject;

public interface ProjectStorage {
  AspectSyncProject load();

  void store(AspectSyncProject project);
}
