package org.jetbrains.plugins.bsp.server.client

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.Location
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TaskDataKind
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import ch.epfl.scala.bsp4j.TestStatus
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import ch.epfl.scala.bsp4j.CompileParams as BspCompileParams
import ch.epfl.scala.bsp4j.CompileReport as BspCompileReport
import ch.epfl.scala.bsp4j.CompileResult as BspCompileResult
import ch.epfl.scala.bsp4j.CompileTask as BspCompileTask
import ch.epfl.scala.bsp4j.RunParams as BspRunParams
import ch.epfl.scala.bsp4j.TaskId as BspTaskId
import ch.epfl.scala.bsp4j.TestFinish as BspTestFinish
import ch.epfl.scala.bsp4j.TestParams as BspTestParams
import ch.epfl.scala.bsp4j.TestReport as BspTestReport
import ch.epfl.scala.bsp4j.TestStart as BspTestStart
import ch.epfl.scala.bsp4j.TestTask as BspTestTask
import ch.epfl.scala.bsp4j.TestResult as BspTestResult
import ch.epfl.scala.bsp4j.RunResult as BspRunResult

public data class OriginId(val id: String) {
  public fun toTaskId(): ClientTaskId = ClientTaskId(id)
}

public data class ClientTaskId(val id: String) {
  public fun toOriginId(): OriginId = OriginId(id)
}

private data class ResultFuture(val future: CompletableFuture<Unit>)

private sealed class Task<StartedData, ProgressData, FinishedData, ClientTopLevelResult>(
  val resultFuture: ResultFuture,
  val observer: TaskObserver<StartedData, ProgressData, FinishedData, ClientTopLevelResult>,
) {
  val subtasks: MutableSet<ClientTaskId> =
    mutableSetOf() // Remember the subtasks of this task and remove them when the task is finished
}

public fun BspCompileResult.toClient(): ClientCompileResult =
  ClientCompileResult(
    statusCode = statusCode,
  )

public fun BspTestResult.toClient(): ClientTestResult =
  ClientTestResult(
    statusCode = statusCode,
  )

public fun BspRunResult.toClient(): ClientRunResult =
  ClientRunResult(
    statusCode = statusCode,
  )


public class Subtask(public val originId: OriginId, public val subtasks: MutableSet<ClientTaskId>)

public data class ClientCompileTaskParams(val targets: List<BuildTargetIdentifier>, val arguments: List<String>) {
  public fun toProtocol(originId: OriginId): BspCompileParams =
    BspCompileParams(targets).apply {
      this.originId = originId.id
      this.arguments = arguments
    }
}

public data class ClientTestTaskParams(
  val targets: List<BuildTargetIdentifier>,
  val arguments: List<String>,
  // TODO: data
) {
  public fun toProtocol(originId: OriginId): BspTestParams =
    BspTestParams(targets).apply {
      this.originId = originId.id
      this.arguments = arguments
    }
}

public data class ClientRunTaskParams(
  val target: BuildTargetIdentifier,
  val arguments: List<String>,
  // TODO: data
) {
  public fun toProtocol(originId: OriginId): BspRunParams =
    BspRunParams(target).apply {
      this.originId = originId.id
      this.arguments = arguments
    }
}

public data class ClientTaskStartedParams<Data>(
  public val taskId: ClientTaskId,
  public val parentTaskId: ClientTaskId,
  public val eventTime: Instant?,
  public val message: String?,
  public val data: Data?
) {
  public companion object {
    public fun withoutData(
      taskId: ClientTaskId,
      parentTaskId: ClientTaskId,
      params: TaskStartParams
    ): ClientTaskStartedParams<Nothing> {
      return ClientTaskStartedParams(
        taskId = taskId,
        parentTaskId = parentTaskId,
        eventTime = params.eventTime?.let { Instant.ofEpochMilli(it) },
        message = params.message,
        data = null
      )
    }
  }

  public fun <T> withData(data: T): ClientTaskStartedParams<T> =
    ClientTaskStartedParams(
      taskId = taskId,
      parentTaskId = parentTaskId,
      eventTime = eventTime,
      message = message,
      data = data
    )
}

