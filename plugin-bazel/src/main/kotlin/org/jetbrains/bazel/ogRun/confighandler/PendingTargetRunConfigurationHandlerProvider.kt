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
package org.jetbrains.bazel.ogRun.confighandler

import org.jetbrains.bazel.ogRun.BlazeCommandRunConfiguration
import org.jetbrains.bazel.ogRun.other.Kind

class PendingTargetRunConfigurationHandlerProvider : BlazeCommandRunConfigurationHandlerProvider {
  override val displayLabel: String
    get() = "(select)"

  override fun canHandleKind(state: BlazeCommandRunConfigurationHandlerProvider.TargetState, kind: Kind?): Boolean =
    state == BlazeCommandRunConfigurationHandlerProvider.TargetState.PENDING

  override fun createHandler(configuration: BlazeCommandRunConfiguration): BlazeCommandRunConfigurationHandler =
    PendingTargetRunConfigurationHandler(configuration)

  override val id: String
    get() = "PendingTargetHandler"
}
