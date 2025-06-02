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
package org.jetbrains.bazel.commons.command

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import java.util.Collections

/**
 * A class for Blaze/Bazel command names. We enumerate the commands we use (and that we expect users
 * to be interested in), but do NOT use an enum because we want to allow users to specify arbitrary
 * commands.
 */
class BlazeCommandName private constructor(name: String) {
  private val name: String

  init {
    Preconditions.checkArgument(!name.isEmpty(), "Command should be non-empty.")
    this.name = name
  }

  override fun toString(): String = name

  override fun equals(o: Any?): Boolean {
    if (o !is BlazeCommandName) {
      return false
    }
    val that = o
    return name == that.name
  }

  override fun hashCode(): Int = name.hashCode()

  companion object {
    private val _knownCommands: MutableMap<String, BlazeCommandName> =
      Collections.synchronizedMap<String, BlazeCommandName>(LinkedHashMap<String, BlazeCommandName>())

    val TEST: BlazeCommandName = fromString("test")
    val RUN: BlazeCommandName = fromString("run")
    val BUILD: BlazeCommandName = fromString("build")
    val QUERY: BlazeCommandName = fromString("query")
    val CQUERY: BlazeCommandName = fromString("cquery")
    val INFO: BlazeCommandName = fromString("info")
    val MOBILE_INSTALL: BlazeCommandName = fromString("mobile-install")
    val COVERAGE: BlazeCommandName = fromString("coverage")
    val MOD: BlazeCommandName = fromString("mod")

    fun fromString(name: String): BlazeCommandName = _knownCommands.putIfAbsent(name, BlazeCommandName(name))!!

    /**
     * @return An unmodifiable view of the Blaze commands we know about (including those that the user
     * has specified, in addition to those we have hard-coded).
     */
    fun knownCommands(): Collection<BlazeCommandName> = ImmutableList.copyOf(_knownCommands.values)
  }
}