public data class ClientCompileTaskData(
  val target: BuildTargetIdentifier
) {
  public constructor(data: BspCompileTask) : this(
    target = data.target
  )
}

public sealed interface ClientTestTaskStartedData

public sealed interface ClientTestTaskFinishedData

public data class ClientTestTaskData(
  val target: BuildTargetIdentifier
) : ClientTestTaskStartedData {
  public constructor(data: BspTestTask) : this(
    target = data.target
  )
}

public data class ClientTestReportData(
  val target: BuildTargetIdentifier,
  val passed: Int,
  val failed: Int,
  val ignored: Int,
  val cancelled: Int,
  val skipped: Int,
  val time: Duration?
) : ClientTestTaskFinishedData {
  public constructor(data: BspTestReport) : this(
    target = data.target,
    passed = data.passed,
    failed = data.failed,
    ignored = data.ignored,
    cancelled = data.cancelled,
    skipped = data.skipped,
    time = data.time?.let { Duration.ofMillis(it) }
  )
}

public data class ClientTestStartData(
  val displayName: String,
  val location: Location?,
) : ClientTestTaskStartedData {
  public constructor(data: BspTestStart) : this(
    displayName = data.displayName,
    location = data.location
  )
}

public data class ClientTestFinishData(
  val displayName: String,
  val message: String?,
  val status: TestStatus,
  val location: Location?,
  // TODO: additional data
) : ClientTestTaskFinishedData {
  public constructor(data: BspTestFinish) : this(
    displayName = data.displayName,
    message = data.message,
    status = data.status,
    location = data.location
  )
}

public data class ClientTaskProgressParams<Data>(
  public val taskId: ClientTaskId,
  public val parentTaskId: ClientTaskId,
  public val eventTime: Instant?,
  public val message: String?,
  public val total: Long?,
  public val progress: Long?,
  public val unit: String?,
  public val data: Data?
) {
  public companion object {
    public fun withoutData(
      taskId: ClientTaskId,
      parentTaskId: ClientTaskId,
      params: TaskProgressParams
    ): ClientTaskProgressParams<Nothing> {
      return ClientTaskProgressParams(
        taskId = taskId,
        parentTaskId = parentTaskId,
        eventTime = params.eventTime?.let { Instant.ofEpochMilli(it) },
        message = params.message,
        total = params.total,
        progress = params.progress,
        unit = params.unit,
        data = null
      )
    }
  }

  public fun <T> withData(data: T): ClientTaskProgressParams<T> =
    ClientTaskProgressParams(
      taskId = taskId,
      parentTaskId = parentTaskId,
      eventTime = eventTime,
      message = message,
      total = total,
      progress = progress,
      unit = unit,
      data = data
    )
}

public data class ClientTaskFinishedParams<Data>(
  public val taskId: ClientTaskId,
  public val parentTaskId: ClientTaskId,
  public val eventTime: Instant?,
  public val message: String?,
  public val statusCode: StatusCode,
  public val data: Data?
) {
  public companion object {
    public fun withoutData(
      taskId: ClientTaskId,
      parentTaskId: ClientTaskId,
      params: TaskFinishParams
    ): ClientTaskFinishedParams<Nothing> =
      ClientTaskFinishedParams(
        taskId = taskId,
        parentTaskId = parentTaskId,
        eventTime = params.eventTime?.let { Instant.ofEpochMilli(it) },
        message = params.message,
        statusCode = params.status,
        data = null
      )
  }

  public fun <T> withData(data: T): ClientTaskFinishedParams<T> =
    ClientTaskFinishedParams(
      taskId = taskId,
      parentTaskId = parentTaskId,
      eventTime = eventTime,
      message = message,
      statusCode = statusCode,
      data = data
    )
}

