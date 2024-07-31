package com.google.idea.testing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.junit5.impl.TestApplicationExtension;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class BazelTestApplicationExtension implements BeforeAllCallback, AfterEachCallback, AfterAllCallback {
    private final TestApplicationExtension testApplicationExtension = new TestApplicationExtension();

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        testApplicationExtension.beforeAll(extensionContext);
        // Application is a good disposable for tests:
        // IntelliJ extension will check are there any leaks after disposing application
        // so it's safe for us to use it as a disposable parent
        BlazeTestSystemProperties.configureSystemProperties(ApplicationManager.getApplication());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        testApplicationExtension.afterEach(extensionContext);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        testApplicationExtension.afterAll(extensionContext);
    }
}
