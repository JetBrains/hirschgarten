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

import com.google.common.collect.ImmutableMultimap
import com.google.idea.blaze.base.model.primitives.Label
import org.jetbrains.bazel.label.Label
import java.util.function.Consumer

/** Results from a 'blaze test' invocation.  */
class BlazeTestResults private constructor(perTargetResults: ImmutableMultimap<Label?, BlazeTestResult?>?) {
    @JvmField
    val perTargetResults: ImmutableMultimap<Label?, BlazeTestResult?>?

    init {
        this.perTargetResults = perTargetResults
    }

    companion object {
        @JvmField
        val NO_RESULTS: BlazeTestResults = BlazeTestResults(ImmutableMultimap.of<Label?, BlazeTestResult?>())

        fun fromFlatList(results: MutableCollection<BlazeTestResult?>): BlazeTestResults? {
            if (results.isEmpty()) {
                return NO_RESULTS
            }
            val map: ImmutableMultimap.Builder<Label?, BlazeTestResult?> =
                ImmutableMultimap.builder<Label?, BlazeTestResult?>()
            results.forEach(Consumer { result: BlazeTestResult? -> map.put(result!!.label, result) })
            return BlazeTestResults(map.build())
        }
    }
}
