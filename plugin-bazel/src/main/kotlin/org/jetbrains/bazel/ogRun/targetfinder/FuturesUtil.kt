/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.ogRun.targetfinder

import com.google.common.base.Function
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.JdkFutureAdapters
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.function.Predicate

/** Utilities operating on futures.  */
object FuturesUtil {
    private val logger = Logger.getInstance(FuturesUtil::class.java)

    /**
     * Blocks while calling get on the future. Use with care: logs a warning for [ ], and otherwise returns null on error or interrupt.
     */
    fun <T> getIgnoringErrors(future: Future<T?>): T? {
        try {
            return future.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: ExecutionException) {
            logger.warn(e)
        }
        return null
    }

    /**
     * Iterates through the futures, returning the first future satisfying the predicate. Future
     * returns null if there are no results matching the predicate.
     *
     *
     * Prioritizes immediately available results.
     */
    fun <T> getFirstFutureSatisfyingPredicate(
        iterable: Iterable<Future<T?>>, predicate: Predicate<T?>
    ): ListenableFuture<T?> {
        val futures: MutableList<ListenableFuture<T?>?> = ArrayList<ListenableFuture<T?>?>()
        for (future in iterable) {
            if (future.isDone()) {
                val result = getIgnoringErrors<T?>(future)
                if (predicate.test(result)) {
                    return Futures.immediateFuture<T?>(result)
                }
            } else {
                // we can't return ListenableFuture directly, because implementations are using different
                // versions of that class...
                futures.add(JdkFutureAdapters.listenInPoolThread<T?>(future))
            }
        }
        if (futures.isEmpty()) {
            return Futures.immediateFuture<T?>(null)
        }
        return Futures.transform<MutableList<T?>?, T?>(
            Futures.allAsList<T?>(futures),
            Function { list: MutableList<T?>? ->
                if (list == null) null else list.stream().filter(predicate).findFirst().orElse(null)
            },
            MoreExecutors.directExecutor()
        )
    }
}
