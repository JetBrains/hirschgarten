package org.jetbrains.bsp.protocol

interface CancelExtension {
  fun cancelRequest(params: CancelRequestParams): Unit
}
