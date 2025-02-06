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
package org.jetbrains.plugins.bsp.golang.run.smrunner

import com.google.idea.blaze.base.run.smrunner.SmRunnerUtils.GENERIC_SUITE_PROTOCOL

/** Locate go test packages / functions for test UI navigation.  */
class BlazeGoTestLocator private constructor() : SMTestLocator {
  override fun getLocation(
    protocol: String,
    path: String,
    project: com.intellij.openapi.project.Project,
    scope: com.intellij.psi.search.GlobalSearchScope?
  ): MutableList<com.intellij.execution.Location<*>?> {
    when (protocol) {
      GENERIC_SUITE_PROTOCOL -> return com.google.idea.blaze.golang.run.smrunner.BlazeGoTestLocator.Companion.findTestPackage(
        project,
        path,
      )

      GENERIC_TEST_PROTOCOL -> return com.google.idea.blaze.golang.run.smrunner.BlazeGoTestLocator.Companion.findTestFunction(
        project,
        path,
      )

      else -> return com.google.common.collect.ImmutableList.of<com.intellij.execution.Location<*>?>()
    }
  }

  companion object {
    val INSTANCE: BlazeGoTestLocator = com.google.idea.blaze.golang.run.smrunner.BlazeGoTestLocator()

    private fun findTestPackage(
      project: com.intellij.openapi.project.Project,
      labelString: String?
    ): MutableList<com.intellij.execution.Location<*>?> {
      val target: TargetIdeInfo? =
        com.google.idea.blaze.golang.run.smrunner.BlazeGoTestLocator.Companion.getGoTestTarget(
          project,
          labelString,
        )
      if (target == null) {
        return com.google.common.collect.ImmutableList.of<com.intellij.execution.Location<*>?>()
      }
      // Exactly one source file, we'll go to the file.
      if (target.getSources().size() === 1) {
        val goFiles: MutableList<com.intellij.openapi.vfs.VirtualFile?> =
          com.google.idea.blaze.golang.run.smrunner.BlazeGoTestLocator.Companion.getGoFiles(project, target)
        if (!goFiles.isEmpty()) {
          val psiFile: com.intellij.psi.PsiFile? =
            com.intellij.psi.PsiManager.getInstance(project).findFile(goFiles.get(0))
          if (psiFile != null) {
            return com.google.common.collect.ImmutableList.of<com.intellij.execution.Location<*>?>(
              com.intellij.execution.PsiLocation<com.intellij.psi.PsiFile?>(
                psiFile,
              ),
            )
          }
        }
      }
      // More than one source file or we failed to get one source file, we'll point to the rule.
      val rule: com.intellij.psi.PsiElement? =
        com.google.idea.blaze.golang.run.smrunner.BlazeGoTestLocator.Companion.getTargetRule(
          project,
          target.getKey().getLabel(),
        )
      return if (rule != null) com.google.common.collect.ImmutableList.of<com.intellij.execution.Location<*>?>(
        com.intellij.execution.PsiLocation<com.intellij.psi.PsiElement?>(
          rule,
        ),
      ) else com.google.common.collect.ImmutableList.of<com.intellij.execution.Location<*>?>()
    }

    private fun getTargetRule(
      project: com.intellij.openapi.project.Project?,
      label: Label
    ): com.intellij.psi.PsiElement? {
      val buildReferenceManager: BuildReferenceManager = BuildReferenceManager.getInstance(project)
      val rule: com.intellij.psi.PsiElement? = buildReferenceManager.resolveLabel(label)
      if (rule is StarlarkCallExpression) {
        val kind: Kind? = (rule as StarlarkCallExpression).getRuleKind()
        return if (kind != null && kind.hasLanguage(LanguageClass.GO)
          && kind.getRuleType().equals(RuleType.TEST)
        )
          rule
        else
          null
      }
      // couldn't find the rule, this might be from a web_test
      val targetName = label.targetName().toString()
      return if (rule == null && targetName.endsWith("_wrapped_test"))
        buildReferenceManager.resolveLabel(
          label.withTargetName(targetName.substring(0, targetName.lastIndexOf("_wrapped_test"))),
        )
      else
        null
    }

    /**
     * @param path for function "TestFoo" in target "//foo/bar:baz" would be "//foo/bar:baz::TestFoo".
     * See [BlazeGoTestEventsHandler.testLocationUrl].
     */
    private fun findTestFunction(
      project: com.intellij.openapi.project.Project,
      path: String
    ): MutableList<com.intellij.execution.Location<*>?> {
      val parts: Array<String> =
        path.split(SmRunnerUtils.TEST_NAME_PARTS_SPLITTER.toRegex()).dropLastWhile { it.isEmpty() }
          .toTypedArray()
      if (parts.size != 2) {
        return com.google.common.collect.ImmutableList.of<com.intellij.execution.Location<*>?>()
      }
      val labelString: String? = parts[0]
      val functionName = parts[1]
      val target: TargetIdeInfo? =
        com.google.idea.blaze.golang.run.smrunner.BlazeGoTestLocator.Companion.getGoTestTarget(
          project,
          labelString,
        )
      if (target == null) {
        return com.google.common.collect.ImmutableList.of<com.intellij.execution.Location<*>?>()
      }
      val goFiles: MutableList<com.intellij.openapi.vfs.VirtualFile?> =
        com.google.idea.blaze.golang.run.smrunner.BlazeGoTestLocator.Companion.getGoFiles(project, target)
      if (goFiles.isEmpty()) {
        return com.google.common.collect.ImmutableList.of<com.intellij.execution.Location<*>?>()
      }
      val scope: com.intellij.psi.search.GlobalSearchScope =
        com.intellij.psi.search.GlobalSearchScope.FilesScope.filesScope(project, goFiles)
      val functions: MutableCollection<GoFunctionDeclaration?> =
        com.intellij.psi.stubs.StubIndex.getElements<String?, GoFunctionDeclaration?>(
          GoFunctionIndex.KEY, functionName, project, scope, GoFunctionDeclaration::class.java,
        )
      return functions.stream()
        .map<com.intellij.execution.PsiLocation<GoFunctionDeclaration?>?> { psiElement: GoFunctionDeclaration? ->
          PsiLocation(psiElement)
        }.collect(java.util.stream.Collectors.toList())
    }

    private fun getGoTestTarget(
      project: com.intellij.openapi.project.Project?,
      labelString: String?
    ): TargetIdeInfo? {
      val label: Label? = Label.createIfValid(labelString)
      if (label == null) {
        return null
      }
      val projectData: BlazeProjectData? =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
      if (projectData == null) {
        return null
      }
      val target: TargetIdeInfo? = projectData.getTargetMap().get(TargetKey.forPlainTarget(label))
      if (target != null && target.getKind().hasLanguage(LanguageClass.GO)
        && target.getKind().getRuleType().equals(RuleType.TEST)
      ) {
        return target
      }
      return null
    }

    private fun getGoFiles(
      project: com.intellij.openapi.project.Project?,
      target: TargetIdeInfo?
    ): MutableList<com.intellij.openapi.vfs.VirtualFile?> {
      if (target == null || target.getGoIdeInfo() == null) {
        return com.google.common.collect.ImmutableList.of<com.intellij.openapi.vfs.VirtualFile?>()
      }
      val projectData: BlazeProjectData? =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
      val lfs: com.intellij.openapi.vfs.LocalFileSystem = VirtualFileSystemProvider.getInstance().getSystem()
      if (projectData == null) {
        return com.google.common.collect.ImmutableList.of<com.intellij.openapi.vfs.VirtualFile?>()
      }
      return target.getGoIdeInfo().getSources().stream()
        .map(projectData.getArtifactLocationDecoder()::resolveSource)
        .filter({ obj: Any? -> java.util.Objects.nonNull(obj) })
        .map({ file: java.io.File? -> lfs.findFileByIoFile(file) })
        .collect(java.util.stream.Collectors.toList())
    }
  }
}
