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
    val psi = context.psiLocation
    if (psi !is PsiFileSystemItem || psi !is FakePsiElement) {
      return null
    }
    val vf = (psi as PsiFileSystemItem).virtualFile ?: return null
    val path: WorkspacePath? = getWorkspacePath(context.project, vf) ?: return null
    return CachedValuesManager.getCachedValue(
      psi,
    ) {
      CachedValueProvider.Result.create(
        doFindTestContext(context, vf, psi, path),
        PsiModificationTracker.MODIFICATION_COUNT,
        BlazeSyncModificationTracker.getInstance(context.project),
      )
    }
  }

  private fun doFindTestContext(
    context: ConfigurationContext,
    vf: VirtualFile,
    psi: PsiElement?,
    path: WorkspacePath,
  ): RunConfigurationContext? {
    val relevantExecutors: Set<ExecutorType?> =
      HeuristicTestIdentifier.EP_NAME.extensionList
        .flatMap { it.supportedExecutors(path) }

    if (relevantExecutors.isEmpty()) {
      return null
    }

    val future: ListenableFuture<RunConfigurationContext?> =
      EXECUTOR.submit<RunConfigurationContext?> { findContextAsync(resolveContext(context, vf)) }
    return TestContext
      .builder(psi, relevantExecutors)
      .setContextFuture(future)
      .setDescription(vf.nameWithoutExtension)
      .build()
  }

  companion object {
    private val EXECUTOR: ListeningExecutorService = MoreExecutors.listeningDecorator(PooledThreadExecutor.INSTANCE)

    private fun findContextAsync(context: ConfigurationContext): RunConfigurationContext? =
      TestContextProvider.EP_NAME.extensionList
        .filter {  it !is VirtualFileTestContextProvider }
        .firstNotNullOfOrNull {
          ReadAction.compute<RunConfigurationContext, RuntimeException> { it.getTestContext(context) }
        }

    private fun resolveContext(context: ConfigurationContext, vf: VirtualFile): ConfigurationContext {
      val psi =
        ReadAction.compute<PsiFile, RuntimeException> {
          PsiManager.getInstance(context.project).findFile(vf)
        }
      val location = PsiLocation.fromPsiElement(psi, context.module)
      return if (location == null) {
        context
      } else {
        ConfigurationContext.createEmptyContextForLocation(location)
      }
    }

    private fun getWorkspacePath(project: Project, vf: VirtualFile): WorkspacePath? {
      val resolver: WorkspacePathResolver? =
        WorkspacePathResolverProvider.getInstance(project).getPathResolver() ?: return null
      return resolver.getWorkspacePath(File(vf.path))
    }
  }
}
