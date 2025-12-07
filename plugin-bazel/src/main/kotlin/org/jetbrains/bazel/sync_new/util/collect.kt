package org.jetbrains.bazel.sync_new.util

import java.util.EnumSet

inline fun <reified T : Enum<T>> buildEnumSet(op: EnumSet<T>.() -> Unit) = EnumSet.noneOf(T::class.java).apply(op)
