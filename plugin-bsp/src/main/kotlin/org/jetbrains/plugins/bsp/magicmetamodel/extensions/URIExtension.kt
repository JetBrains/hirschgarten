package org.jetbrains.plugins.bsp.magicmetamodel.extensions

import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal fun URI.toAbsolutePath(): Path =
  this.normalize()
    .toPath()
    .toAbsolutePath()
