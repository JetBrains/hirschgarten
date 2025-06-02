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
package org.jetbrains.bazel.run2.confighandler

import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration

internal class PendingTargetRunConfigurationHandlerProvider : BlazeCommandRunConfigurationHandlerProvider {
  override fun canHandleKind(state: BlazeCommandRunConfigurationHandlerProvider.TargetState, kind: TargetKind?): Boolean =
    state == BlazeCommandRunConfigurationHandlerProvider.TargetState.PENDING

  override fun createHandler(config: BlazeCommandRunConfiguration): BlazeCommandRunConfigurationHandler =
    PendingTargetRunConfigurationHandler(config)

  override val id: String
    get() = "PendingTargetHandler"
}
