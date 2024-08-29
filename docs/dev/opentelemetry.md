# OpenTelemetry metrics

Our plugins collect performance data through [OpenTelemetry](https://opentelemetry.io/).

## Intellij-BSP

### Time metrics collected
```
bsp.sync.project.ms 
collect.project.details.ms  
apply.changes.on.workspace.model.ms  
replacebysource.in.apply.on.workspace.model.ms  
replaceprojectmodel.in.apply.on.workspace.model.ms  
add.bsp.fetched.jdks.ms  
create.target.id.to.module.entities.map.ms  
load.modules.ms  
create.module.details.ms  
calculate.all.unique.jdk.infos.ms  
create.libraries.ms 
create.library.modules.ms
```
### Memory metrics collected:
```
bsp.used.after.sync.mb  
bsp.used.after.indexing.mb  
bsp.max.used.memory.mb  
```

### Telemetry code explanation
On IntelliJ-BSP side, we use an already existing telemetry API from IDEA platform to export the time and memory metrics. Here's how creating a time metric looks like:
```kotlin
bspTracer.spanBuilder("collect.project.details.ms").use {  
  connectAndExecuteWithServer { server, capabilities ->  
    collectModel(server, capabilities, buildProject)  
  }  
}
```
Memory metrics are a bit more complicated because the platform doesn't have a nice API for adding custom OpenTelemetry gauges (see `MemoryProfiler.kt`).
All the metrics are then exported by IDEA however it's configured, but usually at IDEA exit.

To populate the performance dashboard for IntelliJ-BSP we use `intellij-ide-starter` (see `BazelTest.kt`) which collects the metrics and exports them as a TeamCity artifact.

### Run IntelliJ-BSP benchmark locally
To run the benchmark locally, run the following command: `bazel test //plugin-bsp/performance-testing:performance-testing --jvmopt="-Dbsp.benchmark.cache.directory=/Users/<username>/IdeaProjects/hirschgarten -Dbsp.benchmark.project.path=/Users/<username>/IdeaProjects/<project-to-benchmark-on>" --sandbox_writable_path=/ --action-env=PATH`. It will download IDEA, launch it, open the project and wait for it to import. Then it collects the metrics from IDEA and saves them as a TeamCity artifact.

### Run IDEA while exporting to Jaeger
If you want to use IDEA yourself but still get the metrics from the sync, you can do that by exporting metrics via Jaeger. Here's how:
1. As per https://www.jaegertracing.io/docs/1.58/getting-started/, launch Jaeger in Docker like so:  `docker run --rm --name jaeger -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 -p 6831:6831/udp -p 6832:6832/udp -p 5778:5778 -p 16686:16686 -p 4317:4317 -p 4318:4318 -p 14250:14250 -p 14268:14268 -p 14269:14269 -p 9411:9411 jaegertracing/all-in-one:1.58`
2. Add `-Didea.diagnostic.opentelemetry.otlp=http://127.0.0.1:4318` to IDEA's VM arguments
3. Do a sync, close IDEA.
4. Navigate to http://localhost:16686/ to access Jaeger UI.
5. Search for `bsp.sync.project.ms`. You will be shown a timeline of all operations (both in IntelliJ-BSP and Bazel-BSP) and how much they took.
### Run IntelliJ-BSP benchmark on TeamCity
Run https://buildserver.labs.intellij.net/buildConfiguration/Bazel_Hirschgarten_BenchmarkPluginBspmetrics.
Results will appear in https://ij-perf.labs.jb.gg/bazel/intellijBSPDashboard in 30-60 minutes.
## Bazel-BSP
### Time metrics collected
```
resolve.project.time.ms
building.project.with.aspect.time.ms
mapping.to.internal.model.time.ms
parsing.aspect.outputs.time.ms
create.modules.time.ms
reading.aspect.output.paths.time.ms
fetching.all.possible.target.names.time.ms discovering.supported.external.rules.time.ms
select.targets.time.ms libraries.from.jdeps.time.ms libraries.from.targets.and.deps.time.ms
build.dependency.tree.time.ms
build.reverse.sources.time.ms
targets.as.libraries.time.ms
create.ap.libraries.time.ms
create.kotlin.stdlibs.time.ms
save.invalid.target.labels.time.ms
libraries.from.transitive.compile-time.jars.time.ms
```
### Memory metrics collected
```
max.used.memory.mb
used.at.exit.mb
```
### Telemetry code explanation
Because Bazel-BSP is a standalone server, we have to setup OpenTelemetry by ourselves, see `telemetry.kt` and `fun setupTelemetry(config: TelemetryConfig)`. We have three options for exporting metrics:
```
data class TelemetryConfig(  
  val bspClientLogger: BspClientLogger? = null,  
  val metricsFile: Path? = null,  
  val openTelemetryEndpoint: String? = null,  
)
```
The first just prints the results to the user into the console (e.g. `Task 'Mapping to internal model' completed in 135ms`).
It is possible to export metrics to a file, which is done when running Bazel-BSP dashboard.
And lastly, if IDEA (and thus IntelliJ-BSP since they share the same telemetry) has a Jaeger (or other) exporter, this URL is passed to Bazel-BSP upon initialization. With that it's possible to see metrics both from the plugin and the server in Jaeger.

Tracing, i.e. collecting time metrics, is done like so:
```
tracer.spanBuilder("Resolve project").use {
	// ...
}
```
Memory metrics are collected in the same hacky way as in IntelliJ-BSP (see `MemoryProfiler.kt` in the server).
### Run Bazel-BSP benchmark locally
```
bazel run //server/bspcli:bspcli /path/to/project /path/to/output/metrics.txt //...
```
Then `metrics.txt` will contain the metrics.
### Run Bazel-BSP benchmark on TeamCity
Run https://buildserver.labs.intellij.net/buildConfiguration/Bazel_Hirschgarten_BenchmarkServermetrics
Results will appear in https://ij-perf.labs.jb.gg/bazel/bazelBSPDashboard in 30-60 minutes.