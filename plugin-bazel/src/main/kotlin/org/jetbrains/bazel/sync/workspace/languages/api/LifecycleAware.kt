package org.jetbrains.bazel.sync.workspace.languages

interface LifecycleAware {
  fun onSave()
}
