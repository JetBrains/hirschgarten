package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetData
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetWithCustomData

fun <D : WorkspaceFileSetData> WorkspaceFileIndex.findFileSetWithCustomDataCompat(
  file: VirtualFile,
  honorExclusion: Boolean,
  includeContentSets: Boolean,
  includeExternalSets: Boolean,
  includeExternalSourceSets: Boolean,
  includeCustomKindSets: Boolean,
  customDataClass: Class<out D>,
): WorkspaceFileSetWithCustomData<D>? =
  findFileSetWithCustomData(
    file,
    honorExclusion,
    includeContentSets,
    includeExternalSets,
    includeExternalSourceSets,
    includeCustomKindSets,
    customDataClass,
  )
