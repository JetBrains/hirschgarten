/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.bsp.golang.run

import com.google.idea.blaze.base.command.BlazeCommandName

/** Blaze plugin specific [com.goide.execution.GoBuildingRunner].  */
class BlazeGoDebugRunner : GoBuildingRunner() {
  val runnerId: String
    get() = "BlazeGoDebugRunner"

  override fun canRun(executorId: String?, profile: com.intellij.execution.configurations.RunProfile?): Boolean {
    if (DefaultDebugExecutor.EXECUTOR_ID != executorId || profile !is BlazeCommandRunConfiguration) {
      return false
    }
    val config: BlazeCommandRunConfiguration = profile as BlazeCommandRunConfiguration
    val handlerState: BlazeCommandRunConfigurationCommonState? =
      config.getHandlerStateIfType(BlazeCommandRunConfigurationCommonState::class.java)
    val command: BlazeCommandName? =
      if (handlerState != null) handlerState.getCommandState().getCommand() else null
    val kind: Kind? = config.getTargetKind()
    return kind != null && kind.hasLanguage(LanguageClass.GO)
      && (kind.getRuleType().equals(RuleType.BINARY) || kind.getRuleType().equals(RuleType.TEST))
      && (BlazeCommandName.TEST.equals(command) || BlazeCommandName.RUN.equals(command))
  }

  @kotlin.Throws(com.intellij.execution.ExecutionException::class)
  override fun execute(
    environment: com.intellij.execution.runners.ExecutionEnvironment,
    state: com.intellij.execution.configurations.RunProfileState?
  ): org.jetbrains.concurrency.Promise<com.intellij.execution.ui.RunContentDescriptor?> {
    if (state !is BlazeGoDummyDebugProfileState) {
      return resolvedPromise<com.intellij.execution.ui.RunContentDescriptor?>()
    }
    return resolvedPromise<com.intellij.execution.ui.RunContentDescriptor?>(
      doExecute(
        environment,
        state as BlazeGoDummyDebugProfileState,
      ),
    )
  }

  @kotlin.Throws(com.intellij.execution.ExecutionException::class)
  protected fun doExecute(
    environment: com.intellij.execution.runners.ExecutionEnvironment, blazeState: BlazeGoDummyDebugProfileState
  ): com.intellij.execution.ui.RunContentDescriptor {
    EventLoggingService.getInstance().logEvent(javaClass, "debugging-go")
    val goState: GoApplicationRunningState = blazeState.toNativeState(environment)
    com.intellij.openapi.progress.ProgressManager.getInstance().runProcessWithProgressSynchronously(
      java.lang.Runnable { com.intellij.openapi.application.ReadAction.run<java.lang.RuntimeException?>(com.intellij.util.ThrowableRunnable { goState.prepareStateInBGT() }) },
      "Preparing Go Application Running State",
      false,
      environment.getProject(),
    )
    val executionResult: com.intellij.execution.ExecutionResult = goState.execute(environment.getExecutor(), this)
    return XDebuggerManager.getInstance(environment.getProject())
      .startSession(
        environment,
        object : XDebugProcessStarter() {
          override fun start(session: XDebugSession): XDebugProcess {
            val connection: RemoteVmConnection<*> =
              DlvRemoteVmConnection(DlvDisconnectOption.KILL)
            val process: XDebugProcess =
              DlvDebugProcess(session, connection, executionResult,  /* remote= */true)
            connection.open(goState.getDebugAddress())
            return process
          }
        },
      )
      .getRunContentDescriptor()
  }
}
