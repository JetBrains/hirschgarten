package org.jetbrains.plugins.bsp.server.client

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TaskDataKind
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import ch.epfl.scala.bsp4j.CompileParams as BspCompileParams
import ch.epfl.scala.bsp4j.CompileReport as BspCompileReport
import ch.epfl.scala.bsp4j.CompileResult as BspCompileResult
import ch.epfl.scala.bsp4j.CompileTask as BspCompileTask
import ch.epfl.scala.bsp4j.TaskId as BspTaskId

public data class OriginId(val id: String) {
  public fun toTaskId(): ClientTaskId = ClientTaskId(id)
}

public data class ClientTaskId(val id: String) {
  public fun toOriginId(): OriginId = OriginId(id)
}

// / An action is a top-level task started by the user, such as a build or a test run.
// / Actions can fail or be cancelled, but they cannot be nested.
// / They can have, however, any number of subtasks.

public interface Task {
  public val observer: TaskObserver
  public val subtasks: MutableSet<ClientTaskId> // / Remember the subtasks of this task and remove them when the task is finished
  public fun cancel()
}

public fun BspCompileResult.toClient(): ClientTopLevelCompileTaskFinishedParams =
  ClientTopLevelCompileTaskFinishedParams(
    statusCode = statusCode,
  )

private class CompileTask(
  private val resultFuture: CompletableFuture<BspCompileResult>,
  public override val observer: CompileTaskObserver
) : Task {
  init {
    resultFuture.whenComplete { result, error ->
      if (error != null) {
        observer.onTopLevelTaskFailed(error)
      } else {
        observer.onTopLevelCompileTaskFinished(result.toClient())
      }
    }
  }

  public override val subtasks: MutableSet<ClientTaskId> = mutableSetOf()
  override fun cancel() {
    resultFuture.cancel(true)
  }
}

public class SubTask(public val originId: OriginId, public val subtasks: MutableSet<ClientTaskId>)

public data class ClientCompileTaskParams(val targets: List<BuildTargetIdentifier>, val arguments: List<String>) {
  public fun toProtocol(originId: OriginId): BspCompileParams =
    BspCompileParams(targets).apply {
      this.originId = originId.id
      this.arguments = arguments
    }
}

public sealed interface ClientTaskStartedParams {
  public val taskId: ClientTaskId
  public val parentTaskId: ClientTaskId
  public val eventTime: Instant?
  public val message: String?
}

public data class ClientGenericTaskStartedParams(
  override val taskId: ClientTaskId,
  override val parentTaskId: ClientTaskId,
  override val eventTime: Instant?,
  override val message: String?
) : ClientTaskStartedParams {
  public constructor(taskId: ClientTaskId, parentTaskId: ClientTaskId, params: TaskStartParams) : this(
    taskId = taskId,
    parentTaskId = parentTaskId,
    eventTime = params.eventTime?.let { Instant.ofEpochMilli(it) },
    message = params.message
  )
}

public data class ClientCompileTaskStartedParams(
  override val taskId: ClientTaskId,
  override val parentTaskId: ClientTaskId,
  override val eventTime: Instant?,
  override val message: String?,
  val target: BuildTargetIdentifier
) : ClientTaskStartedParams {
  public constructor(params: ClientGenericTaskStartedParams, data: BspCompileTask) : this(
    taskId = params.taskId,
    parentTaskId = params.parentTaskId,
    eventTime = params.eventTime,
    message = params.message,
    target = data.target
  )
}

public sealed interface ClientTaskProgressParams {
  public val taskId: ClientTaskId
  public val parentTaskId: ClientTaskId
  public val eventTime: Instant?
  public val message: String?
  public val total: Long?
  public val progress: Long?
  public val unit: String?
}

public data class ClientGenericTaskProgressParams(
  override val taskId: ClientTaskId,
  override val parentTaskId: ClientTaskId,
  override val eventTime: Instant?,
  override val message: String?,
  override val total: Long?,
  override val progress: Long?,
  override val unit: String?
) : ClientTaskProgressParams {
  public constructor(taskId: ClientTaskId, parentTaskId: ClientTaskId, params: TaskProgressParams) : this(
    taskId = taskId,
    parentTaskId = parentTaskId,
    eventTime = params.eventTime?.let { Instant.ofEpochMilli(it) },
    message = params.message,
    total = params.total,
    progress = params.progress,
    unit = params.unit
  )
}

