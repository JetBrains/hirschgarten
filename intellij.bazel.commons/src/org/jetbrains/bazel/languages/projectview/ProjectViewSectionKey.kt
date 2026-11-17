package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.ApiStatus

class ProjectViewSectionKey<T> @ApiStatus.Internal constructor(@NlsSafe val name: String, @get:ApiStatus.Internal val default: T)
