package org.jetbrains.bazel.clion.workspace.copts

import com.jetbrains.cidr.lang.workspace.compiler.CompilerSpecificSwitchBuilder
import org.jetbrains.bazel.clion.workspace.CcImportContext
import org.jetbrains.bsp.protocol.OutputLocation

/** A compile option that carries a path. */
internal interface CoptsProcessor {
  val flag: String

  context(ctx: CcImportContext)
  fun apply(builder: CompilerSpecificSwitchBuilder, value: String)
}

internal class GnuCoptsProcessor(
  override val flag: String,
  private val separator: String = "",
  private val body: CompilerSpecificSwitchBuilder.(String) -> Unit,
) : CoptsProcessor {

  context(ctx: CcImportContext)
  private fun resolvePath(value: String): String {
    // GCC and Clang replace a `=` prefix of an include path with the sysroot. Keep such a path as it is.
    if (value.startsWith("=")) return value

    // TODO: this would need to be resolved against the hardlink cache as well
    return ctx.resolve(OutputLocation.parseExecrootPath(value))?.toString() ?: value
  }

  context(ctx: CcImportContext)
  override fun apply(builder: CompilerSpecificSwitchBuilder, value: String) {
    body(builder, resolvePath(value.removePrefix(separator)))
  }
}

