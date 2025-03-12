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
package org.jetbrains.bazel.ogRun

import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.intellij.execution.BeforeRunTask
import org.jetbrains.bazel.ogRun.confighandler.BlazeCommandRunConfigurationHandlerProvider
import java.awt.event.ActionListener
import java.awt.event.ItemListener
import kotlin.concurrent.Volatile
import com.intellij.execution.RunManagerEx.getInstanceEx
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.LocatableRunConfigurationOptions;
import com.intellij.execution.configurations.ModuleRunProfile;
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.jetbrains.bazel.label.Label
import org.jdom.Element;
import org.jetbrains.bazel.ogRun.confighandler.BlazeCommandRunConfigurationHandler
import org.jetbrains.bazel.ogRun.confighandler.BlazeCommandRunConfigurationRunner;
import org.jetbrains.bazel.ogRun.state.ConsoleOutputFileSettingsUi;
import org.jetbrains.bazel.ogRun.state.RunConfigurationState;
import org.jetbrains.bazel.ogRun.state.RunConfigurationStateEditor;
import org.jetbrains.bazel.ogRun.targetfinder.FuturesUtil;
import org.jetbrains.bazel.ogRun.targetfinder.TargetFinder;
import org.jetbrains.bazel.ogRun.ui.TargetExpressionListUi;
import java.lang.String.*