public sealed interface ClientTaskFinishedParams {
  public val taskId: ClientTaskId
  public val parentTaskId: ClientTaskId
  public val eventTime: Instant?
  public val message: String?
  public val statusCode: StatusCode
}

public data class ClientCompileTaskFinishedParams(
  override val taskId: ClientTaskId,
  override val parentTaskId: ClientTaskId,
  override val eventTime: Instant?,
  override val message: String?,
  override val statusCode: StatusCode,
  val target: BuildTargetIdentifier,
  val errors: Int,
  val warnings: Int,
  val time: Duration?,
  val noOp: Boolean?
) : ClientTaskFinishedParams {
  public constructor(params: ClientGenericTaskFinishedParams, data: BspCompileReport) : this(
    taskId = params.taskId,
    parentTaskId = params.parentTaskId,
    eventTime = params.eventTime,
    message = params.message,
    statusCode = params.statusCode,
    target = data.target,
    errors = data.errors,
    warnings = data.warnings,
    time = data.time?.let { Duration.ofMillis(it) },
    noOp = data.noOp
  )
}

public data class ClientGenericTaskFinishedParams(
  override val taskId: ClientTaskId,
  override val parentTaskId: ClientTaskId,
  override val eventTime: Instant?,
  override val message: String?,
  override val statusCode: StatusCode,
) : ClientTaskFinishedParams {
  public constructor(taskId: ClientTaskId, parentTaskId: ClientTaskId, params: TaskFinishParams) : this(
    taskId = taskId,
    parentTaskId = parentTaskId,
    eventTime = params.eventTime?.let { Instant.ofEpochMilli(it) },
    message = params.message,
    statusCode = params.status
  )
}

public data class ClientTopLevelCompileTaskFinishedParams(val statusCode: StatusCode) // can have additional data
public interface CompileTaskObserver : TaskObserver {
  public fun onCompileTaskStarted(params: ClientCompileTaskStartedParams)

  public fun onCompileTaskFinished(params: ClientCompileTaskFinishedParams)

  public fun onTopLevelCompileTaskFinished(params: ClientTopLevelCompileTaskFinishedParams)
}

public sealed interface TaskObserver {
  public fun onTaskStarted(params: ClientGenericTaskStartedParams)
  public fun onTaskProgress(params: ClientGenericTaskProgressParams)
  public fun onTaskFinished(params: ClientGenericTaskFinishedParams)
  public fun onTopLevelTaskFailed(throwable: Throwable)
}

public interface TaskHandle {
  public val originId: OriginId
  public fun cancel()
}

public sealed class TaskError : Throwable()

public data class NoParent(val taskId: ClientTaskId) : TaskError()
public data class OriginNotFound(val taskId: ClientTaskId, val originId: OriginId) : TaskError()

// / Task already finished or never started
public data class IncorrectSubtask(val taskId: ClientTaskId, val originId: OriginId) : TaskError()
public data class DeserializationError(val taskId: ClientTaskId, val error: Throwable) : TaskError()

public data class WrongTaskType(val taskId: ClientTaskId, val taskType: String) : TaskError()
public data class UnsupportedDataKind(val taskId: ClientTaskId, val kind: String) : TaskError()

// TODO: Need to check for server-side cancellation.
public class TaskClient(private val server: BuildServer) {
  private val tasks: MutableMap<OriginId, Task> = mutableMapOf()
  private val subtasks: MutableMap<ClientTaskId, SubTask> = mutableMapOf()

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

  @Synchronized
  private fun cancelTask(originId: OriginId) {
    tasks[originId]?.cancel()
    finishTask(originId)
  }

  public fun startCompileTask(params: ClientCompileTaskParams, observer: CompileTaskObserver): TaskHandle {
    val originId = nextOriginId()
    val serverCompileTaskParams = params.toProtocol(originId)
    val resultFuture = server.buildTargetCompile(serverCompileTaskParams)
    val task = CompileTask(resultFuture, observer)
    tasks[originId] = task
    val handle = object : TaskHandle {
      override val originId: OriginId = originId

      override fun cancel() {
        cancelTask(originId)
      }
    }
    return handle
  }

  private data class InternalTaskData(
    val taskId: ClientTaskId,
    val parentTaskId: ClientTaskId,
    val parentSubTask: SubTask?,
    val originId: OriginId,
    val originTask: Task
  )

