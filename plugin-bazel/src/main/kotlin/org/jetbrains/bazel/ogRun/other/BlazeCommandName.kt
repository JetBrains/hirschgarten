/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.other

import com.google.common.base.Preconditions
import java.util.Collections
import javax.annotation.concurrent.Immutable

/**
 * A class for Blaze/Bazel command names. We enumerate the commands we use (and that we expect users
 * to be interested in), but do NOT use an enum because we want to allow users to specify arbitrary
 * commands.
 */
@Immutable
data class BlazeCommandName(val name: String) {
  init {
    Preconditions.checkArgument(!name.isEmpty(), "Command should be non-empty.")
  }

  override fun toString(): String = name

  companion object {
    private val _knownCommands: MutableMap<String, BlazeCommandName> =
      Collections.synchronizedMap(LinkedHashMap())

    val TEST = fromString("test")
    val RUN = fromString("run")
    val BUILD = fromString("build")
    val QUERY = fromString("query")
    val CQUERY = fromString("cquery")
    val INFO = fromString("info")
    val MOBILE_INSTALL = fromString("mobile-install")
    val COVERAGE = fromString("coverage")
    val MOD = fromString("mod")

    fun fromString(name: String): BlazeCommandName {
      _knownCommands.putIfAbsent(name, BlazeCommandName(name))
      return _knownCommands[name]!!
    }

    /**
     * @return An unmodifiable view of the Blaze commands we know about (including those that the user
     * has specified, in addition to those we have hard-coded).
     */
    val knownCommands: Collection<BlazeCommandName> get() {
      return _knownCommands.values
    }
  }
}
