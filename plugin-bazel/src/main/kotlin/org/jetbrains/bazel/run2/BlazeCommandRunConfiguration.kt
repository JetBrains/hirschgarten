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
package org.jetbrains.bazel.run2

import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.jetbrains.bazel.commons.command.BlazeCommandName
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.execution.configurations.ModuleRunProfile
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import org.jdom.Element
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.TargetPattern
import org.jetbrains.bazel.run2.confighandler.BlazeCommandRunConfigurationHandler
import org.jetbrains.bazel.run2.confighandler.BlazeCommandRunConfigurationHandlerProvider
import org.jetbrains.bazel.run2.confighandler.BlazeCommandRunConfigurationRunner
import org.jetbrains.bazel.run2.state.ConsoleOutputFileSettingsUi
import org.jetbrains.bazel.run2.state.RunConfigurationState
import org.jetbrains.bazel.run2.targetfinder.FuturesUtil
import org.jetbrains.bazel.run2.targetfinder.TargetFinder
import org.jetbrains.bazel.run2.ui.TargetExpressionListUi
import java.awt.event.ItemListener
import javax.swing.Box
import javax.swing.JComponent
import kotlin.concurrent.Volatile

/** A run configuration which executes Blaze commands.  */
class BlazeCommandRunConfiguration
  (project: Project, factory: ConfigurationFactory, name: String?) :
  LocatableConfigurationBase<LocatableRunConfigurationOptions>(project, factory, name), BlazeRunConfiguration,
  ModuleRunProfile, RunConfigurationWithSuppressedDefaultDebugAction {
  /** The blaze-specific parts of the last serialized state of the configuration.  */
  private var blazeElementState = Element(BLAZE_SETTINGS_TAG)

  /**
   * Used when we don't yet know all the configuration details, but want to provide a 'run/debug'
   * context action anyway.
   */
  @Volatile
  private var pendingContext: PendingRunConfigurationContext? = null

  /** Set up a run configuration with a not-yet-known target pattern.  */
  fun setPendingContext(pendingContext: PendingRunConfigurationContext) {
    this.pendingContext = pendingContext
    this.targetPatterns = ImmutableList.of<String?>()
    this.targetKindString = null
    this.contextElementString = pendingContext.getSourceElementString()
    updateHandler()
    EventLoggingService.getInstance().logEvent(javaClass, "async-run-config")
  }

  fun clearPendingContext() {
    this.pendingContext = null
  }

  /**
   * Returns true if this was previously a pending run configuration, but it turned out to be
   * invalid. We remove these from the project periodically.
   */
  fun pendingSetupFailed(): Boolean {
    val pendingContext = this.pendingContext
    if (pendingContext == null || !pendingContext.isDone) {
      return false
    }
    if (targetPatterns.isEmpty()) {
      return true
    }
    // setup failed, but it still has useful information (perhaps the user modified it?)
    this.pendingContext = null
    return false
  }

  fun getPendingContext(): PendingRunConfigurationContext? {
    return pendingContext
  }

  @Volatile
  private var targetPatterns: List<String> = listOf()

  // null if the target is null or not a single Label
  @Volatile
  private var targetKindString: String? = null

  // used to recognize previously created pending targets by their corresponding source element
  @Volatile
  var contextElementString: String? = null
    private set

  // for keeping imported configurations in sync with their source XML
  override var keepInSync: Boolean? = null

  private var handlerProvider: BlazeCommandRunConfigurationHandlerProvider
  private var handler: BlazeCommandRunConfigurationHandler

  init {
    // start with whatever fallback is present
    handlerProvider =
      BlazeCommandRunConfigurationHandlerProvider.findHandlerProvider(
        BlazeCommandRunConfigurationHandlerProvider.TargetState.KNOWN,
        null
      )
    handler = handlerProvider.createHandler(this)
    try {
      handler.state.readExternal(blazeElementState)
    } catch (e: InvalidDataException) {
      logger.error(e)
    }
  }

  /** @return The configuration's [BlazeCommandRunConfigurationHandler].
   */
  fun getHandler(): BlazeCommandRunConfigurationHandler {
    return handler
  }

  /**
   * Gets the configuration's handler's [RunConfigurationState] if it is an instance of the
   * given class; otherwise returns null.
   */
  fun <T : RunConfigurationState> getHandlerStateIfType(type: Class<T>): T? {
    val handlerState: RunConfigurationState = handler.state
    return if (type.isInstance(handlerState)) {
      type.cast(handlerState)
    } else {
      null
    }
  }

  public override fun setKeepInSync(keepInSync: Boolean) {
    this.keepInSync = keepInSync
  }

  override fun getKeepInSync(): Boolean {
    return keepInSync
  }

  override val targets: ImmutableList<TargetPattern>
    get() = parseTargets(targetPatterns)

  val singleTarget: TargetPattern?
    /**
     * Returns the single target expression represented by this configuration, or null if there isn't
     * exactly one.
     */
    get() {
      val targets: ImmutableList<TargetPattern> = targets
      return if (targets.size == 1) targets.get(0) else null
    }

  fun setTargetInfo(target: BspTargetInfo.TargetInfo) {
    val pattern = target.label.toString().trim()
    targetPatterns = if (pattern.isEmpty()) ImmutableList.of<String?>() else ImmutableList.of<String?>(pattern)
    updateTargetKind(target.kindString)
  }

  fun setTargets(targets: ImmutableList<TargetPattern?>) {
    targetPatterns =
      targets.map(TargetPattern::toString)
    updateTargetKindAsync(null)
  }

  /** Sets the target expression and asynchronously kicks off a target kind update.  */
  fun setTarget(target: TargetPattern?) {
    targetPatterns =
      if (target != null) ImmutableList.of<E?>(target.toString().trim()) else ImmutableList.of<String?>()
    updateTargetKindAsync(null)
  }

  private fun updateHandler() {
    val handlerProvider: BlazeCommandRunConfigurationHandlerProvider =
      BlazeCommandRunConfigurationHandlerProvider.findHandlerProvider(
        this.targetState, this.targetKind
      )
    updateHandlerIfDifferentProvider(handlerProvider)
  }

  private val targetState: BlazeCommandRunConfigurationHandlerProvider.TargetState
    get() = if (targetPatterns.isEmpty() && pendingContext != null)
      BlazeCommandRunConfigurationHandlerProvider.TargetState.PENDING
    else
      BlazeCommandRunConfigurationHandlerProvider.TargetState.KNOWN

  private fun updateHandlerIfDifferentProvider(
    newProvider: BlazeCommandRunConfigurationHandlerProvider
  ) {
    if (handlerProvider === newProvider) {
      return
    }
    try {
      handler.state.writeExternal(blazeElementState)
    } catch (e: WriteExternalException) {
      logger.error(e)
    }
    handlerProvider = newProvider
    handler = newProvider.createHandler(this)
    try {
      handler.state.readExternal(blazeElementState)
    } catch (e: InvalidDataException) {
      logger.error(e)
    }
  }

  val targetKind: TargetKind?
    /**
     * Returns the [TargetKind] of the single blaze target corresponding to the configuration's target
     * expression, if it's currently known. Returns null if the target expression points to multiple
     * blaze targets.
     */
    get() = TargetKind.fromRuleName(targetKindString)

  /**
   * Queries the kind of the current target pattern, possibly asynchronously, in the case where
   * there's only a single target.
   *
   * @param asyncCallback if the kind is updated asynchronously, this will be run after the kind is
   * updated. If it's updated synchronously, this will not be run.
   */
  fun updateTargetKindAsync(asyncCallback: Runnable?) {
    val targets: ImmutableList<TargetPattern> = parseTargets(targetPatterns)
    if (targets.size != 1 || targets.get(0) !is Label) {
      // TODO(brendandouglas): any reason to support multiple targets here?
      updateTargetKind(null)
      return
    }
    val label: Label? = targets.get(0) as Label?
    val future: ListenableFuture<TargetInfo?> = TargetFinder.findTargetInfoFuture(getProject(), label)
    if (future.isDone) {
      updateTargetKindFromSingleTarget(FuturesUtil.getIgnoringErrors<TargetInfo?>(future))
    } else {
      updateTargetKindFromSingleTarget(null)
      future.addListener(
        {
          updateTargetKindFromSingleTarget(FuturesUtil.getIgnoringErrors<TargetInfo?>(future))
          asyncCallback?.run()
        },
        MoreExecutors.directExecutor()
      )
    }
  }

  private fun updateTargetKindFromSingleTarget(target: TargetInfo?) {
    updateTargetKind(target?.kindString)
  }

  private fun updateTargetKind(kind: String?) {
    targetKindString = kind
    updateHandler()
  }

  private val targetKindName: String?
    /**
     * @return The [Kind] name, if the target is a known rule. Otherwise, "target pattern" if it
     * is a general [TargetPattern], "unknown rule" if it is a [Label] without a
     * known rule, and "unknown target" if there is no target.
     */
    get() {
      val kind: TargetKind? = this.targetKind
      if (kind != null) {
        return kind.toString()
      }

      val targets: ImmutableList<TargetPattern> =
        parseTargets(targetPatterns)
      if (targets.size > 1) {
        return "target patterns"
      }
      val singleTarget: TargetPattern? =
        Iterables.getFirst<TargetPattern?>(targets, null)
      if (singleTarget is Label) {
        return "unknown rule"
      } else if (singleTarget != null) {
        return "target pattern"
      } else {
        return "unknown target"
      }
    }

  @Throws(RuntimeConfigurationException::class)
  override fun checkConfiguration() {
    // Our handler check is not valid when we don't have BlazeProjectData.
    if (BlazeProjectDataManager.getInstance(project).getBlazeProjectData() == null) {
      // With query sync we don't need a sync to run a configuration
      if (Blaze.getProjectType(getProject()) !== ProjectType.QUERY_SYNC) {
        throw RuntimeConfigurationError(
          "Configuration cannot be run until project has been synced."
        )
      }
    }
    val hasBlazeBeforeRunTask =
      RunManagerEx.getInstanceEx(project).getBeforeRunTasks(this).stream()
        .anyMatch { task: BeforeRunTask<*>? -> task!!.getProviderId() == BlazeBeforeRunTaskProvider.ID && task.isEnabled() }
    if (!hasBlazeBeforeRunTask) {
      throw RuntimeConfigurationError(
        java.lang.String.format(
          "Invalid run configuration: the %s before run task is missing. Please re-run sync "
              + "to add it back",
          "Bazel"
        )
      )
    }
    handler.checkConfiguration()
    val pendingContext = this.pendingContext
    if (pendingContext != null && !pendingContext.isDone) {
      return
    }
    val targetPatterns = this.targetPatterns
    if (targetPatterns.isEmpty()) {
      if (handler.commandName !== BlazeCommandName.INFO) {
        throw RuntimeConfigurationError(
          java.lang.String.format(
            "You must specify a %s target expression.", "Bazel"
          )
        )
      }
    }
    for (pattern in targetPatterns) {
      if (handler.commandName !== BlazeCommandName.INFO) {
        if (Strings.isNullOrEmpty(pattern)) {
          throw RuntimeConfigurationError(
            java.lang.String.format(
              "You must specify a %s target expression.", "Bazel"
            )
          )
        }
      }
      if (!pattern.startsWith("//") && !pattern.startsWith("@")) {
        throw RuntimeConfigurationError(
          "You must specify the full target expression, starting with // or @"
        )
      }

      val error: String? = TargetPattern.validate(pattern)
      if (error != null) {
        throw RuntimeConfigurationError(error)
      }
    }
  }

  @Throws(InvalidDataException::class)
  override fun readExternal(element: Element) {
    var element = element
    super.readExternal(element)

    element = getBlazeSettingsCopy(element)!!

    val keepInSyncString = element.getAttributeValue(KEEP_IN_SYNC_TAG)
    keepInSync = if (keepInSyncString != null) keepInSyncString.toBoolean() else null
    contextElementString = element.getAttributeValue(CONTEXT_ELEMENT_ATTR)

    val targets = ImmutableList.builder<String?>()
    val targetElements = element.getChildren(TARGET_TAG)
    for (targetElement in targetElements) {
      if (targetElement != null && !Strings.isNullOrEmpty(targetElement.getTextTrim())) {
        targets.add(targetElement.getTextTrim())
        // backwards-compatibility with prior per-target kind serialization
        val kind = targetElement.getAttributeValue(KIND_ATTR)
        if (kind != null) {
          targetKindString = kind
        }
      }
    }
    targetPatterns = targets.build()
    val singleKind = element.getAttributeValue(KIND_ATTR)
    if (singleKind != null) {
      targetKindString = element.getAttributeValue(KIND_ATTR)
    }

    // Because BlazeProjectData is not available when configurations are loading,
    // we can't call setTarget and have it find the appropriate handler provider.
    // So instead, we use the stored provider ID.
    val providerId = element.getAttributeValue(HANDLER_ATTR)
    val handlerProvider: BlazeCommandRunConfigurationHandlerProvider? =
      BlazeCommandRunConfigurationHandlerProvider.getHandlerProvider(providerId)
    if (handlerProvider != null) {
      updateHandlerIfDifferentProvider(handlerProvider)
    }

    blazeElementState = element
    handler.state.readExternal(blazeElementState)
  }

  @Throws(WriteExternalException::class)
  override fun writeExternal(element: Element) {
    super.writeExternal(element)

    blazeElementState.removeChildren(TARGET_TAG)
    for (target in targetPatterns) {
      if (target.isEmpty()) {
        continue
      }
      val targetElement = Element(TARGET_TAG)
      targetElement.setText(target)
      blazeElementState.addContent(targetElement)
    }
    if (targetKindString != null) {
      blazeElementState.setAttribute(KIND_ATTR, targetKindString)
    }
    if (keepInSync != null) {
      blazeElementState.setAttribute(KEEP_IN_SYNC_TAG, keepInSync.toString())
    } else {
      blazeElementState.removeAttribute(KEEP_IN_SYNC_TAG)
    }
    blazeElementState.setAttribute(HANDLER_ATTR, handlerProvider.id)
    if (contextElementString != null) {
      blazeElementState.setAttribute(CONTEXT_ELEMENT_ATTR, contextElementString)
    }

    handler.state.writeExternal(blazeElementState)
    // copy our internal state to the provided Element
    element.addContent(blazeElementState.clone())
  }

  override fun clone(): BlazeCommandRunConfiguration {
    val configuration = super.clone() as BlazeCommandRunConfiguration
    configuration.blazeElementState = blazeElementState.clone()
    configuration.targetPatterns = targetPatterns
    configuration.targetKindString = targetKindString
    configuration.contextElementString = contextElementString
    configuration.pendingContext = pendingContext
    configuration.keepInSync = keepInSync
    configuration.handlerProvider = handlerProvider
    configuration.handler = handlerProvider.createHandler(this)
    try {
      configuration.handler.state.readExternal(configuration.blazeElementState)
    } catch (e: InvalidDataException) {
      logger.error(e)
    }

    return configuration
  }

  @Throws(ExecutionException::class)
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    val runner: BlazeCommandRunConfigurationRunner? = handler.createRunner(executor, environment)
    if (runner != null) {
      environment.putCopyableUserData<BlazeCommandRunConfigurationRunner?>(
        BlazeCommandRunConfigurationRunner.RUNNER_KEY,
        runner
      )
      return runner.getRunProfileState(executor, environment)
    }
    return null
  }

  override fun suggestedName(): String? {
    return handler.suggestedName(this)
  }

  override fun getConfigurationEditor(): SettingsEditor<out BlazeCommandRunConfiguration?> {
    return BlazeCommandRunConfigurationSettingsEditor(this)
  }

  override fun getModules(): Array<Module?> {
    return arrayOfNulls<Module>(0)
  }

  internal class BlazeCommandRunConfigurationSettingsEditor
    (config: BlazeCommandRunConfiguration) : SettingsEditor<BlazeCommandRunConfiguration?>() {
    private var handlerProvider: BlazeCommandRunConfigurationHandlerProvider? = null
    private var handler: BlazeCommandRunConfigurationHandler? = null
    private var handlerStateEditor: RunConfigurationStateEditor? = null
    private var handlerStateComponent: JComponent? = null
    private var elementState: Element

    private val editorWithoutSyncCheckBox: Box
    private val editor: Box
    private val keepInSyncCheckBox: JBCheckBox
    private val targetExpressionLabel: JBLabel
    private val outputFileUi: ConsoleOutputFileSettingsUi<BlazeCommandRunConfiguration?>
    private val targetsUi: TargetExpressionListUi

    init {
      val project = config.getProject()
      elementState = config.blazeElementState.clone()
      targetsUi = TargetExpressionListUi(project)
      targetExpressionLabel = JBLabel(UIUtil.ComponentStyle.LARGE)
      keepInSyncCheckBox = JBCheckBox("Keep in sync with source XML")
      outputFileUi = ConsoleOutputFileSettingsUi<BlazeCommandRunConfiguration?>()
      editorWithoutSyncCheckBox = UiUtil.createBox(targetExpressionLabel, targetsUi)
      editor =
        UiUtil.createBox(
          editorWithoutSyncCheckBox, outputFileUi.getComponent(), keepInSyncCheckBox
        )
      updateEditor(config)
      updateHandlerEditor(config)
      keepInSyncCheckBox.addItemListener(ItemListener { e: ItemEvent? -> updateEnabledStatus() })
    }

    private fun updateEditor(config: BlazeCommandRunConfiguration) {
      targetExpressionLabel.setText(
        String.format(
          "Target expression (%s handled by %s):",
          config.targetKindName, config.handler.handlerName
        )
      )
      keepInSyncCheckBox.isVisible = config.keepInSync != null
      if (config.keepInSync != null) {
        keepInSyncCheckBox.setSelected(config.keepInSync!!)
      }
      updateEnabledStatus()
    }

    private fun updateEnabledStatus() {
      setEnabled(!keepInSyncCheckBox.isVisible || !keepInSyncCheckBox.isSelected)
    }

    private fun setEnabled(enabled: Boolean) {
      if (handlerStateEditor != null) {
        handlerStateEditor.setComponentEnabled(enabled)
      }
      targetsUi.setEnabled(enabled)
      outputFileUi.setComponentEnabled(enabled)
    }

    private fun updateHandlerEditor(config: BlazeCommandRunConfiguration) {
      handlerProvider = config.handlerProvider
      handler = handlerProvider.createHandler(config)
      try {
        handler.state.readExternal(config.blazeElementState)
      } catch (e: InvalidDataException) {
        logger.error(e)
      }
      handlerStateEditor = handler.state.getEditor(config.getProject())

      if (handlerStateComponent != null) {
        editorWithoutSyncCheckBox.remove(handlerStateComponent)
      }
      handlerStateComponent = handlerStateEditor.createComponent()
      editorWithoutSyncCheckBox.add(handlerStateComponent)
    }

    override fun createEditor(): JComponent {
      return editor
    }

    override fun resetEditorFrom(config: BlazeCommandRunConfiguration) {
      elementState = config.blazeElementState.clone()
      updateEditor(config)
      if (config.handlerProvider !== handlerProvider) {
        updateHandlerEditor(config)
      }
      targetsUi.setTargetExpressions(config.targetPatterns)
      outputFileUi.resetEditorFrom(config)
      handlerStateEditor.resetEditorFrom(config.handler.state)
    }

    override fun applyEditorTo(config: BlazeCommandRunConfiguration) {
      outputFileUi.applyEditorTo(config)
      handlerStateEditor.applyEditorTo(handler.state)
      try {
        handler.state.writeExternal(elementState)
      } catch (e: WriteExternalException) {
        logger.error(e)
      }
      config.keepInSync = if (keepInSyncCheckBox.isVisible()) keepInSyncCheckBox.isSelected() else null

      // now set the config's state, based on the editor's (possibly out of date) handler
      config.updateHandlerIfDifferentProvider(handlerProvider)
      config.blazeElementState = elementState.clone()
      try {
        config.handler.state.readExternal(config.blazeElementState)
      } catch (e: InvalidDataException) {
        logger.error(e)
      }

      // finally, update the handler
      config.targetPatterns = targetsUi.getTargetExpressions()
      config.updateTargetKindAsync(
        Runnable? { ApplicationManager.getApplication().invokeLater(Runnable { this.fireEditorStateChanged() }) })
      updateEditor(config)
      if (config.handlerProvider !== handlerProvider) {
        updateHandlerEditor(config)
        handlerStateEditor.resetEditorFrom(config.handler.state)
      } else {
        handlerStateEditor.applyEditorTo(config.handler.state)
      }
    }
  }

  companion object {
    private val logger = Logger.getInstance(BlazeCommandRunConfiguration::class.java)

    /**
     * Attributes or tags which are common to all run configuration types. We don't want to interfere
     * with the (de)serialization of these.
     *
     *
     * This is here for backwards compatibility deserializing older-style run configurations
     * without the top-level BLAZE_SETTINGS_TAG element.
     */
    private val COMMON_SETTINGS: ImmutableSet<String?> = ImmutableSet.of<String?>(
      "name",
      "nameIsGenerated",
      "default",
      "temporary",
      "method",
      "type",
      "factoryName",
      "selected",
      "option",
      "folderName",
      "editBeforeRun",
      "activateToolWindowBeforeRun",
      "tempConfiguration"
    )

    /**
     * All blaze-specific settings are serialized under this tag, to distinguish them from common
     * settings.
     */
    private const val BLAZE_SETTINGS_TAG = "blaze-settings"

    private const val HANDLER_ATTR = "handler-id"
    private const val TARGET_TAG = "blaze-target"
    private const val KIND_ATTR = "kind"
    private const val KEEP_IN_SYNC_TAG = "keep-in-sync"
    private const val CONTEXT_ELEMENT_ATTR = "context-element"

    /** Returns an empty list if *any* of the patterns aren't valid target expressions.  */
    private fun parseTargets(strings: List<String>): ImmutableList<TargetPattern> {
      val list: ImmutableList.Builder<TargetPattern> = ImmutableList.builder()
      for (s in strings) {
        val expr: TargetPattern? = parseTarget(s)
        if (expr == null) {
          return ImmutableList.of()
        }
        list.add(expr)
      }
      return list.build()
    }

    private fun parseTarget(targetPattern: String?): TargetPattern? {
      if (Strings.isNullOrEmpty(targetPattern)) {
        return null
      }
      // try to canonicalize labels with implicit target names
      val label: Label? = LabelUtils.createLabelFromString( /* blazePackage= */null, targetPattern)
      return label ?: TargetPattern.fromStringSafe(targetPattern)
    }

    private fun getBlazeSettingsCopy(element: Element): Element? {
      var blazeSettings = element.getChild(BLAZE_SETTINGS_TAG)
      if (blazeSettings != null) {
        return blazeSettings.clone()
      }
      // migrate an old-style run configuration
      blazeSettings = element.clone()
      blazeSettings.setName(BLAZE_SETTINGS_TAG)
      for (common in COMMON_SETTINGS) {
        blazeSettings.removeChildren(common)
        blazeSettings.removeAttribute(common)
      }
      return blazeSettings
    }
  }
}