/** A run configuration which executes Blaze commands.  */
class BlazeCommandRunConfiguration
    (
    project: Project,
    factory: ConfigurationFactory,
    name: String?
) : LocatableConfigurationBase<LocatableRunConfigurationOptions?>(
    project,
    factory,
    name
), BlazeRunConfiguration, ModuleRunProfile,
    RunConfigurationWithSuppressedDefaultDebugAction {
    /** The blaze-specific parts of the last serialized state of the configuration.  */
    private var blazeElementState: Element = Element(BLAZE_SETTINGS_TAG)

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
        val pendingContext: PendingRunConfigurationContext? = this.pendingContext
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
    private var targetPatterns: ImmutableList<String> =
        ImmutableList.of<String?>()

    // null if the target is null or not a single Label
    @Volatile
    private var targetKindString: String? = null

    // used to recognize previously created pending targets by their corresponding source element
    @Volatile
    var contextElementString: String? = null
        private set

    // for keeping imported configurations in sync with their source XML
    private var keepInSync: Boolean? = null

    private var handlerProvider: BlazeCommandRunConfigurationHandlerProvider?
    private var handler: BlazeCommandRunConfigurationHandler

    init {
        // start with whatever fallback is present for unknown state. The user may need to fix it.
        handlerProvider =
            BlazeCommandRunConfigurationHandlerProvider.findHandlerProvider(
                BlazeCommandRunConfigurationHandlerProvider.TargetState.PENDING,
                null
            )
        handler = handlerProvider.createHandler(this)
        try {
            handler.state.readExternal(blazeElementState)
        } catch (e: com.intellij.openapi.util.InvalidDataException) {
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
    fun <T : RunConfigurationState?> getHandlerStateIfType(type: Class<T?>): T? {
        val handlerState: RunConfigurationState? = handler.state
        if (type.isInstance(handlerState)) {
            return type.cast(handlerState)
        } else {
            return null
        }
    }

    override val targets: List<Label>
        get() = parseTargets(targetPatterns)

    val singleTarget: Label?
        /**
         * Returns the single target expression represented by this configuration, or null if there isn't
         * exactly one.
         */
        get() = targets.singleOrNull()

    fun setTargetInfo(target: TargetInfo) {
        val pattern = target.label.toString().trim()
        targetPatterns =
            if (pattern.isEmpty()) ImmutableList.of<String?>() else ImmutableList.of<String?>(
                pattern
            )
        updateTargetKind(target.kindString)
    }

    fun setTargets(targets: ImmutableList<TargetExpression?>) {
        targetPatterns = targets.stream().map<Any?>(TargetExpression::toString)
            .collect(ImmutableList.toImmutableList<Any?>())
        updateTargetKindAsync(null) {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(Runnable {
                if (handlersLoadedForKind != config.targetKindString) {
                    handlersLoadedForKind = config.targetKindString
                    updateHandlerListAndAutoSelect(config, true)
                }
                if (editorHandlerProvider !== config.handlerProvider) {
                    updateHandlerEditor(config, config.handlerProvider)
                }
                fireEditorStateChanged()
            })
        }
    }

    /** Sets the target expression and asynchronously kicks off a target kind update.  */
    fun setTarget(target: TargetExpression?) {
        targetPatterns =
            if (target != null) ImmutableList.of<E?>(
                target.toString().trim()
            ) else ImmutableList.of<String?>()
        updateTargetKindAsync(null) {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(Runnable {
                if (handlersLoadedForKind != config.targetKindString) {
                    handlersLoadedForKind = config.targetKindString
                    updateHandlerListAndAutoSelect(config, true)
                }
                if (editorHandlerProvider !== config.handlerProvider) {
                    updateHandlerEditor(config, config.handlerProvider)
                }
                fireEditorStateChanged()
            })
        }
    }

    private fun updateHandler() {
        val handlerProvider: BlazeCommandRunConfigurationHandlerProvider =
            BlazeCommandRunConfigurationHandlerProvider.findHandlerProvider(
                this.targetState, this.targetKind
            )
        updateHandlerIfDifferentProvider(handlerProvider)
    }

    private val targetState: BlazeCommandRunConfigurationHandlerProvider.TargetState
        get() = if ((targetPatterns.isEmpty() && pendingContext != null) ||
            (this.targetKind == null && (handlerProvider == null || handlerProvider.canHandleKind(
                BlazeCommandRunConfigurationHandlerProvider.TargetState.PENDING,
                null
            )))
        )
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
        } catch (e: com.intellij.openapi.util.WriteExternalException) {
            logger.error(e)
        }
        handlerProvider = newProvider
        handler = newProvider.createHandler(this)
        try {
            handler.state.readExternal(blazeElementState)
        } catch (e: com.intellij.openapi.util.InvalidDataException) {
            logger.error(e)
        }
    }

    val targetKind: Kind?
        /**
         * Returns the [Kind] of the single blaze target corresponding to the configuration's target
         * expression, if it's currently known. Returns null if the target expression points to multiple
         * blaze targets.
         */
        get() = Kind.fromRuleName(targetKindString)

    /**
     * Queries the kind of the current target pattern, possibly asynchronously, in the case where
     * there's only a single target.
     *
     * @param asyncCallback if the kind is updated asynchronously, this will be run after the kind is
     * updated. If it's updated synchronously, this will not be run.
     */
    fun updateTargetKindAsync(asyncCallback: Unit, function: () -> Unit) {
        val targets: ImmutableList<TargetExpression?> = parseTargets(targetPatterns)
        if (targets.size != 1 || targets.get(0) !is Label) {
            // TODO(brendandouglas): any reason to support multiple targets here?
            updateTargetKind(null)
            return
        }
        val label: Label? = targets.get(0) as Label?
        val future: com.google.common.util.concurrent.ListenableFuture<TargetInfo?> =
            TargetFinder.findTargetInfoFuture(project, label)
        if (future.isDone) {
            updateTargetKindFromTargetInfoFuture(future, label, null)
        } else {
            updateTargetKindFromSingleTarget(null)
            future.addListener(
                Runnable {
                    updateTargetKindFromTargetInfoFuture(future, label, asyncCallback)
                },
                com.google.common.util.concurrent.MoreExecutors.directExecutor()
            )
        }
    }

    private fun updateTargetKindFromTargetInfoFuture(
        future: com.google.common.util.concurrent.ListenableFuture<TargetInfo?>,
        label: Label?,
        asyncCallback: Runnable?
    ) {
        val targetInfo: TargetInfo? = FuturesUtil.getIgnoringErrors<TargetInfo?>(future)
        if (targetInfo == null) {
            val throwable = getFutureFailure(future)
            if (throwable != null) {
                logger.warn(
                    String.format(
                        "Failed to retrieve target info for run config %s target %s. Error: %s",
                        this,
                        label,
                        throwable
                    ),
                    throwable
                )
            }
        } else {
            if (this.targetKind != targetInfo.getKind()) {
                logger.info(
                    format(
                        "Run configuration %s target %s kind updated to %s",
                        this,
                        targetInfo.getLabel(),
                        targetInfo.getKind()
                    )
                )
            }
        }
        if (updateTargetKindFromSingleTarget(targetInfo)) {
            asyncCallback?.run()
        }
    }

    private fun updateTargetKindFromSingleTarget(target: TargetInfo?): Boolean {
        return updateTargetKind(target?.kindString)
    }

    private fun updateTargetKind(kind: String?): Boolean {
        val targetStateWas: BlazeCommandRunConfigurationHandlerProvider.TargetState = this.targetState
        if (targetKindString == kind) {
            return false
        }
        targetKindString = kind
        if (targetStateWas == BlazeCommandRunConfigurationHandlerProvider.TargetState.PENDING) {
            // Let users choose if already determined.
            updateHandler()
        }
        return true
    }

    private val targetKindName: String?
        /**
         * @return The [Kind] name, if the target is a known rule. Otherwise, "target pattern" if it
         * is a general [TargetExpression], "unknown rule" if it is a [Label] without a
         * known rule, and "unknown target" if there is no target.
         */
        get() {
            val kind: Kind? = this.targetKind
            if (kind != null) {
                return kind.toString()
            }

            val targets: ImmutableList<TargetExpression?> =
                parseTargets(targetPatterns)
            if (targets.size > 1) {
                return "target patterns"
            }
            val singleTarget: TargetExpression? =
                com.google.common.collect.Iterables.getFirst<TargetExpression?>(targets, null)
            if (singleTarget is Label) {
                return "unknown rule"
            } else if (singleTarget != null) {
                return "target pattern"
            } else {
                return "unknown target"
            }
        }

    @Throws(com.intellij.execution.configurations.RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        // Our handler check is not valid when we don't have BlazeProjectData.
        if (BlazeProjectDataManager.getInstance(project).getBlazeProjectData() == null) {
            // With query sync we don't need a sync to run a configuration
            if (Blaze.getProjectType(project) !== ProjectType.QUERY_SYNC) {
                throw RuntimeConfigurationError(
                    "Configuration cannot be run until project has been synced."
                )
            }
        }
        val hasBlazeBeforeRunTask: Boolean =
            getInstanceEx(project).getBeforeRunTasks(this).stream()
                .anyMatch { task: BeforeRunTask<*> -> task.getProviderId() == BlazeBeforeRunTaskProvider.ID && task.isEnabled() }
        if (!hasBlazeBeforeRunTask) {
            throw RuntimeConfigurationError(
                format(
                    "Invalid run configuration: the %s before run task is missing. Please re-run sync "
                            + "to add it back",
                    Blaze.buildSystemName(project)
                )
            )
        }
        handler.checkConfiguration()
        val pendingContext: PendingRunConfigurationContext? = this.pendingContext
        if (pendingContext != null && !pendingContext.isDone) {
            return
        }
        val targetPatterns: ImmutableList<String> = this.targetPatterns
        if (targetPatterns.isEmpty()) {
            if (handler.commandName !== BlazeCommandName.INFO) {
                throw RuntimeConfigurationError(
                    format(
                        "You must specify a %s target expression.", Blaze.buildSystemName(project)
                    )
                )
            }
        }
        for (pattern in targetPatterns) {
            if (handler.commandName !== BlazeCommandName.INFO) {
                if (Strings.isNullOrEmpty(pattern)) {
                    throw RuntimeConfigurationError(
                        format(
                            "You must specify a %s target expression.", Blaze.buildSystemName(project)
                        )
                    )
                }
            }
            if (!pattern.startsWith("//") && !pattern.startsWith("@") && !pattern.startsWith("-//") && !pattern.startsWith(
                    "-@"
                )
            ) {
                throw RuntimeConfigurationError(
                    "You must specify the full target expression, starting with '//' or '@' or '-' for a negative one"
                )
            }

            val error: String? = TargetExpression.validate(pattern)
            if (error != null) {
                throw RuntimeConfigurationError(error)
            }
        }
        if (handlerProvider.canHandleKind(BlazeCommandRunConfigurationHandlerProvider.TargetState.PENDING, null)) {
            throw RuntimeConfigurationError("A Bazel handler must be selected")
        }
    }

    @Throws(com.intellij.openapi.util.InvalidDataException::class)
    override fun readExternal(element: Element) {
        var element: Element = element
        super.readExternal(element)

        element = getBlazeSettingsCopy(element)

        val keepInSyncString: String? = element.getAttributeValue(KEEP_IN_SYNC_TAG)
        keepInSync = if (keepInSyncString != null) keepInSyncString.toBoolean() else null
        contextElementString = element.getAttributeValue(CONTEXT_ELEMENT_ATTR)

        val targets: ImmutableList.Builder<String?> =
            ImmutableList.builder<String?>()
        val targetElements: MutableList<Element?> = element.getChildren(TARGET_TAG)
        for (targetElement in targetElements) {
            if (targetElement != null && !Strings.isNullOrEmpty(targetElement.textTrim)) {
                targets.add(targetElement.textTrim)
                // backwards-compatibility with prior per-target kind serialization
                val kind: String? = targetElement.getAttributeValue(KIND_ATTR)
                if (kind != null) {
                    targetKindString = kind
                }
            }
        }
        targetPatterns = targets.build()
        val singleKind: String? = element.getAttributeValue(KIND_ATTR)
        if (singleKind != null) {
            targetKindString = element.getAttributeValue(KIND_ATTR)
        }

        // Because BlazeProjectData is not available when configurations are loading,
        // we can't call setTarget and have it find the appropriate handler provider.
        // So instead, we use the stored provider ID.
        val providerId: String? = element.getAttributeValue(HANDLER_ATTR)
        val handlerProvider: BlazeCommandRunConfigurationHandlerProvider? =
            BlazeCommandRunConfigurationHandlerProvider.getHandlerProvider(providerId)
        if (handlerProvider != null) {
            updateHandlerIfDifferentProvider(handlerProvider)
        }

        blazeElementState = element
        handler.state.readExternal(blazeElementState)
    }

    @Throws(com.intellij.openapi.util.WriteExternalException::class)
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
        } catch (e: com.intellij.openapi.util.InvalidDataException) {
            logger.error(e)
        }

        return configuration
    }

    @Throws(com.intellij.execution.ExecutionException::class)
    override fun getState(
        executor: com.intellij.execution.Executor?,
        environment: com.intellij.execution.runners.ExecutionEnvironment
    ): com.intellij.execution.configurations.RunProfileState? {
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

    val configurationEditor: SettingsEditor<out BlazeCommandRunConfiguration?>
        get() = BlazeCommandRunConfigurationSettingsEditor(this)

    val modules: Array<Module>
        get() = arrayOfNulls<Module>(0)

    internal class BlazeCommandRunConfigurationSettingsEditor
        (config: BlazeCommandRunConfiguration) :
        SettingsEditor<BlazeCommandRunConfiguration?>() {
        private var handlersLoadedForKind: String? = null
        private var editorHandlerProvider: BlazeCommandRunConfigurationHandlerProvider? = null
        private var editorHandler: BlazeCommandRunConfigurationHandler? = null
        private var handlerStateEditor: RunConfigurationStateEditor? = null
        private var handlerStateComponent: JComponent? = null
        private var elementState: Element

        private val editorWithoutSyncCheckBox: Box
        private val editor: Box
        private val keepInSyncCheckBox: JBCheckBox
        private val targetExpressionLabel: JBLabel
        private val outputFileUi: ConsoleOutputFileSettingsUi<BlazeCommandRunConfiguration?>
        private val targetsUi: TargetExpressionListUi
        private val handlerLabel: JLabel
        private val handlerCombo: ComboBox<ProviderItem?>

        class ProviderItem(provider: BlazeCommandRunConfigurationHandlerProvider?) {
            override fun toString(): String {
                return provider.displayLabel
            }

            val provider: BlazeCommandRunConfigurationHandlerProvider?

            init {
                this.provider = provider
            }
        }

        init {
            val project: Project = config.project
            elementState = config.blazeElementState.clone()
            targetsUi = TargetExpressionListUi(project)
            targetExpressionLabel = JBLabel(UIUtil.ComponentStyle.LARGE)
            keepInSyncCheckBox = JBCheckBox("Keep in sync with source XML")
            outputFileUi = ConsoleOutputFileSettingsUi<BlazeCommandRunConfiguration?>()
            handlerLabel = JLabel("Bazel handler:")
            handlerCombo =
                ComboBox<ProviderItem?>(
                    DefaultComboBoxModel<ProviderItem?>(
                        BlazeCommandRunConfigurationHandlerProvider.findHandlerProviders().stream()
                            .map<ProviderItem?> { provider: BlazeCommandRunConfigurationHandlerProvider? ->
                                ProviderItem(provider)
                            }.toArray<ProviderItem?> { _Dummy_.__Array__() })
                )
            handlerCombo.setEditable(false)

            editorWithoutSyncCheckBox = UiUtil.createBox(targetExpressionLabel, targetsUi, handlerLabel, handlerCombo)
            editor =
                UiUtil.createBox(
                    editorWithoutSyncCheckBox, outputFileUi.getComponent(), keepInSyncCheckBox
                )
            updateEditor(config)
            updateHandlerEditor(config, config.handlerProvider)
            keepInSyncCheckBox.addItemListener(ItemListener { e: ItemEvent? -> updateEnabledStatus() })
            handlerCombo.addActionListener(ActionListener { e: ActionEvent? ->
                if (handlerCombo.selectedItem is ProviderItem) {
                    updateHandlerProviderToConfig(
                        config,
                        BlazeCommandRunConfigurationHandlerProvider.getHandlerProvider(providerItem.provider.getId())
                    )
                }
            })
        }

        private fun updateEditor(config: BlazeCommandRunConfiguration) {
            targetExpressionLabel.setText(
                String.format(
                    "Target expression (%s handled by %s):",
                    config.targetKindName, config.handler.handlerName
                )
            )
            keepInSyncCheckBox.isVisible = true
            if (config.keepInSync != null) {
                keepInSyncCheckBox.setSelected(config.keepInSync)
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
            handlerCombo.setEnabled(enabled)
        }

        private fun updateHandlerEditor(
            config: BlazeCommandRunConfiguration,
            newProvider: BlazeCommandRunConfigurationHandlerProvider
        ) {
            editorHandlerProvider = newProvider
            editorHandler = editorHandlerProvider.createHandler(config)
            try {
                editorHandler.state.readExternal(config.blazeElementState)
            } catch (e: com.intellij.openapi.util.InvalidDataException) {
                logger.error(e)
            }
            handlerStateEditor = editorHandler.state.getEditor(config.project)

            if (handlerStateComponent != null) {
                editorWithoutSyncCheckBox.remove(handlerStateComponent)
            }
            handlerStateComponent = handlerStateEditor.createComponent()
            editorWithoutSyncCheckBox.add(handlerStateComponent)
            if (handlerCombo.selectedItem != ProviderItem(newProvider)) {
                handlerCombo.setSelectedItem(ProviderItem(newProvider))
            }
        }

        override fun createEditor(): JComponent {
            return editor
        }

        override fun resetEditorFrom(config: BlazeCommandRunConfiguration) {
            elementState = config.blazeElementState.clone()
            handlerCombo.setSelectedItem(ProviderItem(config.handlerProvider))
            updateEditor(config)
            if (config.handlerProvider !== editorHandlerProvider) {
                updateHandlerEditor(config, config.handlerProvider)
            }
            targetsUi.setTargetExpressions(config.targetPatterns)
            outputFileUi.resetEditorFrom(config)
            handlerStateEditor.resetEditorFrom(config.handler.state)
        }

        override fun applyEditorTo(config: BlazeCommandRunConfiguration) {
            outputFileUi.applyEditorTo(config)
            handlerStateEditor.applyEditorTo(editorHandler.state)
            try {
                editorHandler.state.writeExternal(elementState)
            } catch (e: com.intellij.openapi.util.WriteExternalException) {
                logger.error(e)
            }
            config.keepInSync = if (keepInSyncCheckBox.isVisible) keepInSyncCheckBox.isSelected else null

            // now set the config's state, based on the editor's (possibly out of date) handler
            config.updateHandlerIfDifferentProvider(editorHandlerProvider)
            config.blazeElementState = elementState.clone()
            try {
                config.handler.state.readExternal(config.blazeElementState)
            } catch (e: com.intellij.openapi.util.InvalidDataException) {
                logger.error(e)
            }

            // finally, update the handler
            config.targetPatterns = targetsUi.getTargetExpressions()
            config.updateTargetKindAsync(
                Runnable? {
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(Runnable {
                    if (handlersLoadedForKind != config.targetKindString) {
                        handlersLoadedForKind = config.targetKindString
                        updateHandlerListAndAutoSelect(config, true)
                    }
                    if (editorHandlerProvider !== config.handlerProvider) {
                        updateHandlerEditor(config, config.handlerProvider)
                    }
                    fireEditorStateChanged()
                })
            })
            updateHandlerListAndAutoSelect(config, false)
            if (editorHandlerProvider !== config.handlerProvider) {
                updateHandlerEditor(config, config.handlerProvider)
            }
            updateEditor(config)
            if (config.handlerProvider !== editorHandlerProvider) {
                updateHandlerEditor(config, config.handlerProvider)
                handlerStateEditor.resetEditorFrom(config.handler.state)
            } else {
                handlerStateEditor.applyEditorTo(config.handler.state)
            }
        }

        private fun updateHandlerListAndAutoSelect(config: BlazeCommandRunConfiguration, autoSelect: Boolean) {
            val handlers: ImmutableList<ProviderItem?> =
                (if (handlerSelectionLaxMode.getValue() || config.targetKind == null)
                    BlazeCommandRunConfigurationHandlerProvider.findHandlerProviders()
                else
                    BlazeCommandRunConfigurationHandlerProvider.findHandlerProviders(
                        config.targetState,
                        config.targetKind
                    ))
                    .stream().map<ProviderItem?> { provider: BlazeCommandRunConfigurationHandlerProvider? ->
                        ProviderItem(provider)
                    }
                    .collect(ImmutableList.toImmutableList<ProviderItem?>())
            val currentHandlers: ImmutableList.Builder<ProviderItem?> =
                ImmutableList.builder<ProviderItem?>()
            for (i in 0..<handlerCombo.getModel().size) {
                currentHandlers.add(handlerCombo.getModel().getElementAt(i))
            }
            var selected: Any? = handlerCombo.selectedItem
            if (currentHandlers.build() != handlers) {
                handlerCombo.setModel(DefaultComboBoxModel<ProviderItem?>(handlers.toArray<ProviderItem?>(IntFunction { _Dummy_.__Array__() })))
                if (autoSelect && handlerSelectionAutoFixMode.getValue()) {
                    val newBestSelection: BlazeCommandRunConfigurationHandlerProvider? =
                        com.google.common.collect.Iterables.getFirst<BlazeCommandRunConfigurationHandlerProvider?>(
                            BlazeCommandRunConfigurationHandlerProvider.findHandlerProviders(
                                config.targetState,
                                config.targetKind
                            ), null
                        )
                    if (newBestSelection != null && selected !== newBestSelection) {
                        // Note, we only auto-update the configuration in the UI. At the runtime the configuration updates its handler only if it is in
                        // the pending state, i.e. the handler is unknown or intentionally set to pending.
                        selected = newBestSelection
                        logger.info(
                            String.format(
                                "Auto-updating %s run configuration handler to %s",
                                config,
                                newBestSelection
                            )
                        )
                    }
                }
            }
            if (!handlers.contains(selected)) {
                selected = com.google.common.collect.Iterables.getFirst<ProviderItem?>(handlers, null)
            }
            if (handlerCombo.selectedItem != selected) {
                handlerCombo.setSelectedItem(selected)
            }
        }

        private fun updateHandlerProviderToConfig(
            config: BlazeCommandRunConfiguration,
            provider: BlazeCommandRunConfigurationHandlerProvider
        ) {
            try {
                try {
                    handlerStateEditor.applyEditorTo(editorHandler.state)
                    editorHandler.state.writeExternal(elementState)
                } catch (t: Throwable) {
                    logger.error("Attempt to preserve state crashed", t)
                }
                updateHandlerEditor(config, provider)
                try {
                    editorHandler.state.readExternal(elementState)
                } catch (t: Throwable) {
                    logger.error("Attempt to preserve state crashed", t)
                }
                handlerStateEditor.resetEditorFrom(editorHandler.state)
            } catch (t: Throwable) {
                logger.error("Cannot configure handler provider", t)
            }
        }
    }

    companion object {
        private val handlerSelectionLaxMode: BoolExperiment =
            BoolExperiment("aswb.run.config.handler.lax.editing", false)

        private val handlerSelectionAutoFixMode: BoolExperiment =
            BoolExperiment("aswb.run.config.handler.auto.fix", true)

        private val logger: com.intellij.openapi.diagnostic.Logger =
            com.intellij.openapi.diagnostic.Logger.getInstance(BlazeCommandRunConfiguration::class.java)

        /**
         * Attributes or tags which are common to all run configuration types. We don't want to interfere
         * with the (de)serialization of these.
         *
         *
         * This is here for backwards compatibility deserializing older-style run configurations
         * without the top-level BLAZE_SETTINGS_TAG element.
         */
        private val COMMON_SETTINGS: com.google.common.collect.ImmutableSet<String?> =
            com.google.common.collect.ImmutableSet.of<String?>(
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
        private fun parseTargets(strings: MutableList<String>): ImmutableList<TargetExpression?> {
            val list: ImmutableList.Builder<TargetExpression?> =
                ImmutableList.builder<TargetExpression?>()
            for (s in strings) {
                val expr: TargetExpression? = parseTarget(s)
                if (expr == null) {
                    return ImmutableList.of<TargetExpression?>()
                }
                list.add(expr)
            }
            return list.build()
        }

        private fun parseTarget(targetPattern: String?): TargetExpression? {
            if (Strings.isNullOrEmpty(targetPattern)) {
                return null
            }
            // try to canonicalize labels with implicit target names
            val label: Label? = LabelUtils.createLabelFromString( /* blazePackage= */null, targetPattern)
            return if (label != null) label else TargetExpression.fromStringSafe(targetPattern)
        }

        private fun getFutureFailure(future: com.google.common.util.concurrent.ListenableFuture<TargetInfo?>): Throwable? {
            var throwable: Throwable? = null
            try {
                future.get()
            } catch (t: Throwable) {
                throwable = t
            }
            return throwable
        }

        private fun getBlazeSettingsCopy(element: Element): Element? {
            var blazeSettings: Element? = element.getChild(BLAZE_SETTINGS_TAG)
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
