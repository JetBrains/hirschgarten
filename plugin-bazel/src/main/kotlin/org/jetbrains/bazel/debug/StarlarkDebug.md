# Bazel Starlark debugging - basic information
*Class references mentioned here are relative to this document's path.*

## Connection
Before IntelliJ attaches, Bazel build needs to be started with debugging enabled (`bazel build --experimental_skylark_debug ...`). After the build is started, it waits for someone to attach on a chosen port (default `7300`). We connect to `localhost:<port>` (Bazel does not support debugging between hosts) using Java socket infrastructure (managed by class [`connector.StarlarkSocketConnector`](connector/StarlarkSocketConnector.kt)).

## Messages
Communication with the debugger is done using messages defined in `starlark_debugging.proto`, which has been obtained from [Bazel source code](https://github.com/bazelbuild/bazel/blob/ca728739071c78c67b5d251c7be4b9ba7c17b225/src/main/java/com/google/devtools/build/lib/starlarkdebug/proto/starlark_debugging.proto). Messages are sent through sockets using Google protobuf library.

## Threads
Many protobuf messages in Bazel debugging interface need to have thread identifiers specified, so all event handling is being done in a thread-aware way [`connector.ThreadAwareEventHandler`](connector/ThreadAwareEventHandler.kt). That means it knows which threads are paused and can pass that information to IntelliJ debugging interface.

## Expression evaluation
Expression evaluation can be done in any thread, but only at the top of the execution stack - that limitation cannot be worked around, as Bazel does not accept specifying which stack frame to evaluate on. This is the only situation in which the user receives an error information - all other errors simply return an empty result (we cannot inform the user of error details as we are not given those by Bazel). Expression evaluation is performed entirely by Bazel, so some issues cannot be fixed by us (for instance, only top-level variables and functions can be used in the evaluation).

## Paused execution
When execution is paused (either by a breakpoint or the pause functionality of IntelliJ debugging interface), user is given a list of threads (usually one), each having an execution stack, each having a list of values. Compound values (lists, dictionaries etc.) can be expanded, but children are computed lazily ([`platform.StarlarkStackFrame::computeChildren`](platform/StarlarkStackFrame.kt)).

## Terminating
Debugging can be terminated from two sides:

* **Bazel build ends or is stopped** - [`connector.StarlarkSocketConnector`](connector/StarlarkSocketConnector.kt) detects broken data stream and executes `onSocketBreak()`
* **Debugging is terminated through IntelliJ interface** - IntelliJ executes `stop()` in [`platform.StarlarkDebugProcess`](platform/StarlarkDebugProcess.kt)

Both cause the same chain of events:

1. Executing `stop()` in [`connector.StarlarkDebugManager`](connector/StarlarkDebugManager.kt)
2. ...which executes `close()` on [`connector.StarlarkDebugMessenger`](connector/StarlarkDebugMessenger.kt)
3. ...which stops the debug session and executes `close()` on [`connector.StarlarkSocketConnector`](connector/StarlarkSocketConnector.kt)
