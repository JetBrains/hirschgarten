package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import com.bazelbuild.v8_4_2.net.starlark.java.syntax.LoadStatement
import com.bazelbuild.v8_4_2.net.starlark.java.syntax.ParserInput
import com.bazelbuild.v8_4_2.net.starlark.java.syntax.StarlarkFile
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class StarlarkFileParser(
  private val project: Project
) {
  // TODO: handle multiple bazel versions compat
  fun parse(content: String, path: Path): StarlarkParsedFile {
    val input = ParserInput.fromString(content, path.absolutePathString())
    val file = StarlarkFile.parse(input)
    val loads = file.statements.filterIsInstance<LoadStatement>()
      .mapNotNull { Label.parseOrNull(it.import.value) }
    return StarlarkParsedFile(
      path = path,
      loads = loads,
    )
  }
}
