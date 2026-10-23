package org.jetbrains.bazel.sync

import com.intellij.openapi.diagnostic.ControlFlowException
import org.jetbrains.annotations.ApiStatus

/**
 * Stops the sync cleanly when the error is expected.
 * Before it throws this exception, the code shows the reason as a diagnostic.
 * For example, the `.bazelproject` file has errors, and the user must fix them.
 * The sync stops and does not log an error.
 */
@ApiStatus.Internal
class ProjectSyncAbortException : Exception(), ControlFlowException
