/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.intent.ext.sanitize
import mozilla.components.support.utils.EXTRA_ACTIVITY_REFERRER_CATEGORY
import mozilla.components.support.utils.EXTRA_ACTIVITY_REFERRER_PACKAGE
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.WebURLFinder
import mozilla.components.support.utils.ext.getApplicationInfoCompat
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.perf.MarkersActivityLifecycleCallbacks
import org.mozilla.fenix.perf.StartupTimeline

/**
 * Processes incoming intents and sends them to the corresponding activity.
 */
class BookmarkIntentReceiveActivity : Activity() {

    @VisibleForTesting
    override fun onCreate(savedInstanceState: Bundle?) {
        // DO NOT MOVE ANYTHING ABOVE THIS getProfilerTime CALL.
        val startTimeProfiler = components.core.engine.profiler?.getProfilerTime()

        // StrictMode violation on certain devices such as Samsung
        components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            super.onCreate(savedInstanceState)
        }

        // The intent property is nullable, but the rest of the code below
        // assumes it is not. If it's null, then we make a new one and open
        // the HomeActivity.
        val intent = intent?.let { Intent(it) } ?: Intent()
        intent.sanitize().stripUnwantedFlags()
        processIntent(intent)

        components.core.engine.profiler?.addMarker(
            MarkersActivityLifecycleCallbacks.MARKER_NAME,
            startTimeProfiler,
            "BookmarkIntentReceiveActivity.onCreate",
        )
        StartupTimeline.onActivityCreateEndIntentReceiver() // DO NOT MOVE ANYTHING BELOW HERE.
    }

    fun processIntent(intent: Intent) {
        addReferrerInformation(intent)
        val safeIntent = SafeIntent(intent)
        val extraText = safeIntent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val url = WebURLFinder(extraText).bestWebURL()
        CoroutineScope(Dispatchers.Main).launch {
            val isSuccess = url?.let {
                withContext(Dispatchers.IO) {
                    components.useCases.bookmarksUseCases.addBookmark(it, extraText)
                }
            } ?: false
            val text = when (isSuccess) {
                true -> "Bookmark added: $extraText"
                false -> "Bookmark adding failed: $extraText"
            }
            val duration = Toast.LENGTH_SHORT
            val toast = Toast.makeText(applicationContext, text, duration)
            toast.show()
        }
    }

    private fun addReferrerInformation(intent: Intent) {
        // Pass along referrer information when possible.
        // Referrer is supported for API>=22.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return
        }
        // NB: referrer can be spoofed by the calling application. Use with caution.
        val r = referrer ?: return
        intent.putExtra(EXTRA_ACTIVITY_REFERRER_PACKAGE, r.host)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Category is supported for API>=26.
            r.host?.let { host ->
                try {
                    val category = packageManager.getApplicationInfoCompat(host, 0).category
                    intent.putExtra(EXTRA_ACTIVITY_REFERRER_CATEGORY, category)
                } catch (e: PackageManager.NameNotFoundException) {
                    // At least we tried.
                }
            }
        }
    }
}

private fun Intent.stripUnwantedFlags() {
    // Explicitly remove the new task and clear task flags (Our browser activity is a single
    // task activity and we never want to start a second task here).
    flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
    flags = flags and Intent.FLAG_ACTIVITY_CLEAR_TASK.inv()

    // IntentReceiverActivity is started with the "excludeFromRecents" flag (set in manifest). We
    // do not want to propagate this flag from the intent receiver activity to the browser.
    flags = flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS.inv()
}
