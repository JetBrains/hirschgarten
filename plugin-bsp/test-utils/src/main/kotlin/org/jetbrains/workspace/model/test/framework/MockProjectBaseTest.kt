package org.jetbrains.workspace.model.test.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Path

public open class MockProjectBaseTest {
  protected lateinit var project: Project
  protected lateinit var virtualFileManager: VirtualFileManager

  @BeforeEach
  protected open fun beforeEach() {
    project = emptyProjectTestMock()
    virtualFileManager = VirtualFileManager.getInstance()
  }

  private fun emptyProjectTestMock(): Project {
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val fixtureBuilder = factory.createFixtureBuilder("test", true)
    val fixture = fixtureBuilder.fixture
    fixture.setUp()

    return fixture.project
  }

  protected fun Path.toVirtualFile(): VirtualFile = virtualFileManager.findFileByNioPath(this)!!
}
