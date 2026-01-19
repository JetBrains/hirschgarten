package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.internal.InternalsBridgeService
import org.jetbrains.bazel.sync_new.internal.StarlarkSyntaxStmt
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class StarlarkFileParser(private val project: Project) {
  // TODO: handle multiple bazel versions compat
  fun parse(content: String, path: Path): StarlarkParsedFile {
    val bridge = project.service<InternalsBridgeService>().bridge
    val parsed = bridge.createStarlarkParser().parse(content, path.absolutePathString())
    val loads = parsed.stmts.filterIsInstance<StarlarkSyntaxStmt.LoadStmt>()
      .mapNotNull { Label.parseOrNull(it.import) }
    return StarlarkParsedFile(
      path = path,
      loads = loads,
    )
  }
}
