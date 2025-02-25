package org.jetbrains.bazel.server.bsp

import io.opentelemetry.context.Context
import org.apache.logging.log4j.LogManager
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.CompletableFuture

class BspRequestsRunner(private val serverLifetime: BazelBspServerLifetime) {
  fun <T, R> handleRequest(
    methodName: String,
    function: (CancelChecker, T) -> R,
    arg: T,
  ): CompletableFuture<R> {
    LOGGER.info("{} call with param: {}", methodName, arg)
    return serverIsRunning(methodName) ?: runAsync(methodName) { function(it, arg) }
  }

  fun <R> handleRequest(methodName: String, function: (CancelChecker) -> R): CompletableFuture<R> {
    LOGGER.info("{} call", methodName)
    return serverIsRunning(methodName) ?: runAsync(
      methodName,
      function,
    )
  }

  fun handleNotification(methodName: String, function: () -> Unit) {
    LOGGER.info("{} call", methodName)
    function()
  }

  fun <R> handleRequest(
    methodName: String,
    supplier: (CancelChecker) -> R,
    precondition: (String) -> CompletableFuture<R>?,
  ): CompletableFuture<R> {
    LOGGER.info("{} call", methodName)
    return precondition(methodName) ?: runAsync(methodName, supplier)
  }

  private fun <T> serverIsRunning(methodName: String): CompletableFuture<T>? =
    serverIsInitialized(methodName) ?: serverIsNotFinished(methodName)

  fun <T> serverIsInitialized(methodName: String): CompletableFuture<T>? =
    if (!serverLifetime.isInitialized) {
      failure(
        methodName,
        ResponseError(
          ResponseErrorCode.ServerNotInitialized,
          "Server has not been initialized yet!",
          false,
        ),
      )
    } else {
      null
    }

  fun <T> serverIsNotFinished(methodName: String): CompletableFuture<T>? =
    if (serverLifetime.isFinished) {
      failure(
        methodName,
        ResponseError(
          ResponseErrorCode.ServerNotInitialized,
          "Server has already shutdown!",
          false,
        ),
      )
    } else {
      null
    }

  private fun <T> runAsync(methodName: String, request: (CancelChecker) -> T): CompletableFuture<T> {
    val telemetryContext = Context.current()
    val asyncRequest =
      CompletableFutures.computeAsync { cancelChecker ->
        telemetryContext.makeCurrent().use { request(cancelChecker) }
      }
    return CancellableFuture
      .from(asyncRequest)
      .thenApply<Either<Throwable, T>> { Either.forRight(it) }
      .exceptionally { Either.forLeft(it) }
      .thenCompose {
        if (it.isLeft) {
          failure(
            methodName,
            it.left,
          )
        } else {
          success<T>(methodName, it.right)
        }
      }
  }

  private fun <T> success(methodName: String, response: T): CompletableFuture<T> {
    LOGGER.info("{} call finishing successfully", methodName)
    return CompletableFuture.completedFuture(response)
  }

  private fun <T> failure(methodName: String, error: ResponseError): CompletableFuture<T> {
    LOGGER.error("{} call finishing with error: {}", methodName, error)
    return CompletableFuture.failedFuture(ResponseErrorException(error))
  }

  private fun <T> failure(methodName: String, throwable: Throwable): CompletableFuture<T> {
    LOGGER.error("$methodName call finishing with error", throwable)
    if (throwable is ResponseErrorException) {
      return CompletableFuture.failedFuture(throwable)
    }
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    throwable.printStackTrace(pw)
    val message =
      """
      ${throwable.message}
      $sw
      """.trimIndent()
    return CompletableFuture.failedFuture(
      ResponseErrorException(
        ResponseError(ResponseErrorCode.InternalError, message, null),
      ),
    )
  }

  companion object {
    private val LOGGER =
      LogManager.getLogger(
        BspRequestsRunner::class.java,
      )
  }
}
