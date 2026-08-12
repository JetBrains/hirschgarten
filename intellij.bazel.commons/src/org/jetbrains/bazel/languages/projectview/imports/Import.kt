package org.jetbrains.bazel.languages.projectview.imports

import com.intellij.build.FilePosition
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
sealed class Import {

  abstract val isRequired: Boolean

  data class Resolved(val path: Path, override val isRequired: Boolean) : Import()
  data class Unresolved(val text: String, val position: FilePosition?, override val isRequired: Boolean) : Import()
}
