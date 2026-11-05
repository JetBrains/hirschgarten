package org.jetbrains.bazel.golang.sync

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Icons that the Go backend does not own. The icon files live in `intellij.bazel.resource`.
 * The language server runs without that module, so each icon is nullable.
 */
internal object BazelGoOptionalIcons {
  @JvmField
  val bazel: Icon? = IconLoader.findIcon("icons/bazel.svg", BazelGoOptionalIcons::class.java.classLoader)
}
