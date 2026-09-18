package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class ProjectViewSectionKey<T>(@NlsSafe val name: String, val default: T)
