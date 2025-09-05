package org.jetbrains.bsp.protocol

interface BuildTaskHandler {
  fun onBuildTaskStart(params: TaskStartParams)

  fun onBuildTaskFinish(params: TaskFinishParams)
}
