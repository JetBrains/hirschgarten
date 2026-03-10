package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
data class TaskFinishParams(
  val taskId: TaskId,
  val eventTime: Long? = null,
  val message: String? = null,
  val status: BazelStatus,
  val data: TaskFinishData? = null,
)

@ApiStatus.Internal
sealed interface TaskFinishData

@ApiStatus.Internal
data class CompileReport(
  val target: Label,
  val errors: Int,
  val warnings: Int,
  val time: Long? = null,
  val noOp: Boolean? = null,
) : TaskFinishData

@ApiStatus.Internal
data class TestFinish(
  val displayName: String,
  val status: TestStatus,
  val message: String? = null,
  val location: Location? = null,
  val data: TestFinishData? = null,
) : TaskFinishData

@ApiStatus.Internal
sealed interface TestFinishData

@ApiStatus.Internal
data class JUnitStyleTestCaseData(
  val time: Double?,
  val className: String?,
  val errorMessage: String?,
  val output: String?,
  val errorType: String?,
) : TestFinishData

@ApiStatus.Internal
data class JUnitStyleTestSuiteData(
  val time: Double?,
  val systemOut: String?,
  val systemErr: String?,
) : TestFinishData
