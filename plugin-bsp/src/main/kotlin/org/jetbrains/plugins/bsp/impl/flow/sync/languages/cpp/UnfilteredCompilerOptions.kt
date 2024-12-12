package org.jetbrains.plugins.bsp.impl.flow.sync.languages.cpp

import com.google.common.base.Preconditions


class UnfilteredCompilerOptions(private val baseOptionParser: BaseOptionParser, val registeredParser: Map<String, OptionParser>) {
  fun parse(unfilteredOptions: List<String>) {
    var nextOptionParser: OptionParser = baseOptionParser
    for (unfilteredOption in unfilteredOptions) {
      nextOptionParser = nextOptionParser.parseValue(unfilteredOption)!!
    }
  }

  fun getUninterpretedOptions(): List<String> {
    return baseOptionParser.values()
  }

  fun getExtractedOptionValues(optionName: String): List<String> {
    val parser: OptionParser = registeredParser.get(optionName)!!
    return parser.values()
  }

  class Builder {
    private val baseOptionParser = BaseOptionParser()
    private val registeredParsers: MutableMap<String, OptionParser> = mutableMapOf()
    fun registerSingleOrSplitOption(optionName: String): Builder {
      val newParser = SingleOrSplitOptionParser(optionName, baseOptionParser)
      registeredParsers[optionName] = newParser
      baseOptionParser.registerParser(newParser)
      return this
    }

    fun build(unfilteredOptions: List<String>): UnfilteredCompilerOptions {
      val option = UnfilteredCompilerOptions(baseOptionParser, registeredParsers.toMap())
      option.parse(unfilteredOptions)
      return option
    }
  }
}

interface OptionParser {
  /** Checks if the parser handles the next option value.  */
  fun handlesOptionValue(optionValue: String): Boolean

  /**
   * Parses the option and returns the next handler (assumes [.handlesOptionValue] is true).
   */
  fun parseValue(optionValue: String): OptionParser?

  /** Return a list of option values captured by the parser.  */
  fun values(): List<String>
}

/**
 * A base option parser that defers to a list of more-specific registered flag parsers, before
 * handling the flag itself.
 */
class BaseOptionParser() : OptionParser {
  private val registeredOptionParsers: MutableList<OptionParser> = mutableListOf()
  private val values: MutableList<String> = ArrayList()
  fun registerParser(parser: OptionParser): Unit {
    registeredOptionParsers.add(parser)
  }

  override fun handlesOptionValue(optionValue: String): Boolean {
    return true
  }

  override fun parseValue(optionValue: String): OptionParser? {
    for (registeredParser in registeredOptionParsers) {
      if (registeredParser.handlesOptionValue(optionValue)) {
        return registeredParser.parseValue(optionValue)
      }
    }
    values.add(optionValue)
    return this
  }

  override fun values(): List<String> {
    return values
  }
}

/**
 * A parser that handles flags that can be one or two tokens (e.g., "-Ihdrs", vs "-I", "hdrs").
 */
class SingleOrSplitOptionParser(
  val optionName: String,
  val baseOptionParser: BaseOptionParser
) : OptionParser {
  private val values: MutableList<String> = ArrayList()
  private var consumeNext = false

  override fun handlesOptionValue(optionValue: String): Boolean {
    return consumeNext || optionValue.startsWith(optionName)
  }

  override fun parseValue(optionValue: String): OptionParser? {
    if (consumeNext) {
      consumeNext = false
      values.add(optionValue)
      return baseOptionParser
    }
    if (optionValue == optionName) {
      consumeNext = true
      return this
    }
    if (optionValue.startsWith(optionName)) {
      values.add(optionValue.substring(optionName.length).trim { it <= ' ' })
      return baseOptionParser
    }
    return null
  }

  override fun values(): List<String> {
    return values
  }
}


