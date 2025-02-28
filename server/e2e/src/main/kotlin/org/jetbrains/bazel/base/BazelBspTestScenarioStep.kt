package org.jetbrains.bazel.base

class BazelBspTestScenarioStep(private val testName: String, private val testkitCall: () -> Unit) {
  fun executeAndReturnResult(): Boolean {
    println("Executing \"$testName\"...")

    return try {
      testkitCall()
      println("Step \"$testName\" successful!")
      true
    } catch (e: Exception) {
      println("Step \"$testName\" failed!")
      e.printStackTrace()
      false
    }
  }
}
