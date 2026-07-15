package org.jetbrains.bazel.languages.projectview

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class SectionKey<T>(val name: String, val default: T)
