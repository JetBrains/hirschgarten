package org.jetbrains.bazel.executioncontext.api

import org.jetbrains.bazel.projectview.model.ProjectView

/**
 * `ExecutionContext` base class - you need to extend it if you want to create your
 * implementation of `ExecutionContext`.
 */
abstract class ExecutionContext

/**
 * Constructs a `ExecutionContext` for a `ProjectView`. Probably you should use
 * `ProjectViewToExecutionContextEntityMapper` or `ProjectViewToExecutionContextEntityOptionMapper`
 * in your implementation.
 *
 * @param <T> type of yours `ExecutionContext`
 * @see ExecutionContext
 *
 * @see org.jetbrains.bazel.projectview.model.ProjectView
 * @see ExecutionContextEntityExtractor
 */
interface ExecutionContextConstructor<T : ExecutionContext> {
  fun construct(projectView: ProjectView): T
}
