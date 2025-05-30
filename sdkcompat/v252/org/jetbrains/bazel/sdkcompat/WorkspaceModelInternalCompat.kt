package org.jetbrains.bazel.sdkcompat

import com.intellij.platform.backend.workspace.StorageReplacement
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal

fun WorkspaceModelInternal.replaceWorkspaceModelCompat(description: String, storageReplacement: StorageReplacement) =
  replaceWorkspaceModel(description, storageReplacement)
