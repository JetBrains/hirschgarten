/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.testlogs

import com.google.idea.blaze.base.command.buildresult.BuildResultParser
import java.util.*

/**
 * A strategy for locating results from a single 'blaze test' invocation (e.g. output XML files).
 *
 *
 * Parses the output BEP proto written by blaze to locate the test XML files.
 */
class LocalBuildEventProtocolTestFinderStrategy
(buildResultHelper: BuildResultHelper) : BlazeTestResultFinderStrategy {
  private val buildResultHelper: BuildResultHelper

  init {
    this.buildResultHelper = buildResultHelper
  }

  @Throws(GetArtifactsException::class)
  override fun findTestResults(): BlazeTestResults {
    buildResultHelper.getBepStream(Optional.empty<T?>()).use { bepStream ->
      return BuildResultParser.getTestResults(bepStream)
    }
  }

  override fun deleteTemporaryOutputFiles() {
    buildResultHelper.deleteTemporaryOutputFiles()
  }
}
