/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.run2

import com.google.common.collect.ImmutableList
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.TargetPattern
import java.lang.String
import kotlin.assert
import kotlin.text.lastIndexOf
import kotlin.text.substring
import kotlin.toString

/**
 * Utility class for creating Blaze configuration names of the form "{build system name} {command
 * name} {target string}", where each component is optional.
 */
class BlazeConfigurationNameBuilder {
  private var commandName: String? = null
  private var targetString: String? = null

  /**
   * Use the passed `configuration` to initialize the build system name, command name, and
   * target string. If the configuration's command name is null, this will default to "command". If
   * the configuration's target is null, target string will also be null.
   */
  constructor(configuration: BlazeCommandRunConfiguration) {
    val commandName = configuration.getHandler().commandName
    setCommandName(commandName?.toString() ?: "command")

    val targets: ImmutableList<out TargetPattern> = configuration.targets
    if (!targets.isEmpty()) {
      val first: TargetPattern = targets.get(0)
      val text: String? = if (first is Label) getTextForLabel(first as Label?) else first.toString()
      setTargetString(text)
    }
  }

  /** Sets the command name to `commandName`.  */
  @CanIgnoreReturnValue
  fun setCommandName(commandName: String?): BlazeConfigurationNameBuilder {
    this.commandName = commandName
    return this
  }

  /** Sets the target string to `targetString`.  */
  @CanIgnoreReturnValue
  fun setTargetString(targetString: String?): BlazeConfigurationNameBuilder {
    this.targetString = targetString
    return this
  }

  /**
   * Sets the target string to a string of the form "{package}:{target}", where 'target' is `label`'s target, and the 'package' is the containing package. For example, the [Label]
   * "//javatests/com/google/foo/bar/baz:FooTest" will set the target string to "baz:FooTest".
   */
  @CanIgnoreReturnValue
  fun setTargetString(label: Label): BlazeConfigurationNameBuilder {
    this.targetString =
      String.format("%s:%s", getImmediatePackage(label), label.targetName().toString())
    return this
  }

  /**
   * Builds a name of the form "{build system name} {command name} {target string}". Any null
   * components are omitted, and there is always one space inserted between each included component.
   */
  fun build(): kotlin.String {
    // Use this instead of String.join to omit null terms.
    return StringUtil.join(listOf("Bazel", commandName, targetString), " ")
  }

  companion object {
    /**
     * Returns a ui-friendly label description, or the form "{package}:{target}", where 'target' is
     * `label`'s target, and the 'package' is the containing package. For example, the [ ] "//javatests/com/google/foo/bar/baz:FooTest" will return "baz:FooTest".
     */
    fun getTextForLabel(label: Label): String = String.format("%s:%s", getImmediatePackage(label), label.targetName)

    /**
     * Get the portion of a label between the colon and the preceding slash. Example:
     * "//javatests/com/google/foo/bar/baz:FooTest" -> "baz".
     */
    private fun getImmediatePackage(label: Label): String {
      val labelString = label.toString()
      val colonIndex = labelString.lastIndexOf(':')
      assert(colonIndex >= 0)
      val slashIndex = labelString.lastIndexOf('/', colonIndex)
      assert(slashIndex >= 0)
      return labelString.substring(slashIndex + 1, colonIndex)
    }
  }
}
