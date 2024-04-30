package org.jetbrains.bazel.debug.error

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos

class StarlarkDebuggerError(error: StarlarkDebuggingProtos.Error) : RuntimeException(error.message)
