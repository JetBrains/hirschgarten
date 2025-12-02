package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label

data class TaskFinishParams(
  val taskId: TaskId,
  val originId: String,
  val eventTime: Long? = null,
  val message: String? = null,
  val status: BazelStatus,
  val data: TaskFinishData? = null,
)

sealed interface TaskFinishData

data class CompileReport(
  val target: Label,
  val errors: Int,
  val warnings: Int,
  val time: Long? = null,
  val noOp: Boolean? = null,
) : TaskFinishData

data class TestFinish(
  val displayName: String,
  val status: TestStatus,
  val message: String? = null,
  val location: Location? = null,
  val data: TestFinishData? = null,
) : TaskFinishData

sealed interface TestFinishData

public data class JUnitStyleTestCaseData(
  val time: Double?,
  val className: String?,
  val errorMessage: String?,
  val output: String?,
  val errorType: String?,
) : TestFinishData

public data class JUnitStyleTestSuiteData(
  val time: Double?,
  val systemOut: String?,
  val systemErr: String?,
) : TestFinishData
