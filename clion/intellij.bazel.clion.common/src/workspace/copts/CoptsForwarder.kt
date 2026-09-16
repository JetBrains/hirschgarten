package org.jetbrains.bazel.clion.workspace.copts

import com.jetbrains.cidr.lang.workspace.compiler.CompilerSpecificSwitchBuilder

/** An option that forwards other options. */
internal interface CoptsForwarder {

  /** Returns the matching part of the option, or null when the option is not this forwarder. */
  fun match(option: String): String?

  /** Gives the options that [option] forwards. It takes a separate value from [options]. */
  fun expand(option: String, options: Iterator<String>): List<String>

  /** Writes the forwarded switches back using [flag]. */
  fun apply(builder: CompilerSpecificSwitchBuilder, flag: String, values: List<String>)
}

/** A forwarder that gives the next option to another tool. */
internal class CoptsForwardNext(private val flag: String, private val prefix: Boolean = false) : CoptsForwarder {

  override fun match(option: String): String? = when {
    option == flag -> flag
    prefix && option.startsWith(flag) -> option
    else -> null
  }

  override fun expand(option: String, options: Iterator<String>): List<String> {
    if (options.hasNext()) {
      return listOf(options.next())
    }
    else {
      return emptyList()
    }
  }

  override fun apply(builder: CompilerSpecificSwitchBuilder, flag: String, values: List<String>) {
    for (value in values) {
      builder.withSwitch(flag)
      builder.withSwitch(value)
    }
  }
}

internal class CoptsForwardCommaSeparated(private val flag: String) : CoptsForwarder {

  private val prefix = "$flag,"

  override fun match(option: String): String? = flag.takeIf { option.startsWith(prefix) }

  override fun expand(option: String, options: Iterator<String>): List<String> {
    return option.removePrefix(prefix).split(",")
  }

  override fun apply(builder: CompilerSpecificSwitchBuilder, flag: String, values: List<String>) {
    if (values.isNotEmpty()) {
      builder.withSwitch(values.joinToString(prefix = prefix, separator = ","))
    }
  }
}
