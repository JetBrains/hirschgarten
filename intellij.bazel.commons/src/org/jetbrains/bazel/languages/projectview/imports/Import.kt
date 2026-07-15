package org.jetbrains.bazel.languages.projectview.imports

import com.intellij.build.FilePosition
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
sealed class Import {

  abstract val isRequired: Boolean

  data class Resolved(val file: VirtualFile, override val isRequired: Boolean) : Import()
  data class Unresolved(val text: String, val position: FilePosition?, override val isRequired: Boolean) : Import()
}
