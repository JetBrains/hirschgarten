package org.jetbrains.bazel.clion.shared.run.debug

import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
@Serializable
enum class CcDebugInfoChoice {
  OPEN_PROJECT_VIEW,
  USE_DBG,
  DEBUG_ANYWAY,
  ALWAYS_DEBUG,
  CANCEL,
}
