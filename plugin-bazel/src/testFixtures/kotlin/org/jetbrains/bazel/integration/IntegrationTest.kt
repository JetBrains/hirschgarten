/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.integration


import com.google.idea.testing.IntellijTestSetupRule
import com.google.idea.testing.ServiceHelper
import com.google.idea.testing.VerifyRequiredPluginsEnabled
import com.google.idea.testing.runfiles.Runfiles
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.*

/** Base test class for blaze integration tests. [UsefulTestCase]  */
abstract class BlazeIntegrationTestCase {
    /** Test rule that ensures tests do not run on Windows (see http://b.android.com/222904)  */
    class IgnoreOnWindowsRule : TestRule {
        override fun apply(base: Statement, description: Description): Statement {
            if (SystemInfo.isWindows) {
                return object : Statement() {
                    @Throws(Throwable::class)
                    override fun evaluate() {
                        println(
                            ("Test \""
                                    + description.getDisplayName()
                                    + "\" does not run on Windows (see http://b.android.com/222904)")
                        )
                    }
                }
            }
            return base
        }
    }

    @Rule
    val rule: IgnoreOnWindowsRule = IgnoreOnWindowsRule()

    @Rule
    val setupRule: IntellijTestSetupRule = IntellijTestSetupRule()

    @Rule
    val testRunWrapper: TestRule? = if (runTestsOnEdt()) EdtRule() else null

    protected var testFixture: CodeInsightTestFixture? = null
    protected var workspaceRoot: WorkspaceRoot = null
    protected var projectDataDirectory: VirtualFile? = null
    protected var fileSystem: TestFileSystem? = null
    protected var workspace: WorkspaceFileSystem? = null