  private data class TaskData(val taskId: ClientTaskId, val parentTaskId: ClientTaskId, val originTask: Task)

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

    if (parentSubTask == null) {
      originTask.subtasks.add(taskId)
    } else {
      parentSubTask.subtasks.add(taskId)
    }

    subtasks[taskId] = SubTask(originId, mutableSetOf())

    return TaskData(taskId, parentTaskId, originTask)
  }

  @Synchronized
  private fun getSubtask(bspTaskId: BspTaskId): TaskData {
    val (taskId, parentTaskId, _, originId, originTask) = getTaskDataForId(bspTaskId)
    if (!originTask.subtasks.contains(taskId)) {
      // TODO: Task already finished or never started
      throw IncorrectSubtask(taskId, originId)
    }
    return TaskData(taskId, parentTaskId, originTask)
  }

  @Synchronized
  private fun finishSubtask(bspTaskId: BspTaskId): TaskData {
    val (taskId, parentTaskId, _, originId, originTask) = getTaskDataForId(bspTaskId)

    removeSubtasksRecursively(taskId)
    subtasks.remove(taskId)
    originTask.subtasks.remove(taskId)
    return TaskData(taskId, parentTaskId, originTask)
  }

  public inner class BspClientListener {
    private val gson = Gson()

    public fun onBuildTaskStart(params: TaskStartParams) {

      val (taskId, parentTaskId, originTask) = addSubtask(params.taskId)

      val clientGenericTaskStartedParams = ClientGenericTaskStartedParams(taskId, parentTaskId, params)

      when (params.dataKind) {
        null, "" -> {
          originTask.observer.onTaskStarted(clientGenericTaskStartedParams)
        }

        TaskDataKind.COMPILE_TASK
        -> {
          // TODO: Extract
          val compileTask = try {
            gson.fromJson(params.data as JsonObject, BspCompileTask::class.java)
          } catch (e: Exception) {
            throw DeserializationError(taskId, e)
          }

          val clientCompileTaskStartedParams = ClientCompileTaskStartedParams(
            clientGenericTaskStartedParams,
            compileTask
          )

          val compileTaskObserver = originTask.observer as? CompileTaskObserver ?: run {
            throw WrongTaskType(taskId, TaskDataKind.COMPILE_TASK)
          }

          compileTaskObserver.onCompileTaskStarted(clientCompileTaskStartedParams)
        }

        else -> {
          throw UnsupportedDataKind(taskId, params.dataKind)
        }
      }
    }

    public fun onBuildTaskProgress(params: TaskProgressParams) {
      val (taskId, parentTaskId, originTask) = getSubtask(params.taskId)

      val clientGenericTaskProgressParams = ClientGenericTaskProgressParams(
        taskId,
        parentTaskId,
        params
      )

      when (params.dataKind) {
        null, "" -> {
          originTask.observer.onTaskProgress(clientGenericTaskProgressParams)
        }

        else -> {
          throw UnsupportedDataKind(taskId, params.dataKind)
        }
      }
    }

    public fun onBuildTaskFinish(params: TaskFinishParams) {
      val (taskId, parentTaskId, originTask) = finishSubtask(params.taskId)

      val clientGenericTaskFinishedParams = ClientGenericTaskFinishedParams(
        taskId,
        parentTaskId,
        params
      )

      when (params.dataKind) {
        null, "" -> {
          originTask.observer.onTaskFinished(clientGenericTaskFinishedParams)
        }

        TaskDataKind.COMPILE_REPORT -> {
          val compileReport = try {
            gson.fromJson(params.data as JsonObject, BspCompileReport::class.java)
          } catch (e: Exception) {
            throw DeserializationError(taskId, e)
          }

          val clientCompileTaskFinishedParams = ClientCompileTaskFinishedParams(
            clientGenericTaskFinishedParams,
            compileReport
          )

          val compileTaskObserver = originTask.observer as? CompileTaskObserver ?: run {
            throw WrongTaskType(taskId, TaskDataKind.COMPILE_REPORT)
          }

          compileTaskObserver.onCompileTaskFinished(clientCompileTaskFinishedParams)
        }

        else -> {
          throw UnsupportedDataKind(taskId, params.dataKind)
        }
      }
    }
  }
}
