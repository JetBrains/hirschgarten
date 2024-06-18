package org.jetbrains.bazel.debug

import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger
import org.jetbrains.bazel.debug.connector.StarlarkSocketConnector
import org.jetbrains.bazel.debug.connector.ThreadAwareEventHandler
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointHandler
import org.jetbrains.bazel.debug.utils.MockDebugSession
import org.jetbrains.bazel.debug.utils.MockSocket
import org.junit.jupiter.api.BeforeEach

abstract class StarlarkDebugClientTestBase {
  @BeforeEach
  fun establishMockConnection() {
    socket = MockSocket()
    session = MockDebugSession()
    val connector = StarlarkSocketConnector.simpleSocketConnection(socket)

    messenger = StarlarkDebugMessenger(connector, session)
    breakpointHandler = StarlarkBreakpointHandler(messenger)
    eventHandler = ThreadAwareEventHandler(session, messenger, breakpointHandler::getBreakpointByPathAndLine)
  }

  protected lateinit var socket: MockSocket
  protected lateinit var session: MockDebugSession
  protected lateinit var messenger: StarlarkDebugMessenger
  protected lateinit var breakpointHandler: StarlarkBreakpointHandler
  protected lateinit var eventHandler: ThreadAwareEventHandler
}