    @Before
    @Throws(Throwable::class)
    fun setUp() {
        testFixture = createTestFixture()
        testFixture!!.setUp()
        // In 241 `setUp()` first processes events and then waits for indexing to complete. In most
        // cases indexing finishes sooner and processing events runs all startup activities, but when it
        // does not they get deferred. This seems to be a bug in the platform's test utils, which should
        // soon become irrelevant as it only affects old style `StartupActivity`es.
        EdtTestUtil.runInEdtAndWait<RuntimeException?>(ThrowableRunnable { UIUtil.dispatchAllInvocationEvents() })
        fileSystem =
            TestFileSystem(this.project, testFixture!!.tempDirFixture, this.isLightTestCase)

        runWriteAction(
            Runnable {
                val workspaceRootVirtualFile: VirtualFile = fileSystem.createDirectory("workspace")
                workspaceRoot = WorkspaceRoot(File(workspaceRootVirtualFile.getPath()))
                projectDataDirectory = fileSystem.createDirectory("project-data-dir")
                workspace = WorkspaceFileSystem(workspaceRoot, fileSystem)
            })

        BlazeImportSettingsManager.getInstance(this.project)
            .setImportSettings(
                BlazeImportSettings(
                    workspaceRoot.toString(),
                    "test-project",
                    projectDataDirectory!!.getPath(),
                    workspaceRoot.fileForPath(WorkspacePath("project-view-file")).getPath(),
                    buildSystem(),
                    ProjectType.ASPECT_SYNC
                )
            )

        registerApplicationService<T>(
            InputStreamProvider::class.java,
            object : InputStreamProvider() {
                @Throws(IOException::class)
                public override fun forFile(file: File): InputStream {
                    val vf: VirtualFile = fileSystem.findFile(file.getPath())
                    if (vf == null) {
                        throw FileNotFoundException()
                    }
                    return vf.getInputStream()
                }

                @Throws(IOException::class)
                public override fun forOutputArtifact(output: BlazeArtifact): BufferedInputStream {
                    if (output is LocalFileArtifact) {
                        return BufferedInputStream(forFile((output as LocalFileArtifact).getFile()))
                    }
                    throw RuntimeException("Can't handle output artifact type: " + output.getClass())
                }
            })
        
        if (this.isLightTestCase) {
            registerApplicationService<FileOperationProvider?>(
                FileOperationProvider::class.java, MockFileOperationProvider()
            )
            registerApplicationService<VirtualFileSystemProvider?>(
                VirtualFileSystemProvider::class.java, TempVirtualFileSystemProvider()
            )
        }

        val requiredPlugins = System.getProperty("idea.required.plugins.id")
        if (requiredPlugins != null) {
            VerifyRequiredPluginsEnabled.runCheck(requiredPlugins.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
        }
    }

    @After
    @Throws(Throwable::class)
    fun tearDown() {
        if (!this.isLightTestCase) {
            // Workaround to avoid a platform race condition that occurs when we delete a VirtualDirectory
            // whose children were affected by external file system events that RefreshQueue is still
            // processing. We only need this for heavy test cases, since light test cases perform all file
            // operations synchronously through an in-memory file system.
            // See https://youtrack.jetbrains.com/issue/IDEA-218773
            val refreshSession = RefreshQueue.getInstance().createSession(false, true, null)
            refreshSession.addFile(fileSystem.findFile(workspaceRoot.directory().getPath()))
            refreshSession.launch()
        }
        runWriteAction(
            Runnable {
                // Clear attached jdks
                val table = ProjectJdkTable.getInstance()
                for (sdk in ProjectJdkTable.getInstance().getAllJdks()) {
                    table.removeJdk(sdk)
                }
                // Clear attached libraries
                val libraryTable =
                    LibraryTablesRegistrar.getInstance().getLibraryTable(this.project)
                for (library in libraryTable.getLibraries()) {
                    libraryTable.removeLibrary(library)
                }
            })
        testFixture!!.tearDown()
        testFixture = null
        
    }

    private fun createTestFixture(): CodeInsightTestFixture {
        val factory = IdeaTestFixtureFactory.getFixtureFactory()

        if (this.isLightTestCase) {
            val fixtureBuilder =
                factory.createLightFixtureBuilder(LightJavaCodeInsightFixtureTestCase.JAVA_8, "test-project")
            val lightFixture = fixtureBuilder.getFixture()
            return factory.createCodeInsightFixture(lightFixture, LightTempDirTestFixtureImpl(true))
        }

        val fixtureBuilder =
            factory.createFixtureBuilder("test-project")
        return factory.createCodeInsightFixture(fixtureBuilder.getFixture())
    }

    protected val isLightTestCase: Boolean
        /**
         * Override to back this test with a heavy test fixture, which will actually modify files on disk
         * instead of keeping everything in memory like a light test fixture does. This can hurt test
         * performance, though we aren't sure to what extent (b/117435202).
         */
        get() = true

    /** Override to run tests off the EDT.  */
    protected fun runTestsOnEdt(): Boolean {
        return true
    }

    protected val project: Project
        get() = testFixture!!.project

    protected val testRootDisposable: Disposable
        get() = setupRule.testRootDisposable

    protected fun <T> registerApplicationService(key: Class<T>, implementation: T) {
        ServiceHelper.registerApplicationService(key, implementation, this.testRootDisposable)
    }

    protected fun <T> registerApplicationComponent(key: Class<T>, implementation: T) {
        ServiceHelper.registerApplicationComponent(key, implementation, this.testRootDisposable)
    }

    protected fun <T> registerProjectService(key: Class<T>, implementation: T) {
        ServiceHelper.registerProjectService(
            this.project, key, implementation, this.testRootDisposable
        )
    }

    fun <T> registerProjectComponent(key: Class<T>, implementation: T) {
        ServiceHelper.registerProjectComponent(
            this.project, key, implementation, this.testRootDisposable
        )
    }

    protected fun <T : Any> registerExtension(name: ExtensionPointName<T>, instance: T) {
        ServiceHelper.registerExtension(name, instance, this.testRootDisposable)
    }

    protected fun <T : Any> registerExtensionFirst(name: ExtensionPointName<T>, instance: T) {
        ServiceHelper.registerExtensionFirst(name, instance, this.testRootDisposable)
    }

    companion object {
        @Throws(Throwable::class)
        private fun runWriteAction(writeAction: Runnable) {
            EdtTestUtil.runInEdtAndWait<RuntimeException?>(
                ThrowableRunnable { ApplicationManager.getApplication().runWriteAction(writeAction) })
        }
    }
}
