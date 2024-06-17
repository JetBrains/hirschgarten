@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.plugins.bsp.performance.testing

import com.intellij.openapi.application.PathManager
import com.intellij.platform.diagnostic.telemetry.FilteredMetricsExporter
import com.intellij.platform.diagnostic.telemetry.MetricsExporterEntry
import com.intellij.platform.diagnostic.telemetry.TelemetryManager
import com.intellij.platform.diagnostic.telemetry.belongsToScope
import com.intellij.platform.diagnostic.telemetry.impl.CsvMetricsExporter
import com.intellij.platform.diagnostic.telemetry.impl.RollingFileSupplier
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.intellij.util.ref.GCUtil
import com.sun.management.GarbageCollectionNotificationInfo
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong
import javax.management.Notification
import javax.management.NotificationEmitter
import javax.management.NotificationListener
import javax.management.openmbean.CompositeData
import kotlin.io.path.div
import kotlin.math.max
import kotlin.time.Duration

private const val MB = 1024 * 1024

internal object MemoryProfiler : NotificationListener {
  private var maxUsedMb = AtomicLong()
  private var usedAtExitMb = AtomicLong()

  fun startRecording() {
    createOpenTelemetryMemoryGauges()
    registerMetricsExporter()

    for (bean in ManagementFactory.getGarbageCollectorMXBeans()) {
      (bean as? NotificationEmitter)?.addNotificationListener(this, null, null)
    }
  }

  private fun createOpenTelemetryMemoryGauges() {
    val maxUsedMbGauge = bspMeter.gaugeBuilder("bsp.max.used.memory.mb").ofLongs().buildObserver()
    val usedAtExistMbGauge = bspMeter.gaugeBuilder("bsp.used.at.exit.mb").ofLongs().buildObserver()
    bspMeter.batchCallback({
      maxUsedMbGauge.record(maxUsedMb.get())
      usedAtExistMbGauge.record(usedAtExitMb.get())
    }, maxUsedMbGauge, usedAtExistMbGauge)
  }

  private fun registerMetricsExporter() {
    val basePath = PathManager.getLogDir() / "open-telemetry-metrics.bsp.csv"
    // TODO replace with TelemetryMeterJsonExporter after IJ platform update to 242
    val metricsExporter = CsvMetricsExporter(RollingFileSupplier(basePath))
    val filteredMetricsExporter =
      FilteredMetricsExporter(SynchronizedClearableLazy { metricsExporter }) { metric ->
        metric.belongsToScope(bspScope)
      }
    TelemetryManager.getInstance()
      .addMetricsExporters(listOf(MetricsExporterEntry(listOf(filteredMetricsExporter), Duration.INFINITE)))
  }

  override fun handleNotification(notification: Notification, handback: Any?) {
    if (notification.type != GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION) return
    val gcInfo = GarbageCollectionNotificationInfo.from(notification.userData as CompositeData)
    if (gcInfo.gcAction != "end of major GC") return
    val usedMb = getUsedMemoryMb()
    maxUsedMb.getAndUpdate { max(it, usedMb) }
  }

  fun stopRecording() {
    GCUtil.tryGcSoftlyReachableObjects()
    val usedAtExitMb = getUsedMemoryMb()
    this.usedAtExitMb.set(usedAtExitMb)
    maxUsedMb.getAndUpdate { max(it, usedAtExitMb) }

    for (bean in ManagementFactory.getGarbageCollectorMXBeans()) {
      (bean as? NotificationEmitter)?.removeNotificationListener(this)
    }
  }

  private fun getUsedMemoryMb(): Long {
    val runtime = Runtime.getRuntime()
    return (runtime.totalMemory() - runtime.freeMemory()) / MB
  }
}
