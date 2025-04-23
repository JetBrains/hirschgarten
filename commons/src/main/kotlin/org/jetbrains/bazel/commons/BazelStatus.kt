package org.jetbrains.bazel.commons

// https://bazel.build/run/scripts#exit-codes
private const val SUCCESS_EXIT_CODE = 0
private const val BUILD_ERROR_EXIT_CODE = 1
private const val BAD_COMMAND_LINE_ARGUMENTS_EXIT_CODE = 2
private const val CANCEL_EXIT_CODE = 8
private const val OOM_EXIT_CODE = 33

// specific code for bazel sync
private const val PARTIALLY_SUCCESS_WITH_KEEP_GOING = 3

enum class BazelStatus {
  SUCCESS,
  BAD_COMMAND_LINE_ARGUMENTS,
  BUILD_ERROR,
  CANCEL,
  OOM_ERROR,
  FATAL_ERROR, // for other non-categorized errors
  ;

  fun merge(anotherBazelStatus: BazelStatus): BazelStatus {
    if (this == OOM_ERROR || anotherBazelStatus == OOM_ERROR) {
      // OOM errors treated specially, so preserve them.
      return OOM_ERROR
    }
    return if (this.ordinal >= anotherBazelStatus.ordinal) this else anotherBazelStatus
  }

  companion object {
    fun fromExitCode(exitCode: Int): BazelStatus =
      when (exitCode) {
        SUCCESS_EXIT_CODE, PARTIALLY_SUCCESS_WITH_KEEP_GOING -> SUCCESS
        BAD_COMMAND_LINE_ARGUMENTS_EXIT_CODE -> BAD_COMMAND_LINE_ARGUMENTS
        BUILD_ERROR_EXIT_CODE -> BUILD_ERROR
        CANCEL_EXIT_CODE -> CANCEL
        OOM_EXIT_CODE -> OOM_ERROR
        else -> FATAL_ERROR
      }
  }
}
