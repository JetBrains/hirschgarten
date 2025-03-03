package org.jetbrains.bazel.debug

import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger
import org.jetbrains.bazel.debug.connector.StarlarkSocketConnector
import org.jetbrains.bazel.debug.connector.ThreadAwareEventHandler
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointHandler
import org.jetbrains.bazel.debug.utils.MockDebugSession
import org.jetbrains.bazel.debug.utils.MockSocket
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest

abstract class StarlarkDebugClientTestBase : MockProjectBaseTest() {
  protected fun establishMockConnection(): MockConnectionPack {
    val socket = MockSocket()
    val session = MockDebugSession()
    val connector = StarlarkSocketConnector.simpleSocketConnection(socket)

    val messenger = StarlarkDebugMessenger(connector, session)
    val breakpointHandler = StarlarkBreakpointHandler(messenger)
    val eventHandler = ThreadAwareEventHandler(session, messenger, breakpointHandler::getBreakpointByPathAndLine)

    return MockConnectionPack(socket, session, connector, messenger, breakpointHandler, eventHandler)
  }

  protected data class MockConnectionPack(
    val socket: MockSocket,
    val session: MockDebugSession,
    val connector: StarlarkSocketConnector,
    val messenger: StarlarkDebugMessenger,
    val breakpointHandler: StarlarkBreakpointHandler,
    val eventHandler: ThreadAwareEventHandler,
  )
}
