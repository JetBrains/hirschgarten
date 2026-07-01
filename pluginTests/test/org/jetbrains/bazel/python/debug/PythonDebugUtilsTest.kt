package org.jetbrains.bazel.python.debug

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
  fun `runner script type follows changed file contents`() {
    val target = Label.parse("//tools/scripts:runner")
    val fixture = registerPythonTarget(
      target = target,
      ruleKind = "py_binary",
      ruleType = RuleType.BINARY,
      imports = emptyList(),
      runnerScriptName = target.targetName,
      runnerScriptContent = "#!/usr/bin/env python\nprint('debug target')\n",
      mainFileName = "main.py",
    )

    val debugInfoBefore = preparePythonDebug(project, target)
    assertEquals(fixture.runnerScript, debugInfoBefore.pythonFile)

    Files.writeString(fixture.runnerScript, "#!/usr/bin/env bash\necho debug target\n")
    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(fixture.runnerScript)

    val debugInfoAfter = preparePythonDebug(project, target)
    assertEquals(fixture.mainFile, debugInfoAfter.pythonFile)
  }

  @Test
  fun `Bazel-declared venv interpreter is used as python binary`() {
    val target = Label.parse("//tools/scripts:runner")
    val basePythonBinary = tempDir.resolve("bazel-out/bin/rules_python/bin/python3")
    val fixture = registerPythonTarget(
      target = target,
      ruleKind = "py_binary",
      ruleType = RuleType.BINARY,
      imports = emptyList(),
      runnerScriptName = target.targetName,
      runnerScriptContent = """
        #!/usr/bin/env bash
        PYTHON_BINARY="stale/runner/script/value"
        echo debug target
      """.trimIndent(),
      mainFileName = "main.py",
      interpreter = basePythonBinary,
    )
    val venvPythonBinary = fixture.runfilesWorkspaceRoot
      .resolve(target.packagePath.toString())
      .resolve("_${target.targetName}.venv/bin/python3")
    venvPythonBinary.parent.createDirectories()
    venvPythonBinary.createFile()
    assertTrue(venvPythonBinary.toFile().setExecutable(true))
    writeRunfilesManifest(fixture, venvPythonBinary)

    val debugInfo = preparePythonDebug(project, target)

    assertEquals(fixture.mainFile, debugInfo.pythonFile)
    assertEquals(venvPythonBinary, debugInfo.pythonBinary)
  }

  @Test
  fun `target interpreter is used when runfiles contain stale undeclared venv`() {
    val target = Label.parse("//tools/scripts:runner")
    val pythonBinary = tempDir.resolve("bazel-out/bin/rules_python/bin/python3")
    val fixture = registerPythonTarget(
      target = target,
      ruleKind = "py_binary",
      ruleType = RuleType.BINARY,
      imports = emptyList(),
      runnerScriptName = target.targetName,
      runnerScriptContent = "#!/usr/bin/env bash\necho debug target\n",
      mainFileName = "main.py",
      interpreter = pythonBinary,
    )
    val staleVenvPythonBinary = fixture.runfilesWorkspaceRoot
      .resolve(target.packagePath.toString())
      .resolve("_${target.targetName}.venv/bin/python3")
    staleVenvPythonBinary.parent.createDirectories()
    staleVenvPythonBinary.createFile()
    writeRunfilesManifest(fixture)

    val debugInfo = preparePythonDebug(project, target)

    assertEquals(pythonBinary, debugInfo.pythonBinary)
  }

  @Test
  fun `missing target interpreter leaves python binary empty`() {
    val target = Label.parse("//tools/scripts:runner")
    registerPythonTarget(
      target = target,
      ruleKind = "py_binary",
      ruleType = RuleType.BINARY,
      imports = emptyList(),
      runnerScriptName = target.targetName,
      runnerScriptContent = """
        #!/usr/bin/env bash
        PYTHON_BINARY=''
        echo debug target
      """.trimIndent(),
      mainFileName = "main.py",
      interpreter = null,
    )

    val debugInfo = preparePythonDebug(project, target)

    assertNull(debugInfo.pythonBinary)
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

  @Test
  fun `python debug build arguments include debug flags additional bazel params and keep going`() {
    val arguments = buildPythonDebugBazelArguments(
      debugFlags = listOf("--debug_flag=1"),
      additionalBazelParams = "--@rules_python//python/config_settings:bootstrap_impl=system_python --flag \"two words\"",
    )

    assertEquals(
      listOf(
        "--debug_flag=1",
        "--@rules_python//python/config_settings:bootstrap_impl=system_python",
        "--flag",
        "two words",
      ),
      arguments,
    )
  }

  private fun registerPythonTarget(
    target: Label,
    ruleKind: String,
    ruleType: RuleType,
    imports: List<String>,
    runnerScriptName: String = "${target.targetName}.py",
    runnerScriptContent: String = "print('debug target')\n",
    mainFileName: String? = null,
    interpreter: Path? = tempDir.resolve("python3"),
  ): PythonTargetFixture {
    initializeBazelProject(project, tempDir)
    createFileIfAbsent(tempDir.resolve("MODULE.bazel"))

    val runnerScript = tempDir.resolve("bazel-out/bin").resolve(target.packagePath.toString()).resolve(runnerScriptName)
    runnerScript.parent.createDirectories()
    Files.writeString(runnerScript, runnerScriptContent)

    val mainFile =
      mainFileName?.let {
        tempDir.resolve(target.packagePath.toString()).resolve(it).also { mainFile ->
          mainFile.parent.createDirectories()
          Files.writeString(mainFile, "print('main file')\n")
        }
      } ?: runnerScript

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
            languageClasses = setOf(PythonLanguageClass.PYTHON),
            ruleType = ruleType,
          ),
          sources = SourceFileCollection.EMPTY,
          generatedSources = SourceFileCollection.EMPTY,
          resources = SourceFileCollection.EMPTY,
          baseDirectory = tempDir.resolve(target.packagePath.toString()),
          data = listOf(
            PythonBuildTarget(
              version = "3.8",
              interpreter = interpreter,
              imports = imports,
              mainFile = mainFile,
              runnerScript = runnerScript,
            ),
          ),
        ),
      ),
    )

    return PythonTargetFixture(
      target = target,
      runnerScript = runnerScript,
      mainFile = mainFile,
      runfilesRoot = runfilesRoot,
      runfilesWorkspaceRoot = runfilesWorkspaceRoot,
      packageWorkingDirectory = packageWorkingDirectory,
      importRoots = importRoots,
      sitePackagesRoot = sitePackagesRoot,
      interpreter = interpreter,
    )
  }

  private fun assertPythonDebugInfo(fixture: PythonTargetFixture, debugInfo: ReflectedPythonDebugInfo) {
    assertEquals(fixture.runnerScript, debugInfo.pythonFile)
    assertEquals(fixture.packageWorkingDirectory, debugInfo.workingDirectory)

    val environmentVariables = debugInfo.environmentVariables
    assertEquals(fixture.interpreter, debugInfo.pythonBinary)
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
      pythonBinary = result.getField("pythonBinary"),
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

  private fun buildPythonDebugBazelArguments(debugFlags: List<String>, additionalBazelParams: String?): List<String> {
    val stateClass = Class.forName("org.jetbrains.bazel.python.debug.PythonDebugCommandLineStateKt")
    val parameterTypes = arrayOf(List::class.java, String::class.java)
    val method = stateClass.declaredMethods.single {
      it.name.startsWith("buildPythonDebugBazelArguments") && it.parameterTypes.contentEquals(parameterTypes)
    }
    method.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return method.invoke(null, debugFlags, additionalBazelParams) as List<String>
  }

  private fun createFileIfAbsent(path: Path) {
    path.parent?.createDirectories()
    if (!Files.exists(path)) {
      path.createFile()
    }
  }

  private fun writeRunfilesManifest(fixture: PythonTargetFixture, vararg files: Path) {
    val content = files.joinToString(separator = "\n", postfix = "\n") { file ->
      val runfilesPath = fixture.runfilesRoot.relativize(file).toString().replace(File.separatorChar, '/')
      "$runfilesPath $file"
    }
    Files.writeString(fixture.runfilesRoot.resolve("MANIFEST"), content)
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
    val mainFile: Path,
    val runfilesRoot: Path,
    val runfilesWorkspaceRoot: Path,
    val packageWorkingDirectory: Path,
    val importRoots: List<Path>,
    val sitePackagesRoot: Path,
    val interpreter: Path?,
  )

  private data class ReflectedPythonDebugInfo(
    val pythonFile: Path,
    val pythonBinary: Path?,
    val environmentVariables: Map<String, String>,
    val workingDirectory: Path,
  )
}
