package org.jetbrains.bsp.protocol.utils

import org.jetbrains.bsp.protocol.AndroidBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget

inline fun <reified Data> BuildTarget.extractData(): Data? {
  // cache local field to lv
  val data = this.data

  // allocation-free fast path
  if (data.size == 1) {
    return data[0] as? Data
  }

  // also here - avoid allocating iterator
  for (n in 0 until data.size) {
    val d = data[n] as? Data
    if (d != null) {
      return d
    }
  }
  return null
}

fun extractPythonBuildTarget(target: BuildTarget): PythonBuildTarget? = target.extractData()

fun extractScalaBuildTarget(target: BuildTarget): ScalaBuildTarget? = target.extractData()

fun extractAndroidBuildTarget(target: BuildTarget): AndroidBuildTarget? = target.extractData()

fun extractGoBuildTarget(target: BuildTarget): GoBuildTarget? = target.extractData()

fun extractKotlinBuildTarget(target: BuildTarget): KotlinBuildTarget? =
  target.extractData()
    ?: extractAndroidBuildTarget(target)?.kotlinBuildTarget

fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? =
  target.extractData()
    ?: extractAndroidBuildTarget(target)?.jvmBuildTarget
    ?: extractKotlinBuildTarget(target)?.jvmBuildTarget
    ?: extractScalaBuildTarget(target)?.jvmBuildTarget

inline fun <reified Data> Sequence<BuildTarget>.mapWithBuildTargets() = mapNotNull { it.extractData<Data>() }
inline fun <reified Data> Collection<BuildTarget>.mapWithBuildTargets() = mapNotNull { it.extractData<Data>() }
