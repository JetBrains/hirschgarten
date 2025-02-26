package org.jetbrains.bazel.server.sync;

import org.jetbrains.bazel.server.model.AspectSyncProject;

public interface ProjectStorage {
  AspectSyncProject load();

  void store(AspectSyncProject project);
}
