package org.jetbrains.bazel.sync_new.internal

import com.bazelbuild.v8_4_2.net.starlark.java.syntax.LoadStatement
import com.bazelbuild.v8_4_2.net.starlark.java.syntax.ParserInput
import com.bazelbuild.v8_4_2.net.starlark.java.syntax.StarlarkFile
import com.bazelbuild.v8_4_2.net.starlark.java.syntax.Statement
import com.bazelbuild.v8_4_2.net.starlark.java.syntax.SyntaxError

internal class InternalsBridgeImpl_v8_4_2 : InternalsBridge {
  override fun createStarlarkParser(): StarlarkParser = StarlarParserImpl_v8_4_2()
}

private class StarlarParserImpl_v8_4_2 : StarlarkParser {
  override fun parse(content: String, file: String): StarlarkSyntaxFile? {
    return try {
      val input = ParserInput.fromString(content, file)
      StarlarkSyntaxFileImpl_v8_4_2(file = StarlarkFile.parse(input))
    } catch (_: SyntaxError.Exception) {
      null
    }
  }

}

private class StarlarkSyntaxFileImpl_v8_4_2(private val file: StarlarkFile) : StarlarkSyntaxFile {
  override val stmts: List<StarlarkSyntaxStmt>
    get() = file.statements.mapNotNull { StarlarkSyntaxStmtImpl_v8_4_2.create(it) }

}

private sealed interface StarlarkSyntaxStmtImpl_v8_4_2 : StarlarkSyntaxStmt {
  data class LoadStmtImpl_v8_4_2(val load: LoadStatement) : StarlarkSyntaxStmt.LoadStmt, StarlarkSyntaxStmtImpl_v8_4_2 {
    override val import: String
      get() = load.import.value
  }

  companion object {
    fun create(stmt: Statement): StarlarkSyntaxStmtImpl_v8_4_2? = when (stmt) {
      is LoadStatement -> LoadStmtImpl_v8_4_2(stmt)
      else -> null
    }
  }
}
