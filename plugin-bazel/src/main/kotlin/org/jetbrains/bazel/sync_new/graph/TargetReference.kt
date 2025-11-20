package org.jetbrains.bazel.sync_new.graph

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import kotlin.reflect.KProperty

interface TargetReference {
  val label: Label

  fun getBuildTarget(): BazelTargetVertex?
}

operator fun TargetReference.getValue(thisRef: Any?, property: KProperty<*>): BazelTargetVertex? = getBuildTarget()
