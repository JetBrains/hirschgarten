package org.jetbrains.bazel.golang.treeview

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import java.util.SortedSet
import java.util.TreeSet
import javax.swing.Icon

/** Represents a [SyntheticLibrary] with a mutable set of child files.  */
internal class BazelGoExternalSyntheticLibrary(private val presentableText: String, childFiles: Set<VirtualFile>) :
  SyntheticLibrary(),
  ItemPresentation {
  private val childFiles: SortedSet<VirtualFile> = TreeSet(Comparator.comparing(VirtualFile::toString)).also { it.addAll(childFiles) }

  fun addFiles(files: Set<VirtualFile>) {
    childFiles.addAll(files)
  }

  override fun getIcon(unused: Boolean): Icon = BazelPluginIcons.bazel

  override fun getSourceRoots(): SortedSet<VirtualFile> = childFiles

  override fun equals(o: Any?): Boolean =
    o is BazelGoExternalSyntheticLibrary &&
      o.presentableText == presentableText &&
      o.sourceRoots == sourceRoots

  override fun hashCode(): Int = presentableText.hashCode()

  override fun getPresentableText(): String = presentableText
}
