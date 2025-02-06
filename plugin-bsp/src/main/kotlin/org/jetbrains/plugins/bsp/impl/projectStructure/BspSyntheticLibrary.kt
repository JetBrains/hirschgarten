package org.jetbrains.plugins.bsp.projectStructure

import BspVfsUtils
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import io.ktor.util.collections.ConcurrentSet
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import java.io.File
import javax.swing.Icon

/**
 * A [SyntheticLibrary] pointing to a list of external files for a language. Only supports one
 * instance per value of presentableText.
 *
 * @param presentableText user-facing text used to name the library. It's also used to implement
 * equals, hashcode -- there must only be one instance per value of this text
 * @param files collection of files that this synthetic library is responsible for.
 */
class BspSyntheticLibrary(private val presentableText: String, private val files: Set<File>) : SyntheticLibrary(),
  ItemPresentation {
  private val validFiles = ConcurrentSet<VirtualFile>()

  /**
   * Constructs library with an initial set of valid [VirtualFile]s.
   *
   * @param presentableText user-facing text used to name the library. It's also used to implement
   * equals, hashcode -- there must only be one instance per value of this text
   * @param files collection of files that this synthetic library is responsible for.
   */
  init {
    validFiles.addAll(files.mapNotNull { f: File -> BspVfsUtils.resolveVirtualFile(f, true) })
  }

  override fun getPresentableText(): String? {
    return presentableText
  }

  fun removeInvalidFiles(deletedFiles: Set<VirtualFile>) {
    if (deletedFiles.any { it.isDirectory }) {
      validFiles.removeIf { !it.isValid }
    } else {
      validFiles.removeAll(deletedFiles)
    }
  }

  override fun getSourceRoots(): MutableSet<VirtualFile> {
    // this must return a set, otherwise SyntheticLibrary#contains will create a new set each time
    // it's invoked (very frequently, on the EDT)
    return validFiles
  }

  override fun equals(o: Any?): Boolean {
    // intended to be only a single instance added to the project for each value of presentableText
    return o is BspSyntheticLibrary && presentableText == o.presentableText
  }

  override fun hashCode(): Int {
    // intended to be only a single instance added to the project for each value of presentableText
    return presentableText.hashCode()
  }

  override fun getLocationString(): String? {
    return null
  }

  override fun getIcon(unused: Boolean): Icon {
    return BspPluginIcons.bsp
  }
}

