package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.protocol.connection.LocatedBspConnectionDetails

public class BspUtilService {

  public var connectionFile: HashMap<String, LocatedBspConnectionDetails> = hashMapOf()

  public companion object {
    public fun getInstance(): BspUtilService =
      ApplicationManager.getApplication().getService(BspUtilService::class.java)
  }
}
