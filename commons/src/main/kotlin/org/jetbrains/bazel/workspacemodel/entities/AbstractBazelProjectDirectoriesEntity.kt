package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Abstract
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

// todo to be removed
//   workaround for sdkcompat
@Abstract
interface AbstractBazelProjectDirectoriesEntity : WorkspaceEntity {
  val projectRoot: VirtualFileUrl
}
