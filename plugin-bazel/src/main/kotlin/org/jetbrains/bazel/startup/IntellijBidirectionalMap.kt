package org.jetbrains.bazel.startup

import org.jetbrains.bazel.commons.BidirectionalMap
import com.intellij.util.containers.BidirectionalMap as IJBidirectionalMap

class IntellijBidirectionalMap<K, V> private constructor(private val delegate: IJBidirectionalMap<K, V>) :
  BidirectionalMap<K, V>,
  MutableMap<K, V> by delegate {
    constructor() : this(IJBidirectionalMap<K, V>())

  override fun getKeysByValue(value: V): List<K> = delegate.getKeysByValue(value) ?: emptyList()
  }
