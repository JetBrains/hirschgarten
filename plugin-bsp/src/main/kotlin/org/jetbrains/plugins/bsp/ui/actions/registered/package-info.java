/**
 * This package contains actions registered in `plugin.xml` file.
 * <p>
 * According to <a href="https://plugins.jetbrains.com/docs/intellij/basic-action-system.html#action-implementation">Plugin SDK docs</a>
 * <strong>only</strong> such actions <strong>cannot</strong> have class fields.
 * <p>
 * Since registration in `plugin.xml` means that the action is available from `Actions` menu (CMD + Shift + A)
 * so it is reachable from any place in the application.
 * Thus, such actions should be defined here in order to keep them in one place.
 */
package org.jetbrains.plugins.bsp.ui.actions.registered;