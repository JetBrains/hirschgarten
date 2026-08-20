package org.jetbrains.bazel.languages.starlark.index

import com.intellij.util.indexing.ID
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object StarlarkLoadEdgesIndex {
  val NAME: ID<String, Collection<String>> = ID.create("starlark.load.edges")
}
