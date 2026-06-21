package org.jetbrains.bazel.python.debug

import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestApplication
class PythonDebugUtilsTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture)
  private val project by projectFixture

  @Test
  fun `pytest debug target uses runfiles package working directory`() {
    val target = Label.parse("//tests/pytest:sample_test")
    val fixture = registerPythonTarget(
      target = target,
      ruleKind = "py_test",
      ruleType = RuleType.TEST,
      imports = listOf("python/imports"),
    )

    val debugInfo = preparePythonDebug(project, target)

    assertPythonDebugInfo(fixture, debugInfo)
  }

  @Test
  fun `unittest debug target uses runfiles package working directory`() {
    val target = Label.parse("//tests/unittest:sample_test")
    val fixture = registerPythonTarget(
      target = target,
      ruleKind = "py_test",
      ruleType = RuleType.TEST,
      imports = listOf("python/imports"),
    )

    val debugInfo = preparePythonDebug(project, target)

    assertPythonDebugInfo(fixture, debugInfo)
  }

  @Test
  fun `non-test python target uses runfiles package working directory`() {
    val target = Label.parse("//tools/scripts:runner")
    val fixture = registerPythonTarget(
      target = target,
      ruleKind = "py_binary",
      ruleType = RuleType.BINARY,
      imports = listOf("tools/imports"),
    )

    val debugInfo = preparePythonDebug(project, target)

    assertPythonDebugInfo(fixture, debugInfo)
  }

  @Test
  fun `python path includes workspace imports and external site packages`(@TempDir tempDir: Path) {
    val runfilesRoot = tempDir.resolve("test_runner.runfiles").createDirectories()
    val workspaceRoot = runfilesRoot.resolve("_main").createDirectories()
    val importedRoot = workspaceRoot.resolve("python/imports").createDirectories()
    val sitePackages = runfilesRoot.resolve("rules_python++pip+pypi__pytest/site-packages").createDirectories()

    val entries = buildPythonPath(workspaceRoot, listOf("python/imports")).split(File.pathSeparator)

    assertEquals(listOf(workspaceRoot.toString(), importedRoot.toString()), entries.take(2))
    assertTrue(sitePackages.toString() in entries)
  }

  @Test
  fun `python path keeps existing entries after bazel entries`(@TempDir tempDir: Path) {
    val runfilesRoot = tempDir.resolve("test_runner.runfiles").createDirectories()
    val workspaceRoot = runfilesRoot.resolve("_main").createDirectories()
    val existingRoot = tempDir.resolve("existing").createDirectories()

    val entries = buildPythonPath(workspaceRoot, emptyList(), existingRoot.toString()).split(File.pathSeparator)

    assertEquals(workspaceRoot.toString(), entries.first())
    assertEquals(existingRoot.toString(), entries.last())
  }

  @Test
  fun `working directory uses target package under runfiles when it exists`(@TempDir tempDir: Path) {
    val workspaceRoot = tempDir.resolve("test_runner.runfiles/_main").createDirectories()
    val packageRoot = workspaceRoot.resolve("foo/bar").createDirectories()

    val workingDirectory = findWorkingDirectory(workspaceRoot, Label.parse("//foo/bar:baz"))

    assertEquals(packageRoot, workingDirectory)
  }

  @Test
  fun `working directory falls back to runfiles workspace root when target package is absent`(@TempDir tempDir: Path) {
    val workspaceRoot = tempDir.resolve("test_runner.runfiles/_main").createDirectories()

    val workingDirectory = findWorkingDirectory(workspaceRoot, Label.parse("//foo/bar:baz"))

    assertEquals(workspaceRoot, workingDirectory)
  }

  private fun registerPythonTarget(
    target: Label,
    ruleKind: String,
    ruleType: RuleType,
    imports: List<String>,
  ): PythonTargetFixture {
    initializeBazelProject(project, tempDir)
    createFileIfAbsent(tempDir.resolve("MODULE.bazel"))

    val runnerScript = tempDir.resolve("bazel-out/bin").resolve(target.packagePath.toString()).resolve("${target.targetName}.py")
    runnerScript.parent.createDirectories()
    Files.writeString(runnerScript, "print('debug target')\n")

    val runfilesRoot = runnerScript.parent.resolve("${runnerScript.fileName}.runfiles").createDirectories()
    val runfilesWorkspaceRoot = runfilesRoot.resolve("_main").createDirectories()
    val packageWorkingDirectory = runfilesWorkspaceRoot.resolve(target.packagePath.toString()).createDirectories()
    val importRoots = imports.map { runfilesWorkspaceRoot.resolve(it).createDirectories() }
    val sitePackagesRoot = runfilesRoot.resolve("rules_python++pip+pypi__pytest/site-packages").createDirectories()

    project.targetUtils.setTargets(
      listOf(
        RawBuildTarget(
          key = WorkspaceTargetKey(label = target),
          dependencies = emptyList(),
          kind = TargetKind(
            kind = ruleKind,
            languageClasses = setOf(LanguageClass.PYTHON),
            ruleType = ruleType,
          ),
          sources = SourceFileCollection.EMPTY,
          generatedSources = SourceFileCollection.EMPTY,
          resources = SourceFileCollection.EMPTY,
          baseDirectory = tempDir.resolve(target.packagePath.toString()),
          data = listOf(
            PythonBuildTarget(
              version = "3.8",
              interpreter = tempDir.resolve("python3"),
              imports = imports,
              mainFile = runnerScript,
              runnerScript = runnerScript,
            ),
          ),
        ),
      ),
    )

    return PythonTargetFixture(
      target = target,
      runnerScript = runnerScript,
      runfilesRoot = runfilesRoot,
      runfilesWorkspaceRoot = runfilesWorkspaceRoot,
      packageWorkingDirectory = packageWorkingDirectory,
      importRoots = importRoots,
      sitePackagesRoot = sitePackagesRoot,
    )
  }

  private fun assertPythonDebugInfo(fixture: PythonTargetFixture, debugInfo: ReflectedPythonDebugInfo) {
    assertEquals(fixture.runnerScript, debugInfo.pythonFile)
    assertEquals(fixture.packageWorkingDirectory, debugInfo.workingDirectory)

    val environmentVariables = debugInfo.environmentVariables
    assertEquals(fixture.target.toString(), environmentVariables["BAZEL_TARGET"])
    assertEquals("_main", environmentVariables["BAZEL_WORKSPACE"])
    assertEquals(fixture.target.targetName, environmentVariables["BAZEL_TARGET_NAME"])
    assertEquals(fixture.runfilesRoot.toString(), environmentVariables["TEST_SRCDIR"])
    assertEquals("_main", environmentVariables["TEST_WORKSPACE"])

    val pythonPathEntries = environmentVariables.getValue("PYTHONPATH").split(File.pathSeparator)
    assertEquals(fixture.runfilesWorkspaceRoot.toString(), pythonPathEntries.first())
    for (importRoot in fixture.importRoots) {
      assertTrue(importRoot.toString() in pythonPathEntries)
    }
    assertTrue(fixture.sitePackagesRoot.toString() in pythonPathEntries)
  }

  private fun preparePythonDebug(project: Project, target: Label): ReflectedPythonDebugInfo {
    val utilsClass = Class.forName("org.jetbrains.bazel.python.debug.PythonDebugUtils")
    val instance = utilsClass.getField("INSTANCE").get(null)
    val parameterTypes = arrayOf(Project::class.java, Label::class.java)
    val method = utilsClass.declaredMethods.single {
      it.name.startsWith("preparePythonDebug") && it.parameterTypes.contentEquals(parameterTypes)
    }
    method.isAccessible = true
    val result = method.invoke(instance, project, target) ?: error("Expected Python debug info for $target")
    return ReflectedPythonDebugInfo(
      pythonFile = result.getField("pythonFile"),
      environmentVariables = result.getField("environmentVariables"),
      workingDirectory = result.getField("workingDirectory"),
    )
  }

  private fun buildPythonPath(workspaceRoot: Path, imports: List<String>, existingPythonPath: String? = null): String {
    val utilsClass = Class.forName("org.jetbrains.bazel.python.debug.PythonDebugUtils")
    val instance = utilsClass.getField("INSTANCE").get(null)
    val parameterTypes = arrayOf(Path::class.java, List::class.java, String::class.java)
    val method = utilsClass.declaredMethods.single {
      it.name.startsWith("buildPythonPath") && it.parameterTypes.contentEquals(parameterTypes)
    }
    method.isAccessible = true
    return method.invoke(instance, workspaceRoot, imports, existingPythonPath) as String
  }

  private fun findWorkingDirectory(workspaceRoot: Path, target: Label): Path {
    val utilsClass = Class.forName("org.jetbrains.bazel.python.debug.PythonDebugUtils")
    val instance = utilsClass.getField("INSTANCE").get(null)
    val parameterTypes = arrayOf(Path::class.java, Label::class.java)
    val method = utilsClass.declaredMethods.single {
      it.name.startsWith("findWorkingDirectory") && it.parameterTypes.contentEquals(parameterTypes)
    }
    method.isAccessible = true
    return method.invoke(instance, workspaceRoot, target) as Path
  }

  private fun createFileIfAbsent(path: Path) {
    path.parent?.createDirectories()
    if (!Files.exists(path)) {
      path.createFile()
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> Any.getField(name: String): T {
    val field = javaClass.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this) as T
  }

  private data class PythonTargetFixture(
    val target: Label,
    val runnerScript: Path,
    val runfilesRoot: Path,
    val runfilesWorkspaceRoot: Path,
    val packageWorkingDirectory: Path,
    val importRoots: List<Path>,
    val sitePackagesRoot: Path,
  )

  private data class ReflectedPythonDebugInfo(
    val pythonFile: Path,
    val environmentVariables: Map<String, String>,
    val workingDirectory: Path,
  )
}