public data class ClientCompileReportData(
  val target: BuildTargetIdentifier,
  val errors: Int,
  val warnings: Int,
  val time: Duration?,
  val noOp: Boolean?
) {
  public constructor(data: BspCompileReport) : this(
    target = data.target,
    errors = data.errors,
    warnings = data.warnings,
    time = data.time?.let { Duration.ofMillis(it) },
    noOp = data.noOp
  )
}


public data class ClientCompileResult(val statusCode: StatusCode) // TODO can have additional data

public data class ClientTestResult(val statusCode: StatusCode) // TODO can have additional data

public data class ClientRunResult(val statusCode: StatusCode) // TODO can have additional data

private class CompileTask(resultFuture: ResultFuture, observer: CompileTaskObserver) :
  Task<ClientCompileTaskData, Nothing, ClientCompileReportData, ClientCompileResult>(
    resultFuture = resultFuture,
    observer = observer,
  )

private class TestTask(resultFuture: ResultFuture, observer: TestTaskObserver) :
  Task<ClientTestTaskStartedData, Nothing, ClientTestTaskFinishedData, ClientTestResult>(
    resultFuture = resultFuture,
    observer = observer,
  )

private class RunTask(resultFuture: ResultFuture, observer: RunTaskObserver) :
  Task<Nothing, Nothing, Nothing, ClientRunResult>(
    resultFuture = resultFuture,
    observer = observer,
  )

public interface TaskObserver<StartedData, ProgressData, FinishedData, TopLevelResult> {
  public fun onTaskStarted(params: ClientTaskStartedParams<out StartedData>)
  public fun onTaskProgress(params: ClientTaskProgressParams<out ProgressData>)
  public fun onTaskFinished(params: ClientTaskFinishedParams<out FinishedData>)
  public fun onTopLevelTaskFinished(params: TopLevelResult)
  public fun onTopLevelTaskFailed(throwable: Throwable)

  @Suppress("UNCHECKED_CAST")
  public fun asGeneric(): TaskObserver<Nothing, Nothing, Nothing, Nothing> =
    this as TaskObserver<Nothing, Nothing, Nothing, Nothing>
}

public interface TaskHandle {
  public val originId: OriginId
  public fun cancel()
}

public sealed class TaskError : Throwable()

public data class NoParent(val taskId: ClientTaskId) : TaskError()

public data class SubtaskAlreadyStarted(val taskId: ClientTaskId, val originId: OriginId) : TaskError()
public data class OriginNotFound(val taskId: ClientTaskId, val originId: OriginId) : TaskError()

public data class SubtaskNotFound(val taskId: ClientTaskId, val originId: OriginId) : TaskError()

/**
 * This subtask is not a child of its parent task.
 * Task already finished or never started */

public data class IncorrectSubtaskParent(
  val taskId: ClientTaskId,
  val parentTaskId: ClientTaskId,
  val originId: OriginId
) : TaskError()

public data class DeserializationError(val taskId: ClientTaskId, val error: Throwable) : TaskError()

public data class WrongTaskType(val taskId: ClientTaskId, val taskType: String) : TaskError()
public data class UnsupportedDataKind(val taskId: ClientTaskId, val kind: String) : TaskError()

private typealias ErasedTask = Task<*, *, *, *>

public class TaskClient(private val server: BuildServer) {
  private val tasks: MutableMap<OriginId, ErasedTask> = mutableMapOf()
  private val subtasks: MutableMap<ClientTaskId, Subtask> = mutableMapOf()

  private fun nextOriginId(): OriginId = OriginId(UUID.randomUUID().toString())

  @Synchronized
  private fun removeSubtasksRecursively(taskId: ClientTaskId) {
    subtasks[taskId]?.subtasks?.forEach { removeSubtasksRecursively(it) }
    subtasks.remove(taskId)
  }

