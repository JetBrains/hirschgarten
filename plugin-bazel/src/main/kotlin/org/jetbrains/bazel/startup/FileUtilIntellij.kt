package org.jetbrains.bazel.startup

import org.jetbrains.bazel.commons.FileUtil
import java.io.File
import com.intellij.openapi.util.io.FileUtil as IntellijFileUtil

object FileUtilIntellij : FileUtil{
  override fun isAncestor(ancestor: String, file: String, strict: Boolean): Boolean = IntellijFileUtil.isAncestor(ancestor, file, strict)

  override fun isAncestor(ancestor: File, file: File, strict: Boolean): Boolean = IntellijFileUtil.isAncestor(ancestor, file, strict)

}
