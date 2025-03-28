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
package org.jetbrains.bazel.commons

import java.nio.file.Paths

/** Represents a blaze-produced artifact.  */
class ArtifactLocation
private constructor(
    val rootExecutionPathFragment: String,
    /**
     * The root-relative path. For external workspace artifacts, this is relative to the external
     * workspace root.
     */
    val relativePath: String, val isSource: Boolean, val isExternal: Boolean
) : Comparable<ArtifactLocation> {
    val isGenerated: Boolean
        get() = !this.isSource

    val isMainWorkspaceSourceArtifact: Boolean
        /** Returns false for generated or external artifacts  */
        get() = this.isSource && !this.isExternal

    val executionRootRelativePath: String
        /** For main-workspace source artifacts, this is simply the workspace-relative path.  */
        get() = Paths.get(this.rootExecutionPathFragment, this.relativePath).toString()

    override fun toString(): String {
        return this.executionRootRelativePath
    }

    override fun compareTo(o: ArtifactLocation): Int {
        return com.google.common.collect.ComparisonChain.start()
            .compare(this.rootExecutionPathFragment, o.rootExecutionPathFragment)
            .compare(this.relativePath, o.relativePath)
            .compareFalseFirst(this.isSource, o.isSource)
            .compareFalseFirst(this.isExternal, o.isExternal)
            .result()
    }
}
