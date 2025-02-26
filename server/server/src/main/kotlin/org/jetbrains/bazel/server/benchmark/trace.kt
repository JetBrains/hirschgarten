package org.jetbrains.bazel.server.benchmark

import io.opentelemetry.sdk.trace.data.SpanData

val SpanData.durationMs: Long
  get() = (this.endEpochNanos - this.startEpochNanos) / 1_000_000
