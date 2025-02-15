/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import mozilla.components.browser.state.state.TabSessionState
import org.mozilla.fenix.tabstray.TrayPagerAdapter

/**
 * Interactor for all things related to inactive tabs in the tabs tray.
 */
interface InactiveTabsInteractor {
    /**
     * Invoked when the header is clicked.
     *
     * @param activated true when the tap should expand the inactive section.
     */
    fun onHeaderClicked(activated: Boolean)

    /**
     * Invoked when an inactive tab is clicked.
     *
     * @param tab [TabSessionState] that was clicked.
     */
    fun onTabClicked(tab: TabSessionState)

    /**
     * Invoked when an inactive tab is closed.
     *
     * @param tab [TabSessionState] that was closed.
     */
    fun onTabClosed(tab: TabSessionState)

    /**
     * Invoked when the user clicks on the delete all inactive tabs button.
     */
    fun onDeleteAllInactiveTabsClicked()

    /**
     * Invoked when the user clicks the close button in the auto close dialog.
     */
    fun onAutoCloseDialogCloseButtonClicked()

    /**
     * Enable the auto-close feature with the "after a month" setting.
     */
    fun onEnableAutoCloseClicked()
}

/**
 * Interactor to be called for any user interactions with the Inactive Tabs feature.
 *
 * @param controller An instance of [InactiveTabsController] which will be delegated for all
 * user interactions.
 * @param browserInteractor [BrowserTrayInteractor] used to respond to interactions with specific
 * inactive tabs.
 */
class DefaultInactiveTabsInteractor(
    private val controller: InactiveTabsController,
    private val browserInteractor: BrowserTrayInteractor,
) : InactiveTabsInteractor {

    /**
     * See [InactiveTabsInteractor.onHeaderClicked].
     */
    override fun onHeaderClicked(activated: Boolean) {
        controller.updateCardExpansion(activated)
    }

    /**
     * See [InactiveTabsInteractor.onAutoCloseDialogCloseButtonClicked].
     */
    override fun onAutoCloseDialogCloseButtonClicked() {
        controller.dismissAutoCloseDialog()
    }

    /**
     * See [InactiveTabsInteractor.onEnableAutoCloseClicked].
     */
    override fun onEnableAutoCloseClicked() {
        controller.enableInactiveTabsAutoClose()
    }

    /**
     * See [InactiveTabsInteractor.onTabClicked].
     */
    override fun onTabClicked(tab: TabSessionState) {
        controller.openInactiveTab(tab)
        browserInteractor.onTabSelected(tab, TrayPagerAdapter.INACTIVE_TABS_FEATURE_NAME)
    }

    /**
     * See [InactiveTabsInteractor.onTabClosed].
     */
    override fun onTabClosed(tab: TabSessionState) {
        controller.closeInactiveTab(tab)
        browserInteractor.onTabClosed(tab, TrayPagerAdapter.INACTIVE_TABS_FEATURE_NAME)
    }

    /**
     * See [InactiveTabsInteractor.onDeleteAllInactiveTabsClicked].
     */
    override fun onDeleteAllInactiveTabsClicked() {
        controller.deleteAllInactiveTabs()
    }
}
