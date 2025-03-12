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
package org.jetbrains.bazel.ogRun.processhandler

import com.google.idea.blaze.base.async.process.LineProcessingOutputStream
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key
import java.nio.charset.StandardCharsets

/** Writes output from a process to a stream  */
class LineProcessingProcessAdapter(outputStream: LineProcessingOutputStream) : ProcessAdapter() {
    private val myOutputStream: LineProcessingOutputStream

    init {
        myOutputStream = outputStream
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>?) {
        val text = event.getText()
        if (text != null) {
            try {
                myOutputStream.write(text.toByteArray(StandardCharsets.UTF_8))
            } catch (e: IOException) {
                // Ignore -- cannot happen
            }
        }
    }
}
