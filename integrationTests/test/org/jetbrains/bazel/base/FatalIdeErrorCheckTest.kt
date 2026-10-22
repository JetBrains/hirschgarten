package org.jetbrains.bazel.base

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Checks which IDE errors fail a step of an IDE Starter test.
 *
 * The error texts with a build id come from TeamCity failures of the IDE Starter tests. They are cut to the first frames
 * and to the frames of the Bazel plugin.
 */
internal class FatalIdeErrorCheckTest {
  @Test
  fun `an EDT assertion of the platform at IDE start does not fail the sync`() {
    assertNull(fatalIdeErrorsFailure("Bazel sync", listOf(PLUGIN_UPDATES_EDT_ASSERTION)))
  }

  @Test
  fun `a slow listener of the XML plugin does not fail the sync`() {
    assertNull(fatalIdeErrorsFailure("Bazel sync", listOf(SLOW_XML_LISTENER)))
  }

  @Test
  fun `a registry read of the test driver before the components load does not fail the sync`() {
    assertNull(fatalIdeErrorsFailure("Bazel sync", listOf(EARLY_REGISTRY_READ)))
  }

  @Test
  fun `an error in a coroutine of the Bazel plugin fails the sync`() {
    val failure = fatalIdeErrorsFailure("Bazel sync", listOf(PLUGIN_UPDATES_EDT_ASSERTION, SLOW_XML_LISTENER, BAZEL_TARGETS_PANEL_COLOR))
    assertNotNull(failure)
    assertTrue(failure!!.startsWith("Fatal IDE errors detected during Bazel sync"), failure)
    assertTrue("BazelTargetsPanel" in failure, failure)
    assertTrue("PluginUpdatesService" !in failure && "XmlFileHighlighter" !in failure, "Only the errors of the Bazel plugin fail the step: $failure")
  }

  @Test
  fun `the report lists only the errors that do not fail the sync`() {
    val report = otherFatalIdeErrorsReport("Bazel sync", listOf(PLUGIN_UPDATES_EDT_ASSERTION, BAZEL_TARGETS_PANEL_COLOR))
    assertNotNull(report)
    assertTrue("PluginUpdatesService" in report!! && "BazelTargetsPanel" !in report, report)
    assertNull(otherFatalIdeErrorsReport("Bazel sync", listOf(BAZEL_TARGETS_PANEL_COLOR)))
  }

  @Test
  fun `a frame of a com_intellij_bazel module fails the sync`() {
    val error = IdeErrorText(null, "java.lang.IllegalStateException\n\tat com.intellij.bazel.python.sync.PythonSyncHook.onSync(PythonSyncHook.kt:40)")
    assertNotNull(fatalIdeErrorsFailure("Bazel sync", listOf(error)))
  }

  @Test
  fun `a Bazel plugin frame in a cause fails the sync`() {
    val error = IdeErrorText(
      "Cannot apply the sync result",
      """
      java.lang.IllegalStateException: Cannot apply the sync result
      	at com.intellij.platform.backend.workspace.WorkspaceModelImpl.update(WorkspaceModelImpl.kt:200)
      Caused by: java.lang.NullPointerException
      	at org.jetbrains.bsp.protocol.BuildTarget.getBaseDirectory(BuildTarget.kt:31)
      """.trimIndent(),
    )
    assertNotNull(fatalIdeErrorsFailure("Bazel sync", listOf(error)))
  }

  @Test
  fun `an error message that names the Bazel plugin fails the sync`() {
    val error = IdeErrorText("Cannot create the extension (plugin: org.jetbrains.bazel)", "java.lang.Throwable")
    assertNotNull(fatalIdeErrorsFailure("Bazel sync", listOf(error)))
  }

  @Test
  fun `a short UI freeze in Bazel code does not fail the sync`() {
    assertNull(fatalIdeErrorsFailure("Bazel sync", listOf(uiFreezeInBazelCode(durationMs = 5_000))))
  }

  @Test
  fun `a long UI freeze in Bazel code fails the sync`() {
    assertNotNull(fatalIdeErrorsFailure("Bazel sync", listOf(uiFreezeInBazelCode(durationMs = 15_000))))
  }

  @Test
  fun `a name that only starts like the Bazel plugin does not fail the sync`() {
    val error = IdeErrorText(null, "java.lang.Throwable\n\tat org.jetbrains.bazelisk.Downloader.run(Downloader.kt:1)")
    assertNull(fatalIdeErrorsFailure("Bazel sync", listOf(error)))
  }

  private fun uiFreezeInBazelCode(durationMs: Int) = IdeErrorText(
    "UI was frozen for ${durationMs}ms",
    """
    com.intellij.diagnostic.Freeze: UI was frozen for ${durationMs}ms
    	at org.jetbrains.bazel.sync.status.SyncStatusService.finishSync(SyncStatusService.kt:54)
    """.trimIndent(),
  )
}

