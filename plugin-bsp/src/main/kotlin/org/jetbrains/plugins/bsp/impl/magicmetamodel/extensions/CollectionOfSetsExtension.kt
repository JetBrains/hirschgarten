package org.jetbrains.plugins.bsp.impl.magicmetamodel.extensions

internal fun <T> Collection<Set<T>>.reduceSets(): Set<T> = this.fold(emptySet()) { acc, el -> acc.union(el) }
