package com.example.kotlinlib

object KotlinHalf {
  private var counter = 0

  fun next(): Int = ++counter
}