/** Build 1090731737, 2026-10-13. */
private val PLUGIN_UPDATES_EDT_ASSERTION = IdeErrorText(
  null,
  $$"""
  com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments: Access is allowed from Event Dispatch Thread (EDT) only
  	at com.intellij.util.concurrency.ThreadingAssertions.createThreadRequirementException(ThreadingAssertions.java:339)
  	at com.intellij.util.concurrency.ThreadingAssertions.softAssertEventDispatchThread(ThreadingAssertions.java:134)
  	at com.intellij.ide.plugins.newui.PluginUpdatesService.subscribe(PluginUpdatesService.kt:142)
  	at com.intellij.openapi.updateSettings.impl.UpdateSettingsEntryPointActionProvider.initPluginsListeners(UpdateSettingsEntryPointActionProvider.java:117)
  	at com.intellij.openapi.updateSettings.impl.UpdateSettingsEntryPointActionProvider$LifecycleListener.appStarted(UpdateSettingsEntryPointActionProvider.java:66)
  """.trimIndent(),
)

/** Build 1101902491, 2026-10-21. */
private val SLOW_XML_LISTENER = IdeErrorText(
  $$"(EDT) ExtensionPoint listener `class com.intellij.ide.highlighter.XmlFileHighlighter$AttributeArrayMapStorage$EmbeddedTokenHighlighterExtensionPointListener` notification took too long: 600.947287ms (plugin: com.intellij.xml)",
  $$"""
  java.lang.Throwable: (EDT) ExtensionPoint listener `class com.intellij.ide.highlighter.XmlFileHighlighter$AttributeArrayMapStorage$EmbeddedTokenHighlighterExtensionPointListener` notification took too long: 600.947287ms
  	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:377)
  	at com.intellij.openapi.extensions.impl.ExtensionPointImpl.notifyListeners$withEdtTimeQuotaCheck(ExtensionPointImpl.kt:631)
  	at com.intellij.openapi.extensions.impl.ExtensionPointImpl.notifyListeners(ExtensionPointImpl.kt:658)
  	at com.intellij.openapi.extensions.impl.ExtensionPointImpl.addExtensionPointListener(ExtensionPointImpl.kt:689)
  	at com.intellij.ide.highlighter.XmlFileHighlighter$AttributeArrayMapStorage._init_$lambda$0(XmlFileHighlighter.kt:148)
  	at org.intellij.plugins.markdown.highlighting.MarkdownSyntaxHighlighter.getTokenHighlights(MarkdownSyntaxHighlighter.kt:22)
  """.trimIndent(),
)

/** Build 1101902482, 2026-10-21. */
private val EARLY_REGISTRY_READ = IdeErrorText(
  "Should be called at least in the state COMPONENTS_LOADED, the current state is: CONFIGURATION_STORE_INITIALIZED\nCurrent violators count: 1",
  $$"""
  java.lang.Throwable
  	at com.intellij.diagnostic.LoadingState.logStateError(LoadingState.java:64)
  	at com.intellij.diagnostic.LoadingState.checkOccurred(LoadingState.java:60)
  	at com.intellij.openapi.util.registry.Registry$Companion.getInstance(Registry.kt:207)
  	at com.intellij.openapi.util.registry.Registry$Companion.get(Registry.kt:75)
  	at com.intellij.openapi.util.registry.Registry.get(Registry.kt)
  	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
  	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
  	at com.intellij.driver.impl.Invoker.invokeMethod(Invoker.java:259)
  """.trimIndent(),
)

/** Build 1101425484, 2026-10-21. */
private val BAZEL_TARGETS_PANEL_COLOR = IdeErrorText(
  "Unhandled exception in [ProjectImpl@1594246322, ClientId(value=Host), ExecutionInitiatorPrecursor, CoroutineName((ProjectImpl@1594246322 x (ApplicationImpl@265827773 x org.jetbrains.bazel))), Dispatchers.Default]",
  $$"""
  java.lang.AssertionError: default color is not specified for ScrollBar.background
  	at com.intellij.ui.components.ScrollBarPainter.getColor(ScrollBarPainter.java:153)
  	at com.intellij.ui.components.ScrollBarPainter.lambda$setBackground$0(ScrollBarPainter.java:195)
  	at com.intellij.ui.components.JBScrollPane.<init>(JBScrollPane.java:127)
  	at org.jetbrains.bazel.ui.widgets.tool.window.components.BazelTargetsPanel.<init>(BazelTargetsPanel.kt:23)
  	at org.jetbrains.bazel.ui.widgets.tool.window.all.targets.BazelAllTargetsWidgetFactory$manage$2$targetPanel$1.invokeSuspend(BazelAllTargetsWidgetFactory.kt:79)
  """.trimIndent(),
)
