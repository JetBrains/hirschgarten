package org.jetbrains.bazel.golang.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.OutputLocation
import java.nio.file.Path

@ApiStatus.Internal
fun Sequence<GoBuildTarget>.findGoSdkHome(resolveLocation: (OutputLocation) -> Path?): Path? =
  firstNotNullOfOrNull { goTarget -> goTarget.sdkHomePath?.let(resolveLocation) }
