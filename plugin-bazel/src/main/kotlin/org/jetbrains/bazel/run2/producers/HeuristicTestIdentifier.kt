/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.run2.producers

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.commons.WorkspacePath
import org.jetbrains.bazel.run2.ExecutorType

/**
 * A rough heuristic to recognize test files without resolving PSI elements.
 *
 *
 * Used by run configuration producers in situations where it's expensive to resolve PSI (e.g.
 * for files outside the project).
 */
interface HeuristicTestIdentifier {
  /**
   * Returns the [ExecutorType]s relevant for this file path, or an empty set if the file path
   * doesn't appear to be runnable.
   *
   *
   * A best effort, rough heuristic based on the file name + path.
   *
   *
   * This method is run frequently on the EDT, so must be fast.
   */
  fun supportedExecutors(path: WorkspacePath): Set<ExecutorType>

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<HeuristicTestIdentifier> =
      ExtensionPointName.create("com.google.idea.blaze.HeuristicTestIdentifier")
  }
}
