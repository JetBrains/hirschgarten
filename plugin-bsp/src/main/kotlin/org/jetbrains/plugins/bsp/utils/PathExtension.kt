package org.jetbrains.plugins.bsp.utils

import java.nio.file.Path

fun Path.allSubdirectoriesSequence(): Sequence<Path> = generateSequence(this) { it.parent }
