package org.jetbrains.bsp.bazel.server.bsp

import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.jsonrpc.MessageIssueHandler
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import org.jetbrains.bsp.bazel.server.benchmark.openTelemetry
import org.jetbrains.bsp.bazel.server.benchmark.openTelemetryInitialized
import java.io.InputStream
import java.util.concurrent.Executors

class TelemetryContextPropagatingLauncherBuilder<T> : Launcher.Builder<T>() {
  override fun create(): Launcher<T> {
    val jsonHandler = createJsonHandler()
    val remoteEndpoint = createRemoteEndpoint(jsonHandler)
    val remoteProxy = createProxy(remoteEndpoint)
    val reader = TelemetryContextPropagatingStreamMessageProducer(input, jsonHandler, remoteEndpoint)
    val messageConsumer = wrapMessageConsumer(remoteEndpoint)
    val msgProcessor = createMessageProcessor(reader, messageConsumer, remoteProxy)
    val execService = if (executorService != null) executorService else Executors.newCachedThreadPool()
    return createLauncher(execService, remoteProxy, remoteEndpoint, msgProcessor)
  }
}

val TEXT_MAP_GETTER: TextMapGetter<Map<String, String>> =
  object : TextMapGetter<Map<String, String>> {
    override fun keys(carrier: Map<String, String>): Set<String> = carrier.keys

    override fun get(carrier: Map<String, String>?, key: String): String? = carrier?.get(key)
  }

internal class CaseInsensitiveMap : HashMap<String, String> {
  constructor(carrier: Map<String, String>) {
    this.putAll(carrier)
  }

  override fun put(key: String, value: String): String? = super.put(getKeyLowerCase(key), value)

  override fun putAll(m: Map<out String, String>) {
    m.forEach { (key: String, value: String) -> this.put(key, value) }
  }

  override fun get(key: String): String? = super.get(getKeyLowerCase(key))

  companion object {
    private fun getKeyLowerCase(key: String): String = key.lowercase()
  }
}

private class TelemetryContextPropagatingStreamMessageProducer(
  input: InputStream,
  jsonHandler: MessageJsonHandler,
  issueHandler: MessageIssueHandler,
) : StreamMessageProducer(input, jsonHandler, issueHandler) {
  private val currentHeaders = mutableMapOf<String, String>()

  override fun parseHeader(line: String, headers: Headers) {
    val (key, value) = line.split(":", limit = 2).map { it.trim() }
    currentHeaders[key] = value
    super.parseHeader(line, headers)
  }

  fun extractTextMapPropagationContext(carrier: Map<String, String>, propagators: ContextPropagators): Context {
    val current = Context.current()
    val caseInsensitiveMap = CaseInsensitiveMap(carrier)
    return propagators.textMapPropagator
      .extract(current, caseInsensitiveMap, TEXT_MAP_GETTER)
  }

  override fun handleMessage(input: InputStream, headers: Headers): Boolean {
    if (!openTelemetryInitialized) {
      // The first call via BSP is build/initialize, at which point the telemetry isn't initialized yet.
      return super.handleMessage(input, headers)
    }
    val context =
      extractTextMapPropagationContext(currentHeaders, openTelemetry.propagators)
        .also { currentHeaders.clear() }
    context.makeCurrent().use {
      return super.handleMessage(input, headers)
    }
  }
}
