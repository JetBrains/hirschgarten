package org.jetbrains.bsp.bazel.commons

import ch.epfl.scala.bsp4j.StatusCode

// https://bazel.build/run/scripts#exit-codes
private const val SUCCESS_EXIT_CODE = 0
private const val BUILD_ERROR_EXIT_CODE = 1
private const val BAD_COMMAND_LINE_ARGUMENTS_EXIT_CODE = 2
private const val CANCEL_EXIT_CODE = 8
private const val OOM_EXIT_CODE = 33

enum class BazelStatus {
  SUCCESS,
  BAD_COMMAND_LINE_ARGUMENTS,
  BUILD_ERROR,
  CANCEL,
  OOM_ERROR,
  FATAL_ERROR, // for other non-categorized errors
  ;

  fun toBspStatusCode(): StatusCode =
    when (this) {
      SUCCESS -> StatusCode.OK
      CANCEL -> StatusCode.CANCELLED
      else -> StatusCode.ERROR
    }

  companion object {
    fun fromExitCode(exitCode: Int): BazelStatus =
      when (exitCode) {
        SUCCESS_EXIT_CODE -> SUCCESS
        BAD_COMMAND_LINE_ARGUMENTS_EXIT_CODE -> BAD_COMMAND_LINE_ARGUMENTS
        BUILD_ERROR_EXIT_CODE -> BUILD_ERROR
        CANCEL_EXIT_CODE -> CANCEL
        OOM_EXIT_CODE -> OOM_ERROR
        else -> FATAL_ERROR
      }

    fun combine(first: BazelStatus, second: BazelStatus): BazelStatus {
      if (first == OOM_ERROR || second == OOM_ERROR) {
        // OOM errors treated specially, so preserve them.
        return OOM_ERROR
      }
      return if (first.ordinal >= second.ordinal) first else second
    }
  }
}
