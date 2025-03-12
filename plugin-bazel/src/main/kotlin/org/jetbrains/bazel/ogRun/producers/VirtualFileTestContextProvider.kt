/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.producers

import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.idea.blaze.base.model.primitives.WorkspacePath
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.ide.PooledThreadExecutor
import java.io.File
import java.util.*
import java.util.concurrent.Callable

/**
 * For situations where psi elements for the current file can't be efficiently resolved (for
 * example, files outside the current project). Uses rough heuristics to recognize test contexts,
 * then does everything else asynchronously.
 */
internal class VirtualFileTestContextProvider : TestContextProvider {
  override fun getTestContext(context: ConfigurationContext): RunConfigurationContext? {
    val psi = context.getPsiLocation()
    if (psi !is PsiFileSystemItem || psi !is FakePsiElement) {
      return null
    }
    val vf = (psi as PsiFileSystemItem).getVirtualFile()
    if (vf == null) {
      return null
    }
    val path: WorkspacePath? = getWorkspacePath(context.getProject(), vf)
    if (path == null) {
      return null
    }
    return CachedValuesManager.getCachedValue<T?>(
      psi,
      CachedValueProvider {
        CachedValueProvider.Result.create<T?>(
          doFindTestContext(context, vf, psi, path),
          PsiModificationTracker.MODIFICATION_COUNT,
          BlazeSyncModificationTracker.getInstance(context.getProject()),
        )
      },
    )
  }

  private fun doFindTestContext(
    context: ConfigurationContext,
    vf: VirtualFile,
    psi: PsiElement?,
    path: WorkspacePath,
  ): RunConfigurationContext? {
    val relevantExecutors: ImmutableSet<ExecutorType?> =
      Arrays
        .stream<HeuristicTestIdentifier?>(HeuristicTestIdentifier.EP_NAME.extensions)
        .map<ImmutableSet<ExecutorType>?> { h: HeuristicTestIdentifier? -> h!!.supportedExecutors(path) }
        .flatMap<ExecutorType> { obj: ImmutableSet<ExecutorType?>? -> obj!!.stream() }
        .collect(ImmutableSet.toImmutableSet<Any?>())
    if (relevantExecutors.isEmpty()) {
      return null
    }

    val future: ListenableFuture<RunConfigurationContext?> =
      EXECUTOR.submit<RunConfigurationContext?>(Callable { findContextAsync(resolveContext(context, vf)) })
    return TestContext
      .builder(psi, relevantExecutors)
      .setContextFuture(future)
      .setDescription(vf.getNameWithoutExtension())
      .build()
  }

  companion object {
    private val EXECUTOR: ListeningExecutorService = MoreExecutors.listeningDecorator(PooledThreadExecutor.INSTANCE)

    private fun findContextAsync(context: ConfigurationContext?): RunConfigurationContext? =
      Arrays
        .stream<TestContextProvider?>(TestContextProvider.EP_NAME.extensions)
        .filter { p: TestContextProvider? -> p !is VirtualFileTestContextProvider }
        .map<RunConfigurationContext?> { p: TestContextProvider? ->
          ReadAction.compute<RunConfigurationContext?, RuntimeException?>(
            ThrowableComputable { p!!.getTestContext(context) },
          )
        }.filter { obj: RunConfigurationContext? -> Objects.nonNull(obj) }
        .findFirst()
        .orElse(null)

    private fun resolveContext(context: ConfigurationContext, vf: VirtualFile): ConfigurationContext {
      val psi =
        ReadAction.compute<PsiFile?, RuntimeException?>(
          ThrowableComputable {
            PsiManager.getInstance(context.getProject()).findFile(vf)
          },
        )
      val location = PsiLocation.fromPsiElement<PsiFile?>(psi, context.getModule())
      return if (location == null) {
        context
      } else {
        ConfigurationContext.createEmptyContextForLocation(location)
      }
    }

    private fun getWorkspacePath(project: Project?, vf: VirtualFile): WorkspacePath? {
      val resolver: WorkspacePathResolver? =
        WorkspacePathResolverProvider.getInstance(project).getPathResolver()
      if (resolver == null) {
        return null
      }
      return resolver.getWorkspacePath(File(vf.getPath()))
    }
  }
}
