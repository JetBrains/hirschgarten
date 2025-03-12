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
package org.jetbrains.bazel.ogRun

import org.jetbrains.bazel.label.Label

/** Marker interface for all run configurations  */
interface BlazeRunConfiguration {
  /**
   * Returns a list of target expressions this configuration should run, empty if its targets aren't
   * known or valid.
   *
   *
   * Will be calculated synchronously, and in edge cases may involve significant work, so
   * shouldn't be called on the EDT.
   */
  val targets: List<Label>?

  /** Keep in sync with source XML  */
  var keepInSync: Boolean?
}