  @Synchronized
  private fun finishTask(originId: OriginId) {
    val task = tasks[originId]
    if (task != null) {
      task.subtasks.forEach { removeSubtasksRecursively(it) }
      tasks.remove(originId)
    }
  }


  private fun <Result, ClientResult> handleResultFuture(
    originId: OriginId,
    future: CompletableFuture<Result>,
    observer: TaskObserver<*, *, *, ClientResult>,
    resultTransformer: (Result) -> ClientResult
  ): ResultFuture =
    ResultFuture(future.handle { result, error ->
      if (error != null) {
        observer.onTopLevelTaskFailed(error)
      } else {
        observer.onTopLevelTaskFinished(resultTransformer(result))
      }
      finishTask(originId)
    })

  private fun <ClientParams, Params, StartedData, ProgressData, FinishedData, Result, ClientResult> startTask(
    params: ClientParams,
    paramsTransformer: (ClientParams, OriginId) -> Params,
    observer: TaskObserver<StartedData, ProgressData, FinishedData, ClientResult>,
    taskStarter: BuildServer.(Params) -> CompletableFuture<Result>,
    resultTransformer: (Result) -> ClientResult,
    taskConstructor: (ResultFuture, TaskObserver<StartedData, ProgressData, FinishedData, ClientResult>) -> ErasedTask
  ): TaskHandle {
    val originId = nextOriginId()
    val bspParams = paramsTransformer(params, originId)
    val future = taskStarter(server, bspParams)
    val resultFuture = handleResultFuture(originId, future, observer, resultTransformer)
    val task = taskConstructor(resultFuture, observer)
    tasks[originId] = task
    val handle = object : TaskHandle {
      override val originId: OriginId = originId

      override fun cancel() {
        future.cancel(true)
      }
    }
    return handle
  }

  public fun startCompileTask(clientParams: ClientCompileTaskParams, observer: CompileTaskObserver): TaskHandle {
    return startTask(
      params = clientParams,
      paramsTransformer = { params, originId -> params.toProtocol(originId) },
      observer = observer,
      taskStarter = BuildServer::buildTargetCompile,
      resultTransformer = { it.toClient() },
      taskConstructor = ::CompileTask
    )
  }

  public fun startTestTask(clientParams: ClientTestTaskParams, observer: TestTaskObserver): TaskHandle {
    return startTask(
      params = clientParams,
      paramsTransformer = { params, originId -> params.toProtocol(originId) },
      observer = observer,
      taskStarter = BuildServer::buildTargetTest,
      resultTransformer = { it.toClient() },
      taskConstructor = ::TestTask
    )
  }

  public fun startRunTask(clientParams: ClientRunTaskParams, observer: RunTaskObserver): TaskHandle {
    return startTask(
      params = clientParams,
      paramsTransformer = { params, originId -> params.toProtocol(originId) },
      observer = observer,
      taskStarter = BuildServer::buildTargetRun,
      resultTransformer = { it.toClient() },
      taskConstructor = ::RunTask
    )
  }

  private data class InternalTaskData(
    val taskId: ClientTaskId,
    val parentTaskId: ClientTaskId,
    val parentSubtask: Subtask?,
    val originId: OriginId,
    val originTask: ErasedTask
  )

  private data class TaskData(val taskId: ClientTaskId, val parentTaskId: ClientTaskId, val originTask: ErasedTask)

  @Synchronized
  private fun getTaskDataForId(bspTaskId: BspTaskId): InternalTaskData {
    val taskId = ClientTaskId(bspTaskId.id)

    // TODO: Support multiple parents
    val parentTaskId = bspTaskId.parents?.firstOrNull()?.let { ClientTaskId(it) } ?: run {
      throw NoParent(taskId)
    }

    val parentTask = subtasks[parentTaskId]

    val originId = parentTask?.originId ?: parentTaskId.toOriginId()

    val originTask = tasks[originId] ?: run {
      throw OriginNotFound(taskId, originId)
    }

    return InternalTaskData(taskId, parentTaskId, parentTask, originId, originTask)
  }

