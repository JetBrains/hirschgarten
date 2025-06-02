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

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration

/**
 * Provides a [BlazeCommandRunConfigurationHandler] corresponding to a given [ ].
 */
interface BlazeCommandRunConfigurationHandlerProvider {
  /** The target state of a blaze run configuration.  */
  enum class TargetState {
    KNOWN,
    PENDING,
  }

  /** Whether this extension is applicable to the kind.  */
  fun canHandleKind(state: TargetState, kind: TargetKind?): Boolean

  /** Returns the corresponding [BlazeCommandRunConfigurationHandler].  */
  fun createHandler(configuration: BlazeCommandRunConfiguration): BlazeCommandRunConfigurationHandler

  /**
   * Returns the unique ID of this [BlazeCommandRunConfigurationHandlerProvider]. The ID is
   * used to store configuration settings and must not change between plugin versions.
   */
  val id: String

  companion object {
    /**
     * Find a BlazeCommandRunConfigurationHandlerProvider applicable to the given kind. If no provider
     * is more relevant, [BlazeCommandGenericRunConfigurationHandlerProvider] is returned.
     */
    fun findHandlerProvider(state: TargetState, kind: TargetKind?): BlazeCommandRunConfigurationHandlerProvider {
      for (handlerProvider in EP_NAME.extensions) {
        if (handlerProvider.canHandleKind(state, kind)) {
          return handlerProvider
        }
      }
      throw RuntimeException(
        "No BlazeCommandRunConfigurationHandlerProvider found for Kind $kind",
      )
    }

    /** Get the BlazeCommandRunConfigurationHandlerProvider with the given ID, if one exists.  */
    fun getHandlerProvider(id: String?): BlazeCommandRunConfigurationHandlerProvider? {
      for (handlerProvider in EP_NAME.extensions) {
        if (handlerProvider.id == id) {
          return handlerProvider
        }
      }
      return null
    }

    @JvmField
    val EP_NAME: ExtensionPointName<BlazeCommandRunConfigurationHandlerProvider> =
      ExtensionPointName.create(
        "com.google.idea.blaze.BlazeCommandRunConfigurationHandlerProvider",
      )
  }
}
