package org.jetbrains.plugins.bsp.server.connection

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import java.io.OutputStream

internal class TelemetryContextPropagatingLauncherBuilder<T> : Launcher.Builder<T>() {
  override fun createRemoteEndpoint(jsonHandler: MessageJsonHandler): RemoteEndpoint {
    val outgoingMessageStream =
      wrapMessageConsumer(TelemetryContextPropagatingStreamMessageConsumer(output, jsonHandler))
    val localEndpoint = ServiceEndpoints.toEndpoint(localServices)
    val remoteEndpoint = if (exceptionHandler == null) {
      RemoteEndpoint(outgoingMessageStream, localEndpoint)
    } else {
      RemoteEndpoint(outgoingMessageStream, localEndpoint, exceptionHandler)
    }
    jsonHandler.methodProvider = remoteEndpoint
    return remoteEndpoint
  }
}

private class TelemetryContextPropagatingStreamMessageConsumer(
  output: OutputStream,
  jsonHandler: MessageJsonHandler,
) : StreamMessageConsumer(output, jsonHandler) {
  override fun getHeader(contentLength: Int): String {
    val headerBuilder = StringBuilder(super.getHeader(contentLength))
    if (headerBuilder.endsWith(CRLF + CRLF)) {
      headerBuilder.setLength(headerBuilder.length - CRLF.length)
    }
    val openTelemetry = GlobalOpenTelemetry.get()
    openTelemetry.propagators.textMapPropagator.inject(Context.current(), headerBuilder, HeaderSetter())
    headerBuilder.append(CRLF)
    return headerBuilder.toString()
  }

  private inner class HeaderSetter : TextMapSetter<StringBuilder> {
    override fun set(headerBuilder: StringBuilder?, key: String, value: String) {
      appendHeader(headerBuilder ?: return, key, value).append(CRLF)
    }
  }
}
