package org.jetbrains.bazel.utils

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import java.net.URI
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.extension
import kotlin.io.path.toPath

enum class SourceType(private val extensions: List<String>) {
  JAVA(listOf("java")),
  KOTLIN(listOf("kt", "kts")),
  SCALA(listOf("scala")),
  PYTHON(listOf("py")),
  ;

  companion object {
    fun fromExtension(extension: String): SourceType? = entries.find { extension in it.extensions }
  }
}

interface SourceTypeIconProvider {
  fun getSourceType(): SourceType

  fun getIcon(): Icon?

  companion object {
    private val EP = ExtensionPointName<SourceTypeIconProvider>("org.jetbrains.bazel.sourceTypeIconProvider")

    fun getIconBySourceType(sourceType: SourceType?): Icon? {
      return sourceType?.let { EP.extensionList.find { it.getSourceType() == sourceType }?.getIcon() }
    }
  }
}

fun VirtualFile.isSourceFile(): Boolean {
  val isFile =
    try {
      this.isFile
    } catch (_: UnsupportedOperationException) {
      false
    }
  return isFile && extension?.lowercase()?.let { SourceType.fromExtension(it) } != null
}

fun Path.isSourceFile(): Boolean = SourceType.fromExtension(this.extension) != null

fun URI.isSourceFile(): Boolean =
  with(toPath()) {
    SourceType.fromExtension(this.extension) != null
  }
