package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails

public class BspUtilService {

  public var bspConnectionDetails: HashMap<String, LocatedBspConnectionDetails> = hashMapOf()
  public var selectedBuildTool: HashMap<String, String> = hashMapOf()
  public val loadedViaBspFile: MutableSet<String> = mutableSetOf()

  public companion object {

    public var projectPathKey: Key<VirtualFile> = Key<VirtualFile>("projectPath")
    public var targetIdKey: Key<BuildTargetIdentifier> = Key<BuildTargetIdentifier>("targetId")
    public fun getInstance(): BspUtilService =
      ApplicationManager.getApplication().getService(BspUtilService::class.java)
  }
}
