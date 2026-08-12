// This import should be red, because it uses a transitive dependency which was not exported.
import com.google.common.annotations.VisibleForTesting
// This import should NOT be red. java_proto_library is a bit tricky in that it doesn't export but rather *forwards* its dependencies.
// We should support that case as well.
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest

@VisibleForTesting
fun main() {
    val workRequest: WorkRequest? = null
    println(workRequest)
}
