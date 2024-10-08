# Bazel Starlark debugging - basic information
*Class references mentioned here are relative to this document's path.*

## Bazel clean
Before using the Starlark debug feature, a `bazel clean` command may be necessary.
That is because if build caches are present and up to date, the analysis phase will not be executed,
therefore not reaching any breakpoints.

## Connection
Firstly, BSP server is asked to build a target in a debug mode (only the analysis phase can be debugged).
We then connect to `localhost:<port>` (Bazel does not support debugging between hosts) using Java socket infrastructure
(managed by class [`connector.StarlarkSocketConnector`](connector/StarlarkSocketConnector.kt)).
Port is chosen automatically and is not saved in the run configuration —
thanks to that the chance of port collision is negligible.

## Messages
Communication with the debugger is done using messages defined in `starlark_debugging.proto`,
which has been obtained from [Bazel source code](https://github.com/bazelbuild/bazel/blob/ca728739071c78c67b5d251c7be4b9ba7c17b225/src/main/java/com/google/devtools/build/lib/starlarkdebug/proto/starlark_debugging.proto).
Messages are sent through sockets using the Google protobuf library.

## Console
IntelliJ debug tool window has a built-in console, which does show related build info.
For unknown reason, when we are suspended on a breakpoint or execution has been paused,
BSP server keeps sending the following log message in a loop:

```text
Loading: 0 packages loaded
    currently loading: <target>
```

To avoid unnecessary clutter, log messages are not displayed while execution is suspended.
Moreover, sometimes log messages arrive after unregistering the listener,
which may lead to the last message not being shown to the user at all.

## Threads
Many protobuf messages in Bazel debugging interface need to have thread identifiers specified,
so whole event handling is being done in a thread-aware way [`connector.ThreadAwareEventHandler`](connector/ThreadAwareEventHandler.kt).
That means it knows which threads are paused and can pass that information to IntelliJ debugging interface.

## Expression evaluation
Expression evaluation can be done in any thread, but only at the top of the execution stack.
That limitation cannot be worked around, as Bazel does not accept specifying which stack frame to evaluate on.
This is the only situation in which the user receives error information,
as all other errors simply return an empty result
(we cannot inform the user of error details as we are not given those by Bazel).
Expression evaluation is performed entirely by Bazel, so some issues cannot be fixed by us
(for instance, only top-level variables and functions can be used in the evaluation).

## Paused execution
When execution is paused (either by a breakpoint or the pause functionality of IntelliJ debugging interface),
the user is given a list of threads (usually one), each having an execution stack, each having a list of values.
Compound values (lists, dictionaries, etc.) can be expanded, but children are computed lazily
([`platform.StarlarkStackFrame::computeChildren`](platform/StarlarkStackFrame.kt)).

## Termination
### Socket break
The socket will break in many situations, most commonly when Bazel process finishes

1. This causes an exception in loop defined in [`connector.StarlarkDebugManager`](connector/StarlarkDebugSessionManager.kt)
2. ...which executes the manager's `stop()`, terminating the debug task
3. ...and executes `close()` on [`connector.StarlarkDebugMessenger`](connector/StarlarkDebugMessenger.kt)

### UI termination
The user can also terminate debugging manually through IntelliJ interface

1. IntelliJ executes `stop()` in [`platform.StarlarkDebugProcess`](platform/StarlarkDebugProcess.kt)
2. ...which executes `close()` on [`connector.StarlarkDebugMessenger`](connector/StarlarkDebugMessenger.kt)
3. ...which executes `close()` on [`connector.StarlarkSocketConnector`](connector/StarlarkSocketConnector.kt), thus breaking the socket
4. ...which causes the chain of events mentioned above
