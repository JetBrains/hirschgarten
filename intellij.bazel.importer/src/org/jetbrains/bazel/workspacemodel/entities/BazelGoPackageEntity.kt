package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey

@ApiStatus.Internal
data class ImportPathId(val importPath: String) : SymbolicEntityId<BazelGoPackageEntity> {
  override val presentableName: @NlsSafe String
    get() = importPath
}

/**
 * Represents a Go target, like `go_library` or `go_test`, with a caveat:
 * targets with the same `importpath` are grouped together into one package.
 *
 * The reason for this grouping is that `rules_go` allows embedding targets with the same `importpath` into one another via `embed`
 * [(see docs)](https://github.com/bazel-contrib/rules_go/blob/master/docs/go/core/embedding.md).
 * In this case the sources are compiled together as if they were one target.
 * That means we cannot know in which context a source file is used in:
 * in the context of the target where the source is actually defined, or in another target with an `embed` attribute.
 */
@ApiStatus.Internal
interface BazelGoPackageEntity : WorkspaceEntityWithSymbolicId {
  override val symbolicId: ImportPathId
    get() = ImportPathId(importPath)

  val importPath: String
  val sources: List<VirtualFileUrl>
}

@ApiStatus.Internal
data class BazelGoTargetEntityId(private val targetKey: WorkspaceModelTargetKey) : SymbolicEntityId<BazelGoTargetEntity> {
  override val presentableName: @NlsSafe String
    get() = toString()
}

@ApiStatus.Internal
interface BazelGoTargetEntity : WorkspaceEntityWithSymbolicId {
  override val symbolicId: BazelGoTargetEntityId
    get() = BazelGoTargetEntityId(_targetKey)

  val _targetKey: WorkspaceModelTargetKey
  val importPath: ImportPathId
}

@get:ApiStatus.Internal
val BazelGoTargetEntity.targetKey: WorkspaceTargetKey
  get() = _targetKey.toWorkspaceTarget()
