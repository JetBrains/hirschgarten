@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.bazel.performance

import com.intellij.openapi.application.PathManager
import com.intellij.platform.diagnostic.telemetry.FilteredMetricsExporter
import com.intellij.platform.diagnostic.telemetry.MetricsExporterEntry
import com.intellij.platform.diagnostic.telemetry.Scope
import com.intellij.platform.diagnostic.telemetry.TelemetryManager
import com.intellij.platform.diagnostic.telemetry.belongsToScope
import com.intellij.platform.diagnostic.telemetry.exporters.RollingFileSupplier
import com.intellij.platform.diagnostic.telemetry.exporters.meters.TelemetryMeterJsonExporter
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.sun.management.GarbageCollectionNotificationInfo
import java.lang.Thread.sleep
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong
import javax.management.Notification
import javax.management.NotificationEmitter
import javax.management.NotificationListener
import kotlin.io.path.div
import kotlin.math.max
import kotlin.time.Duration
import org.jetbrains.bazel.performance.telemetry.Scope as BazelScope

fun BazelScope.toScope(): Scope = Scope(name)

private const val MB = 1024 * 1024

object MemoryProfiler : NotificationListener {
  private val maxMemoryMb = AtomicLong()

  fun startRecordingMaxMemory() {
    registerMaxMemoryGauge()
    registerMetricsExporter()

    for (bean in ManagementFactory.getGarbageCollectorMXBeans()) {
      (bean as? NotificationEmitter)?.addNotificationListener(this, null, null)
    }
  }

  private fun registerMaxMemoryGauge() {
    val maxMemoryMbGauge = bspMeter.gaugeBuilder("bsp.max.used.memory.mb").ofLongs().buildObserver()
    bspMeter.batchCallback({
      maxMemoryMbGauge.record(maxMemoryMb.get())
    }, maxMemoryMbGauge)
  }

  private fun registerMetricsExporter() {
    val basePath = PathManager.getLogDir() / "open-telemetry-meters.bsp.json"
    val metricsExporter = TelemetryMeterJsonExporter(RollingFileSupplier(basePath))
    val filteredMetricsExporter =
      FilteredMetricsExporter(SynchronizedClearableLazy { metricsExporter }) { metric ->
        metric.belongsToScope(bspScope.toScope())
      }
    TelemetryManager
      .getInstance()
      .addMetricsExporters(listOf(MetricsExporterEntry(listOf(filteredMetricsExporter), Duration.INFINITE)))
  }

  override fun handleNotification(notification: Notification, handback: Any?) {
    if (notification.type != GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION) return
    val usedMb = getUsedMemoryMb()
    maxMemoryMb.getAndUpdate { max(it, usedMb) }
  }

  fun stopRecordingMaxMemory() {
    for (bean in ManagementFactory.getGarbageCollectorMXBeans()) {
      (bean as? NotificationEmitter)?.removeNotificationListener(this)
    }
  }

  fun recordMemory(gaugeName: String) {
    forceGc()
    val usedMb = getUsedMemoryMb()

    val gauge = bspMeter.gaugeBuilder(gaugeName).ofLongs().buildObserver()
    bspMeter.batchCallback({
      gauge.record(usedMb)
    }, gauge)
  }

  private fun getUsedMemoryMb(): Long {
    val runtime = Runtime.getRuntime()
    return (runtime.totalMemory() - runtime.freeMemory()) / MB
  }

  private fun forceGc() {
    val oldGcCount = getGcCount()
    System.gc()
    while (oldGcCount == getGcCount()) sleep(1)
  }

  private fun getGcCount(): Long =
    ManagementFactory
      .getGarbageCollectorMXBeans()
      .mapNotNull {
        it.collectionCount.takeIf { count -> count != -1L }
      }.sum()
}
