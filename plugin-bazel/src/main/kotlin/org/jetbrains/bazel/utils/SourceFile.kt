package org.jetbrains.bazel.utils

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import java.net.URI
import java.nio.file.Path
import javax.swing.Icon
import kotlin.io.path.toPath

enum class SourceType(private val extensions: Array<String>) {
  JAVA(arrayOf("java")),
  KOTLIN(arrayOf("kt", "kts")),
  SCALA(arrayOf("scala")),
  PYTHON(arrayOf("py")),
  ;

  companion object {
    fun fromExtension(extension: String): SourceType? = entries.find { extension in it.extensions }

    private val allSourceExtensions = SourceType.entries.flatMap { it.extensions.toList() }.toTypedArray()

    fun hasSourceFileExtension(name: CharSequence): Boolean = allSourceExtensions.any { name.endsWith(it, ignoreCase = true) }
  }
}

interface SourceTypeIconProvider {
  fun getSourceType(): SourceType

  fun getIcon(): Icon?

  companion object {
    private val EP = ExtensionPointName<SourceTypeIconProvider>("org.jetbrains.bazel.sourceTypeIconProvider")

    fun getIconBySourceType(sourceType: SourceType?): Icon? =
      sourceType?.let {
        EP.extensionList.find { it.getSourceType() == sourceType }?.getIcon()
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
  return isFile && SourceType.hasSourceFileExtension(nameSequence)
}

fun Path.isSourceFile(): Boolean = SourceType.hasSourceFileExtension(toString())

fun URI.isSourceFile(): Boolean = SourceType.hasSourceFileExtension(toPath().toString())
