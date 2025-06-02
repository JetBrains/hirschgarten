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
package org.jetbrains.bazel.run2.confighandler

import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration

/**
 * Generic handler provider for [BlazeCommandRunConfiguration]s, used as a fallback in the
 * case where no other handler providers are more relevant.
 */
class BlazeCommandGenericRunConfigurationHandlerProvider : BlazeCommandRunConfigurationHandlerProvider {
  override fun canHandleKind(state: BlazeCommandRunConfigurationHandlerProvider.TargetState, kind: TargetKind?): Boolean =
    state != BlazeCommandRunConfigurationHandlerProvider.TargetState.PENDING

  override fun createHandler(configuration: BlazeCommandRunConfiguration): BlazeCommandRunConfigurationHandler =
    BlazeCommandGenericRunConfigurationHandler(configuration)

  override val id: String
    get() = "BlazeCommandGenericRunConfigurationHandlerProvider"

  companion object {
    val instance: BlazeCommandGenericRunConfigurationHandlerProvider?
      get() =
        BlazeCommandRunConfigurationHandlerProvider.EP_NAME.findExtension(
          BlazeCommandGenericRunConfigurationHandlerProvider::class.java,
        )
  }
}