  @Synchronized
  private fun addSubtask(bspTaskId: BspTaskId): TaskData {
    val (taskId, parentTaskId, parentSubTask, originId, originTask) = getTaskDataForId(bspTaskId)

    if (subtasks.containsKey(taskId)) {
      throw SubtaskAlreadyStarted(taskId, originId)
    }

    subtasks[taskId] = Subtask(originId, mutableSetOf())

    if (parentSubTask == null) {
      originTask.subtasks.add(taskId)
    } else {
      parentSubTask.subtasks.add(taskId)
    }

    return TaskData(taskId, parentTaskId, originTask)
  }

  @Synchronized
  private fun getSubtask(bspTaskId: BspTaskId): TaskData {
    val (taskId, parentTaskId, parentTask, originId, originTask) = getTaskDataForId(bspTaskId)

    if (!subtasks.containsKey(taskId)) {
      throw SubtaskNotFound(taskId, originId)
    }

    if (parentTask != null) {
      if (!parentTask.subtasks.contains(taskId)) {
        throw IncorrectSubtaskParent(taskId, parentTaskId, originId)
      }
    } else if (!originTask.subtasks.contains(taskId)) {
      throw IncorrectSubtaskParent(taskId, originId.toTaskId(), originId)
    }
    return TaskData(taskId, parentTaskId, originTask)
  }

  @Synchronized
  private fun finishSubtask(bspTaskId: BspTaskId): TaskData {
    val (taskId, parentTaskId, _, originId, originTask) = getTaskDataForId(bspTaskId)

    if (!subtasks.containsKey(taskId)) {
      throw SubtaskNotFound(taskId, originId)
    }

    removeSubtasksRecursively(taskId)
    subtasks.remove(taskId)
    originTask.subtasks.remove(taskId)
    return TaskData(taskId, parentTaskId, originTask)
  }

