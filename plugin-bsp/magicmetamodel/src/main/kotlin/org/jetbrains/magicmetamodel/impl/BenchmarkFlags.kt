package org.jetbrains.magicmetamodel.impl

import java.nio.file.Path
import java.nio.file.Paths

private const val BENCHMARK_METRICS_FILE: String = "bsp.benchmark.metrics.file"
private const val IS_BENCHMARK: String = "bsp.is.benchmark"

public object BenchmarkFlags {
  public fun metricsFile(): Path? = System.getProperty(BENCHMARK_METRICS_FILE)?.let { Paths.get(it) }

  public fun isBenchmark(): Boolean = System.getProperty(IS_BENCHMARK, "false").toBoolean()
}
