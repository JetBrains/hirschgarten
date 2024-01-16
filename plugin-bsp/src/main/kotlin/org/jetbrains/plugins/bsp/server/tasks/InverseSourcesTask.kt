package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit

public class InverseSourcesTask(project: Project) :
    BspServerTask<InverseSourcesResult>("buildTargetInverseSources", project) {
    public fun getInverseSources(file: String): InverseSourcesResult? =
        this.connectAndExecuteWithServer { bspServer, bazelBuildServerCapabilities ->
            if (bazelBuildServerCapabilities.inverseSourcesProvider) {
                bspServer.buildTargetInverseSources(InverseSourcesParams(TextDocumentIdentifier(file)))
                    .get(30, TimeUnit.SECONDS)
            } else null
        }
}