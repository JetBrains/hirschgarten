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
package org.jetbrains.bazel;

import com.google.idea.testing.TestUtils;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockComponentManager;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPoint.Kind;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.bazel.commons.EnvironmentProvider;
import org.jetbrains.bazel.commons.FileUtil;
import org.jetbrains.bazel.commons.SystemInfoProvider;
import org.jetbrains.bazel.startup.FileUtilIntellij;
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider;
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test base class.
 *
 * <p>Provides a mock application and a mock project.
 */
public class BlazeTestCase {
  /** Test rule that ensures tests do not run on Windows (see http://b.android.com/222904) */
  public static class IgnoreOnWindowsRule implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
      if (SystemInfo.isWindows) {
        return new Statement() {
          @Override
          public void evaluate() {
            System.out.println(
                "Test \""
                    + description.getDisplayName()
                    + "\" does not run on Windows (see http://b.android.com/222904)");
          }
        };
      }
      return base;
    }
  }

  @Rule public IgnoreOnWindowsRule rule = new IgnoreOnWindowsRule();

  protected MockProject project;
  private ExtensionsAreaImpl extensionsArea;
  protected Disposable testDisposable;

  private static class RootDisposable implements Disposable {
    @Override
    public void dispose() {}
  }

  /** A wrapper around the pico container used by IntelliJ's DI system */
  public static class Container {
    private final MockComponentManager componentManager;
    private final Disposable testDisposable;

    Container(MockComponentManager componentManager, Disposable testDisposable) {
      this.componentManager = componentManager;
      this.testDisposable = testDisposable;
    }

    public <T> void register(Class<T> klass, T instance) {
      componentManager.registerService(klass, instance, testDisposable);
    }
  }

  @Before
  public final void setup() {
    Registry.markAsLoaded();
    Registry.get("allow.macros.for.run.configurations").setValue(false);
    testDisposable = new RootDisposable();
    MockApplication application = TestUtils.createMockApplication(testDisposable);
    MockProject mockProject = TestUtils.mockProject(application.getPicoContainer(), testDisposable);

    extensionsArea = (ExtensionsAreaImpl) Extensions.getRootArea();

    // Initialize SystemInfoProvider for tests
    SystemInfoProvider.Companion.provideSystemInfoProvider(IntellijSystemInfoProvider.INSTANCE);

    // Initialize FileUtil for tests
    FileUtil.Companion.provideFileUtil(FileUtilIntellij.INSTANCE);

    // Initialize EnvironmentProvider for tests
    EnvironmentProvider.Companion.provideEnvironmentProvider(IntellijEnvironmentProvider.INSTANCE);

    this.project = mockProject;

    initTest(
        new Container((MockComponentManager) ApplicationManager.getApplication(), testDisposable),
        new Container(mockProject, testDisposable));
  }

  @After
  public final void tearDown() {
    Disposer.dispose(testDisposable);
  }

  public final Project getProject() {
    return project;
  }

  protected void initTest(Container applicationServices, Container projectServices) {}

  protected <T> ExtensionPointImpl<T> registerExtensionPoint(
      ExtensionPointName<T> name, Class<T> type) {
    extensionsArea.registerExtensionPoint(name.getName(), type.getName(), Kind.INTERFACE);
    return extensionsArea.getExtensionPoint(name.getName());
  }

  protected <T> ExtensionPointImpl<T> registerExtensionPointByName(String name, Class<T> type) {
    extensionsArea.registerExtensionPoint(name, type.getName(), Kind.INTERFACE);
    return extensionsArea.getExtensionPoint(name);
  }
}
