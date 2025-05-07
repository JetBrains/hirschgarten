package org.jetbrains.bazel.cpp.sync.compiler

/**
 * Parses any compiler options that were not extracted by the build system earlier. Do minimal
 * parsing to extract what we need.
 * See com.google.idea.blaze.cpp.UnfilteredCompilerOptions
 */
internal class UnfilteredCompilerOptions(
  private val baseOptionParser: OptionParser,
  private val registeredParsers: Map<String, OptionParser>,
) {
  companion object {
    /** Make a new builder to register options to extract.  */
    fun builder() = Builder()
  }

  internal class Builder {
    private val baseOptionParser = BaseOptionParser()
    private val registeredParsers = mutableMapOf<String, OptionParser>()

    /** Have the options parser handle the given one-or-two-token option (e.g., -Ifoo or -I foo).  */
    fun registerSingleOrSplitOption(optionName: String): Builder {
      registeredParsers.put(
        optionName,
        SingleOrSplitOptionParser(optionName, baseOptionParser),
      )
      return this
    }

    /** Parse the given options and build extracted compiler options  */
    fun build(unfilteredOptions: List<String>): UnfilteredCompilerOptions {
      baseOptionParser.setRegisteredOptionParsers(registeredParsers.values.toList())
      val options =
        UnfilteredCompilerOptions(baseOptionParser, registeredParsers)
      options.parse(unfilteredOptions)
      return options
    }
  }

  private fun parse(unfilteredOptions: List<String>) {
    var nextOptionParser: OptionParser = baseOptionParser
    for (unfilteredOption in unfilteredOptions) {
      nextOptionParser = nextOptionParser.parseValue(unfilteredOption)
    }
  }

  /**
   * Return the list of arguments that are not extracted (don't correspond to a registered option),
   * in the original order.
   */
  val uninterpretedOptions: List<String>
    get() = baseOptionParser.values()

  /**
   * Return the extracted option values for the given registered option name. E.g., if -I is
   * registered, and ["-foo", "-Ibar"] is parsed then getExtractedOptionValues("-I") returns
   * ["bar"]. List is in the original order.
   *
   * @param optionName the name of a flag that was registered to be extracted
   * @return option values corresponding to the flag.
   */
  fun getExtractedOptionValues(optionName: String): List<String> {
    val parser: OptionParser = registeredParsers[optionName] ?: return emptyList()
    return parser.values()
  }

  internal interface OptionParser {
    /** Checks if the parser handles the next option value.  */
    fun handlesOptionValue(optionValue: String): Boolean

    /**
     * Parses the option and returns the next handler (assumes [.handlesOptionValue] is true).
     */
    fun parseValue(optionValue: String): OptionParser

    /** Return a list of option values captured by the parser.  */
    fun values(): List<String>
  }

  /**
   * A base option parser that defers to a list of more-specific registered flag parsers, before
   * handling the flag itself.
   */
  private class BaseOptionParser : OptionParser {
    private val values: MutableList<String> = mutableListOf()
    private var registeredOptionParsers: List<OptionParser> = listOf()

    fun setRegisteredOptionParsers(registeredOptionParsers: List<OptionParser>) {
      this.registeredOptionParsers = registeredOptionParsers
    }

    override fun handlesOptionValue(optionValue: String) = true

    override fun parseValue(optionValue: String): OptionParser {
      for (registeredParser in registeredOptionParsers) {
        if (registeredParser.handlesOptionValue(optionValue)) {
          return registeredParser.parseValue(optionValue)
        }
      }
      values.add(optionValue)
      return this
    }

    override fun values() = values
  }

  /**
   * A parser that handles flags that can be one or two tokens (e.g., "-Ihdrs", vs "-I", "hdrs").
   */
  private class SingleOrSplitOptionParser(private val optionName: String, private val baseOptionParser: BaseOptionParser) : OptionParser {
    private val values: MutableList<String> = mutableListOf()
    private var consumeNext = false

    override fun handlesOptionValue(optionValue: String): Boolean = consumeNext || optionValue.startsWith(optionName)

    override fun parseValue(optionValue: String): OptionParser {
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
      throw IllegalStateException("parseValue should not fail if handlesOptionValue=true")
    }

    override fun values() = values
  }
}
