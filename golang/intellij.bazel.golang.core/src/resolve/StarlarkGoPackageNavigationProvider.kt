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
// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.golang.resolve

import com.intellij.codeInsight.navigation.CtrlMouseHandler
import com.intellij.lang.documentation.DocumentationProviderEx
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.references.resolveLabel

internal class StarlarkGoPackageNavigationProvider : BazelGoPackageNavigationProvider {
  override fun findTargetDefinitionPsi(goPackage: BazelGoPackage): PsiElement? =
    goPackage.getMainLabel()?.let { resolveLabel(goPackage.project, it) }
}

/** Redirects quick navigation text on the fake file system item back to the build rule. */
internal class GoPackageDocumentationProvider : DocumentationProviderEx() {
  override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement): String? =
    if (element is GoPackageFileSystemItem) {
      CtrlMouseHandler.getInfo(element.navigationElement, originalElement)
    } else {
      null
    }
}
