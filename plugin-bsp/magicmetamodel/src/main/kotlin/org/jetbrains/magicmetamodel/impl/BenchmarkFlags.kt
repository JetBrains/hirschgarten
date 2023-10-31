package org.jetbrains.magicmetamodel.impl

import java.nio.file.Path
import java.nio.file.Paths

private const val BENCHMARK_PROJECT_PATH: String = "bsp.benchmark.project.path"
private const val BENCHMARK_METRICS_FILE: String = "bsp.benchmark.metrics.file"

public object BenchmarkFlags {
  public fun benchmarkProjectPath(): Path? = System.getProperty(BENCHMARK_PROJECT_PATH)?.let { Paths.get(it) }

  public fun metricsFile(): Path? = System.getProperty(BENCHMARK_METRICS_FILE)?.let { Paths.get(it) }

  public fun isBenchmark(): Boolean = benchmarkProjectPath() != null
}
