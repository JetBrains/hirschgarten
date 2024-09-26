package org.jetbrains.bazel.sonatype

import java.io.IOException

sealed class SonatypeErrorCode {
  object STAGE_IN_PROGRESS : SonatypeErrorCode()
  object STAGE_FAILURE : SonatypeErrorCode()
  object BUNDLE_UPLOAD_FAILURE : SonatypeErrorCode()
  object MISSING_CREDENTIAL : SonatypeErrorCode()
  object MISSING_STAGING_PROFILE : SonatypeErrorCode()
  object MISSING_PROFILE : SonatypeErrorCode()
  object UNKNOWN_STAGE : SonatypeErrorCode()
  object MULTIPLE_TARGETS : SonatypeErrorCode()
}

class SonatypeException(val errorCode: SonatypeErrorCode, message: String) : IOException(message) {
  override fun toString(): String = "[$errorCode] $message"
}
