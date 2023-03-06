package org.jetbrains.plugins.bsp.server

import ch.epfl.scala.bsp4j.*
import org.jetbrains.plugins.bsp.server.connection.BspServer
import java.util.concurrent.CompletableFuture
import kotlin.math.sqrt

private typealias BTI = BuildTargetIdentifier

public class ChunkingBuildServer<S: BspServer>(private val base: S, private val minChunkSize: Int) : BspServer by base {

  override fun buildTargetSources(params: SourcesParams?): CompletableFuture<SourcesResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { SourcesParams(it) },
      doRequest = base::buildTargetSources,
      unwrapRes = { it.items },
      wrapRes = { SourcesResult(it) }
    )(params)

  override fun buildTargetResources(params: ResourcesParams?): CompletableFuture<ResourcesResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { ResourcesParams(it) },
      doRequest = base::buildTargetResources,
      unwrapRes = { it.items },
      wrapRes = { ResourcesResult(it) }
    )(params)

  override fun buildTargetDependencySources(params: DependencySourcesParams?): CompletableFuture<DependencySourcesResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { DependencySourcesParams(it) },
      doRequest = base::buildTargetDependencySources,
      unwrapRes = { it.items },
      wrapRes = { DependencySourcesResult(it) }
    )(params)

  override fun buildTargetOutputPaths(params: OutputPathsParams?): CompletableFuture<OutputPathsResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { OutputPathsParams(it) },
      doRequest = base::buildTargetOutputPaths,
      unwrapRes = { it.items },
      wrapRes = { OutputPathsResult(it) }
    )(params)

  override fun buildTargetDependencyModules(params: DependencyModulesParams?): CompletableFuture<DependencyModulesResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { DependencyModulesParams(it) },
      doRequest = base::buildTargetDependencyModules,
      unwrapRes = { it.items },
      wrapRes = { DependencyModulesResult(it) }
    )(params)

  private fun<ReqW, Res, ResW> chunkedRequest(
    unwrapReq: (ReqW) -> List<BTI>,
    wrapReq: (List<BTI>) -> ReqW,
    doRequest: (ReqW?) -> CompletableFuture<ResW>,
    unwrapRes: (ResW) -> List<Res>,
    wrapRes: (List<Res>) -> ResW,
  ): (ReqW?) -> CompletableFuture<ResW> =
    fun (requestParams: ReqW?): CompletableFuture<ResW> {
      if (requestParams == null) return doRequest(null)
      val allTargetsIds = unwrapReq(requestParams)
      val requests = allTargetsIds.chunked(chunkSize(allTargetsIds))
        .map { doRequest(wrapReq(it)) }
      val all = CompletableFuture.allOf(*requests.toTypedArray()).thenApply {
        requests.map { unwrapRes(it.get()) }.flatten().let{ wrapRes(it) }
      }
      return all.whenComplete { _, _ ->
        if(all.isCancelled) requests.forEach { it.cancel(true) }
      }
    }

  private fun chunkSize(targetIds: List<Any>) =
    sqrt(targetIds.size.toDouble()).toInt().coerceAtLeast(minChunkSize)

}