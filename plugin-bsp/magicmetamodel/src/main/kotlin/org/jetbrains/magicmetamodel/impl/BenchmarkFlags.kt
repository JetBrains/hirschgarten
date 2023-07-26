package org.jetbrains.magicmetamodel.impl

import java.nio.file.Path
import java.nio.file.Paths

public object BenchmarkFlags {
    public const val BENCHMARK_PROJECT_PATH: String = "bsp.benchmark.project.path"
    public const val BENCHMARK_METRICS_FILE: String = "bsp.benchmark.metrics.file"

    public fun benchmarkProjectPath(): Path? = System.getProperty(BENCHMARK_PROJECT_PATH)?.let { Paths.get(it) }

    public fun metricsFile(): Path? = System.getProperty(BENCHMARK_METRICS_FILE)?.let { Paths.get(it) }


    public fun isBenchmark(): Boolean = benchmarkProjectPath() != null
}
