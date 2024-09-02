package org.jetbrains.plugins.bsp.impl.magicmetamodel.extensions

import java.nio.file.Path

internal fun Path.allSubdirectoriesSequence(): Sequence<Path> = generateSequence(this) { it.parent }