  public inner class BspClientListener {
    private val gson = Gson()

    public fun onBuildTaskStart(params: TaskStartParams) {

      val (taskId, parentTaskId, originTask) = addSubtask(params.taskId)

      val clientGenericTaskStartedParams = ClientTaskStartedParams.withoutData(taskId, parentTaskId, params)

      when (params.dataKind) {
        null, "" -> {
          originTask.observer.asGeneric().onTaskStarted(clientGenericTaskStartedParams)
        }

        TaskDataKind.COMPILE_TASK
        -> {
          // TODO: Extract?
          val compileTask = try {
            gson.fromJson(params.data as JsonObject, BspCompileTask::class.java)
          } catch (e: Exception) {
            throw DeserializationError(taskId, e)
          }

          val clientCompileTaskStartedParams =
            clientGenericTaskStartedParams.withData(ClientCompileTaskData(compileTask))

          val compileTaskObserver = (originTask as? CompileTask)?.observer ?: run {
            throw WrongTaskType(taskId, TaskDataKind.COMPILE_TASK)
          }

          compileTaskObserver.onTaskStarted(clientCompileTaskStartedParams)
        }

        TaskDataKind.TEST_TASK
        -> {
          val testTask = try {
            gson.fromJson(params.data as JsonObject, BspTestTask::class.java)
          } catch (e: Exception) {
            throw DeserializationError(taskId, e)
          }

          val clientTestTaskStartedParams =
            clientGenericTaskStartedParams.withData(ClientTestTaskData(testTask))

          val testTaskObserver = (originTask as? TestTask)?.observer ?: run {
            throw WrongTaskType(taskId, TaskDataKind.TEST_TASK)
          }

          testTaskObserver.onTaskStarted(clientTestTaskStartedParams)
        }

        TaskDataKind.TEST_START
        -> {
          val testStart = try {
            gson.fromJson(params.data as JsonObject, BspTestStart::class.java)
          } catch (e: Exception) {
            throw DeserializationError(taskId, e)
          }

          val clientTestStartParams =
            clientGenericTaskStartedParams.withData(ClientTestStartData(testStart))

          val testTaskObserver = (originTask as? TestTask)?.observer ?: run {
            throw WrongTaskType(taskId, TaskDataKind.TEST_START)
          }

          testTaskObserver.onTaskStarted(clientTestStartParams)
        }

        else -> {
          throw UnsupportedDataKind(taskId, params.dataKind)
        }
      }
    }

    public fun onBuildTaskProgress(params: TaskProgressParams) {
      val (taskId, parentTaskId, originTask) = getSubtask(params.taskId)

      val clientGenericTaskProgressParams = ClientTaskProgressParams.withoutData(
        taskId,
        parentTaskId,
        params
      )

      when (params.dataKind) {
        null, "" -> {
          originTask.observer.asGeneric().onTaskProgress(clientGenericTaskProgressParams)
        }

        else -> {
          throw UnsupportedDataKind(taskId, params.dataKind)
        }
      }
    }

    public fun onBuildTaskFinish(params: TaskFinishParams) {
      val (taskId, parentTaskId, originTask) = finishSubtask(params.taskId)

      val clientGenericTaskFinishedParams = ClientTaskFinishedParams.withoutData(
        taskId,
        parentTaskId,
        params
      )

      when (params.dataKind) {
        null, "" -> {
          originTask.observer.asGeneric().onTaskFinished(clientGenericTaskFinishedParams)
        }

        TaskDataKind.COMPILE_REPORT -> {
          val compileReport = try {
            gson.fromJson(params.data as JsonObject, BspCompileReport::class.java)
          } catch (e: Exception) {
            throw DeserializationError(taskId, e)
          }

          val clientCompileTaskFinishedParams = clientGenericTaskFinishedParams.withData(
            ClientCompileReportData(compileReport)
          )

          val compileTaskObserver = (originTask as? CompileTask)?.observer ?: run {
            throw WrongTaskType(taskId, TaskDataKind.COMPILE_REPORT)
          }

          compileTaskObserver.onTaskFinished(clientCompileTaskFinishedParams)
        }

        TaskDataKind.TEST_REPORT -> {
          val testReport = try {
            gson.fromJson(params.data as JsonObject, BspTestReport::class.java)
          } catch (e: Exception) {
            throw DeserializationError(taskId, e)
          }

          val clientTestTaskFinishedParams = clientGenericTaskFinishedParams.withData(
            ClientTestReportData(testReport)
          )

          val testTaskObserver = (originTask as? TestTask)?.observer ?: run {
            throw WrongTaskType(taskId, TaskDataKind.TEST_REPORT)
          }

          testTaskObserver.onTaskFinished(clientTestTaskFinishedParams)
        }

        TaskDataKind.TEST_FINISH -> {
          val testFinish = try {
            gson.fromJson(params.data as JsonObject, BspTestFinish::class.java)
          } catch (e: Exception) {
            throw DeserializationError(taskId, e)
          }

          val clientTestFinishParams = clientGenericTaskFinishedParams.withData(
            ClientTestFinishData(testFinish)
          )

          val testTaskObserver = (originTask as? TestTask)?.observer ?: run {
            throw WrongTaskType(taskId, TaskDataKind.TEST_FINISH)
          }

          testTaskObserver.onTaskFinished(clientTestFinishParams)
        }

        else -> {
          throw UnsupportedDataKind(taskId, params.dataKind)
        }
      }
    }
  }
}

public typealias CompileTaskObserver = TaskObserver<ClientCompileTaskData, Nothing, ClientCompileReportData, ClientCompileResult>

public typealias TestTaskObserver = TaskObserver<ClientTestTaskStartedData, Nothing, ClientTestTaskFinishedData, ClientTestResult>

public typealias RunTaskObserver = TaskObserver<Nothing, Nothing, Nothing, ClientRunResult>