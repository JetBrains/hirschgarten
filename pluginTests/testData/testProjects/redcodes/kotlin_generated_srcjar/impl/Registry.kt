package com.example.impl

import com.example.spec.Bridge

object Registry {
  val bridges: List<Bridge> = listOf(BridgeImpl())
}
