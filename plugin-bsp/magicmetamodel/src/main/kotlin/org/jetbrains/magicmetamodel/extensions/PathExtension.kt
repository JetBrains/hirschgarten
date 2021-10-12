package org.jetbrains.magicmetamodel.extensions

import java.nio.file.Path

internal fun Path.allSubdirectoriesSequence(): Sequence<Path> =
  generateSequence(this) { it.parent }
