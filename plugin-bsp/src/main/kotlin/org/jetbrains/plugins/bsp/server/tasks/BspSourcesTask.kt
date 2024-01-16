import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.tasks.BspServerMultipleTargetsTask

public class BspSourcesTask(project: Project): BspServerMultipleTargetsTask<SourcesResult>("get sources", project){
    override fun executeWithServer(
        server: BspServer,
        capabilities: BuildServerCapabilities,
        targetsIds: List<BuildTargetIdentifier>
    ): SourcesResult {
        return server.buildTargetSources(SourcesParams(targetsIds)).get()
    }

}