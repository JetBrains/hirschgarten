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
package org.jetbrains.bazel.ogRun.confighandler

import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.google.idea.blaze.base.model.primitives.Kind
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Provides a [BlazeCommandRunConfigurationHandler] corresponding to a given [ ].
 */
interface BlazeCommandRunConfigurationHandlerProvider {
    val displayLabel: String?

    /** The target state of a blaze run configuration.  */
    enum class TargetState {
        KNOWN,
        PENDING
    }

    /** Whether this extension is applicable to the kind.  */
    fun canHandleKind(state: TargetState?, kind: Kind?): Boolean

    /** Returns the corresponding [BlazeCommandRunConfigurationHandler].  */
    fun createHandler(configuration: BlazeCommandRunConfiguration?): BlazeCommandRunConfigurationHandler?

    /**
     * Returns the unique ID of this [BlazeCommandRunConfigurationHandlerProvider]. The ID is
     * used to store configuration settings and must not change between plugin versions.
     */
    val id: String?

    companion object {
        /**
         * Find a BlazeCommandRunConfigurationHandlerProvider applicable to the given kind. If no provider
         * is more relevant, [BlazeCommandGenericRunConfigurationHandlerProvider] is returned.
         */
        fun findHandlerProvider(
            state: TargetState?, kind: Kind?
        ): BlazeCommandRunConfigurationHandlerProvider {
            val result = Iterables.getFirst<BlazeCommandRunConfigurationHandlerProvider?>(
                findHandlerProviders(state, kind),
                null
            )
            if (result != null) {
                return result
            }
            throw RuntimeException(
                "No BlazeCommandRunConfigurationHandlerProvider found for Kind " + kind
            )
        }

        /**
         * Find BlazeCommandRunConfigurationHandlerProviders applicable to the given kind.
         */
        fun findHandlerProviders(
            state: TargetState?, kind: Kind?
        ): MutableCollection<BlazeCommandRunConfigurationHandlerProvider?>? {
            return EP_NAME.extensionList.stream()
                .filter { it: BlazeCommandRunConfigurationHandlerProvider? -> it!!.canHandleKind(state, kind) }.collect(
                    ImmutableList.toImmutableList<BlazeCommandRunConfigurationHandlerProvider?>()
                )
        }

        fun findHandlerProviders(): MutableCollection<BlazeCommandRunConfigurationHandlerProvider?> {
            return EP_NAME.extensionList
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
            create.create<BlazeCommandRunConfigurationHandlerProvider?>(
                "com.google.idea.blaze.BlazeCommandRunConfigurationHandlerProvider"
            )
    }
}
