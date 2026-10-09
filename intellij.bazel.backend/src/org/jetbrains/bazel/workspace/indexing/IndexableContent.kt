package org.jetbrains.bazel.workspace.indexing

import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface IndexableContent {
  val recursiveRoots: Set<VirtualFileUrl>
  val nonRecursiveRoots: Set<VirtualFileUrl>
}

@ApiStatus.Internal
interface MutableIndexableContent : IndexableContent {
  override val recursiveRoots: MutableSet<VirtualFileUrl>
  override val nonRecursiveRoots: MutableSet<VirtualFileUrl>

  fun addAll(content: IndexableContent)
}

@ApiStatus.Internal
fun IndexableContent(
  recursiveRoots: Set<VirtualFileUrl> = emptySet(),
  nonRecursiveRoots: Set<VirtualFileUrl> = emptySet(),
): IndexableContent = MutableIndexableContent(recursiveRoots, nonRecursiveRoots)

@ApiStatus.Internal
fun MutableIndexableContent(
  recursiveRoots: Set<VirtualFileUrl> = emptySet(),
  nonRecursiveRoots: Set<VirtualFileUrl> = emptySet(),
): MutableIndexableContent = IndexableContentImpl(recursiveRoots.toMutableSet(), nonRecursiveRoots.toMutableSet())

@ApiStatus.Internal
fun buildIndexableContent(block: MutableIndexableContent.() -> Unit): IndexableContent = MutableIndexableContent().apply(block)

@ApiStatus.Internal
operator fun MutableIndexableContent.plusAssign(other: IndexableContent) {
  addAll(other)
}

private data class IndexableContentImpl(
  override val recursiveRoots: MutableSet<VirtualFileUrl>,
  override val nonRecursiveRoots: MutableSet<VirtualFileUrl>,
) : MutableIndexableContent {

  override fun addAll(content: IndexableContent) {
    recursiveRoots.addAll(content.recursiveRoots)
    nonRecursiveRoots.addAll(content.nonRecursiveRoots)
  }
}
