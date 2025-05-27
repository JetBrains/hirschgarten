/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.command.buildresult.bepparser

import com.google.common.io.CountingInputStream
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import java.io.IOException
import java.io.InputStream
import java.lang.AutoCloseable
import java.util.Optional

/** Provides [BuildEventStreamProtos.BuildEvent]  */
interface BuildEventStreamProvider : AutoCloseable {
  /** An exception parsing a stream of build events.  */
  class BuildEventStreamException(message: String, e: Throwable? = null) : RuntimeException(message, e)

  /**
   * @return an object that represents the identity of the build to the user.
   */
  val id: String?

  @Throws(BuildEventStreamException::class)
  fun getNext(): BuildEventStreamProtos.BuildEvent?

  val bytesConsumed: Long

  override fun close()

  companion object {
    @Throws(BuildEventStreamException::class)
    fun parseNextEventFromStream(stream: InputStream): BuildEventStreamProtos.BuildEvent? {
      try {
        return BuildEventStreamProtos.BuildEvent.parseDelimitedFrom(stream)
      } catch (e: IOException) {
        throw BuildEventStreamException(e.message ?: "", e)
      }
    }

    /**
     * Creates a [BuildEventStreamProvider] from the given `stream`.
     *
     *
     * Note: takes ownership of the `stream` and closes it when is being closed.
     */
    fun fromInputStream(stream: InputStream): BuildEventStreamProvider {
      val countingStream = CountingInputStream(stream)
      return object : BuildEventStreamProvider {
        override fun close() {
          try {
            stream.close()
          } catch (e: IOException) {
            throw RuntimeException(e)
          }
        }

        override val id: String? = null

        @Throws(BuildEventStreamException::class)
        override fun getNext(): BuildEventStreamProtos.BuildEvent? {
          return parseNextEventFromStream(countingStream)
        }

        override val bytesConsumed: Long = countingStream.count
        }
      }
    }
  }
