package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.openapi.module.JavaModuleType.JAVA_MODULE_ENTITY_TYPE_ID_NAME
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.Dependency
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaAddendum
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.KotlinAddendum
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bazel.workspacemodel.entities.ScalaAddendum
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path
import kotlin.io.path.Path

fun createRawBuildTarget(
  id: Label = Label.parse("//target"),
  tags: List<String> = emptyList(),
  dependencies: List<DependencyLabel> = emptyList(),
  kind: TargetKind = TargetKind(
    kindString = "java_library",
    ruleType = RuleType.LIBRARY,
    languageClasses = setOf(LanguageClass.JAVA),
  ),
  sources: List<SourceItem> = emptyList(),
  resources: List<Path> = emptyList(),
  baseDirectory: Path = Path("/base/dir"),
  noBuild: Boolean = false,
  data: BuildTargetData? = null,
): RawBuildTarget =
  RawBuildTarget(
    id = id,
    tags = tags,
    dependencies = dependencies,
    kind = kind,
    sources = sources,
    resources = resources,
    baseDirectory = baseDirectory,
    noBuild = noBuild,
    data = data,
  )

fun createModuleDetails(
  target: RawBuildTarget = createRawBuildTarget(),
  javacOptions: List<String> = emptyList(),
  dependencies: List<DependencyLabel> = emptyList(),
  defaultJdkName: String? = null,
  jvmBinaryJars: List<Path> = emptyList(),
): ModuleDetails =
  ModuleDetails(
    target = target,
    javacOptions = javacOptions,
    dependencies = dependencies,
    defaultJdkName = defaultJdkName,
    jvmBinaryJars = jvmBinaryJars,
  )

fun createJavaModule(
  name: String = "module",
  type: String = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
  dependencies: List<Dependency> = emptyList(),
  associates: List<String> = emptyList(),
  kind: TargetKind = TargetKind(
    kindString = "java_library",
    ruleType = RuleType.LIBRARY,
    languageClasses = setOf(LanguageClass.JAVA),
  ),
  baseDirContentRoot: ContentRoot = ContentRoot(path = Path("/base/dir")),
  sourceRoots: List<JavaSourceRoot> = emptyList(),
  resourceRoots: List<ResourceRoot> = emptyList(),
  jvmJdkName: String? = null,
  jvmBinaryJars: List<Path> = emptyList(),
  kotlinAddendum: KotlinAddendum? = null,
  javaAddendum: JavaAddendum? = null,
  scalaAddendum: ScalaAddendum? = null,
): JavaModule =
  JavaModule(
    genericModuleInfo = GenericModuleInfo(
      name = name,
      type = ModuleTypeId(type),
      dependencies = dependencies,
      associates = associates,
      kind = kind,
    ),
    baseDirContentRoot = baseDirContentRoot,
    sourceRoots = sourceRoots,
    resourceRoots = resourceRoots,
    jvmJdkName = jvmJdkName,
    jvmBinaryJars = jvmBinaryJars,
    kotlinAddendum = kotlinAddendum,
    javaAddendum = javaAddendum,
    scalaAddendum = scalaAddendum,
  )
