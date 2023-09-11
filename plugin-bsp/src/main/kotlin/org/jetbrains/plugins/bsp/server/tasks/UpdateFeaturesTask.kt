package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.SetCargoFeaturesParams
import ch.epfl.scala.bsp4j.SetCargoFeaturesResult
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

public class UpdateFeaturesTask(project: Project, private val packageId: String, private val featuresState: Set<String>): BspServerTask<SetCargoFeaturesResult>("update features", project) {
    public fun execute(): StatusCode? {
        return try {
            val result = connectAndExecuteWithServer {server, _ ->  server.setCargoFeatures(SetCargoFeaturesParams(packageId, featuresState.toList())).get() }
            result?.statusCode
        } catch (e: Exception) {
            log.warn("Failed to update features for package $packageId", e)
            null
        }
    }
    private companion object {
        private val log = logger<UpdateFeaturesTask>()
    }
}