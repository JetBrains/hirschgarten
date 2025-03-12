/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.testmap

import com.google.idea.blaze.base.dependencies.TargetInfo
import org.jetbrains.bazel.ogRun.other.Kind

class TargetInfoComparator : Comparator<TargetInfo?> {
  /**
   * Sorts the [TargetInfo] objects such that there is a preference to those without the
   * underscore on the name and for those that do actually resolve to a [Kind].
   */
  override fun compare(o1: TargetInfo, o2: TargetInfo): Int {
    val kind1: Kind? = o1.getKind()
    val kind2: Kind? = o2.getKind()

    if ((null == kind1) != (null == kind2)) {
      return if (null == kind1) 1 else -1
    }

    val targetNameStr1 = o1.getLabel().targetName().toString()
    val targetNameStr2 = o2.getLabel().targetName().toString()

    val targetNameLeadingUnderscore1 = targetNameStr1.startsWith("_")
    val targetNameLeadingUnderscore2 = targetNameStr2.startsWith("_")

    if (targetNameLeadingUnderscore1 != targetNameLeadingUnderscore2) {
      return if (targetNameLeadingUnderscore1) 1 else -1
    }

    return targetNameStr1.compareTo(targetNameStr2)
  }
}
