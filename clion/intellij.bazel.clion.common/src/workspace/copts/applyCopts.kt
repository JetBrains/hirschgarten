package org.jetbrains.bazel.clion.workspace.copts

import com.jetbrains.cidr.lang.toolchains.CidrSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.CompilerSpecificSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.workspace.CcImportContext

private val GNU_COMPILERS = setOf(OCCompilerId.GCC, OCCompilerId.CLANG, OCCompilerId.APPLE_CLANG)

/** The GNU spelling of the path options. */
private val GNU_PROCESSORS = listOf(
  GnuCoptsProcessor("-I") { withIncludePath(it) },
  GnuCoptsProcessor("-isystem") { withSystemIncludePath(it) },
  GnuCoptsProcessor("-iquote") { withQuoteIncludePath(it) },
  GnuCoptsProcessor("--sysroot", separator = "=") { withSysroot(it) },
)

/** The GNU and Clang spelling of the forward options. */
private val GNU_FORWARDERS = listOf(
  CoptsForwardNext("-Xclang"),
  CoptsForwardNext("-Xclangas"),
  CoptsForwardNext("-Xanalyzer"),
  CoptsForwardNext("-Xpreprocessor"),
  CoptsForwardNext("-Xassembler"),
  CoptsForwardNext("-Xlinker"),
  CoptsForwardNext("-mllvm"),
  CoptsForwardNext("-Xarch_", prefix = true), // the arch follows the flag, e.g. -Xarch_x86_64
  CoptsForwardNext("-Xopenmp-target", prefix = true), // the target triple can follow the flag, e.g. -Xopenmp-target=nvptx64
  CoptsForwardCommaSeparated("-Wp"),
  CoptsForwardCommaSeparated("-Wa"),
  CoptsForwardCommaSeparated("-Wl"),
)

private class CoptsForwardGroup(val forwarder: CoptsForwarder, val stream: CoptsProcessorStream)

private class CoptsProcessorStream(val sink: CompilerSpecificSwitchBuilder, val processors: List<CoptsProcessor>) {

  constructor(kind: OCCompilerKind, processors: List<CoptsProcessor>) : this(kind.getSwitchBuilder(CidrSwitchBuilder()), processors)

  private var pending: CoptsProcessor? = null

  context(ctx: CcImportContext)
  suspend fun consume(option: String) {
    // Bazel writes an empty option when a make variable expands to nothing. A blank value drops its flag as well.
    if (option.isBlank()) {
      pending = null
      return
    }

    pending?.let {
      it.apply(sink, option)
      pending = null
      return
    }

    val processor = processors.firstOrNull { option.startsWith(it.flag) }
    if (processor == null) {
      sink.withSwitch(option)
      return
    }

    if (option == processor.flag) {
      // the next option is the value; for example, -I include/foo
      pending = processor
    }
    else {
      // the value follows the flag; for example, -Iinclude/foo or --sysroot=path
      processor.apply(sink, option.removePrefix(processor.flag))
    }
  }
}

@ApiStatus.Internal
context(ctx: CcImportContext)
suspend fun CompilerSpecificSwitchBuilder.applyCopts(kind: OCCompilerKind, options: List<String>) {
  // TODO: add support for other compiler, e.g. MSVC
  val isGnu = kind.getId() in GNU_COMPILERS

  val processors = if (isGnu) GNU_PROCESSORS else emptyList()
  val forwarders = if (isGnu) GNU_FORWARDERS else emptyList()

  val stream = CoptsProcessorStream(this, processors)
  val forwardStreams = mutableMapOf<String, CoptsForwardGroup>()

  val iterator = options.iterator()
  options@ while (iterator.hasNext()) {
    val option = iterator.next()

    for (forwarder in forwarders) {
      val match = forwarder.match(option) ?: continue
      val group = forwardStreams.getOrPut(match) { CoptsForwardGroup(forwarder, CoptsProcessorStream(kind, processors)) }

      forwarder.expand(option, iterator).forEach { group.stream.consume(it) }
      continue@options
    }

    stream.consume(option)
  }

  forwardStreams.forEach { (flag, group) -> group.forwarder.apply(this, flag, group.stream.sink.buildRaw()) }
}
