package org.jetbrains.plugins.bsp.android.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.android.tools.idea.run.ApkProvisionException
import com.android.tools.idea.run.ApplicationIdProvider
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.util.getModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.util.moduleEntity
import org.jetbrains.plugins.bsp.workspacemodel.entities.androidAddendumEntity
import com.android.tools.idea.project.getPackageName as getApplicationIdFromManifest

private const val MANIFEST_APPLICATION_ID = "applicationId"

class BspApplicationIdProvider(private val project: Project, private val target: BuildTargetIdentifier) : ApplicationIdProvider {
  override fun getPackageName(): String {
    val module = target.getModule(project) ?: throw ApkProvisionException("Could not find module for target $target")
    module
      .moduleEntity
      ?.androidAddendumEntity
      ?.manifestOverrides
      ?.get(MANIFEST_APPLICATION_ID)
      ?.let { return it }
    return getApplicationIdFromManifest(module) ?: throw ApkProvisionException("Could not get applicationId from manifest")
  }

  override fun getTestPackageName(): String? = null
}
