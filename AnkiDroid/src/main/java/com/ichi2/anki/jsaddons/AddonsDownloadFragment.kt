/****************************************************************************************
 * Copyright (c) 2022 Mani <infinyte01@gmail.com>                                       *
 *                                                                                      *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ***************************************************************************************/

package com.ichi2.anki.jsaddons

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import com.ichi2.async.TaskManager
import timber.log.Timber
import java.io.File
import kotlin.math.abs

class AddonsDownloadFragment(private var addonModel: AddonModel) : DialogFragment() {

    private var mDownloadId: Long = 0

    private var mNpmPackageName: String? = null
    private var mAddonTitle: String? = null

    private var mHandler: Handler = Handler(Looper.getMainLooper())
    private var mIsProgressCheckerRunning = false

    private lateinit var mCancelButton: Button
    private lateinit var mTryAgainButton: Button
    private lateinit var mInstallAddonButton: Button
    private lateinit var mDownloadAddonTitle: TextView
    private lateinit var mDownloadPercentageText: TextView
    private lateinit var mDownloadProgressBar: ProgressBar
    private lateinit var mCheckNetworkInfoText: TextView

    /**
     * Android's DownloadManager - Used here to manage the functionality of downloading addons from tarball url,
     * one at a time. Responsible for enqueuing a download and generating the corresponding download ID,
     * removing a download from the queue and providing cursor using a query related to the download ID.
     * Since only one download is supported at a time, the DownloadManager's queue is expected to
     * have a single request at a time.
     */
    private lateinit var mDownloadManager: DownloadManager

    var isDownloadInProgress = false

    private var mDownloadCancelConfirmationDialog: MaterialDialog? = null

    companion object {
        const val DOWNLOAD_PROGRESS_CHECK_DELAY = 100L

        const val STARTED_PROGRESS_PERCENTAGE = "0"
        const val COMPLETED_PROGRESS_PERCENTAGE = "100"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_addons_download, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mDownloadPercentageText = view.findViewById(R.id.download_percentage)
        mDownloadProgressBar = view.findViewById(R.id.download_progress)
        mCancelButton = view.findViewById(R.id.cancel_addon_download)
        mInstallAddonButton = view.findViewById(R.id.install_addon_btn)
        mTryAgainButton = view.findViewById(R.id.try_again_addon_download)
        mCheckNetworkInfoText = view.findViewById(R.id.check_network_info_text)
        mDownloadAddonTitle = view.findViewById(R.id.downloading_addon_title)

        mDownloadManager = (activity as AddonsBrowser).downloadManager

        downloadFile(addonModel)

        mCancelButton.setOnClickListener {
            Timber.i("Cancel download button clicked which would lead to showing of confirmation dialog")
            showCancelConfirmationDialog()
        }

        mInstallAddonButton.setOnClickListener {
            Timber.i("Install addon button clicked")
            extractDownloadedAddon()
        }

        mTryAgainButton.setOnClickListener {
            Timber.i("Try again button clicked, retry downloading of addon")
            mDownloadManager.remove(mDownloadId)
            downloadFile(addonModel)
            mCancelButton.visibility = View.VISIBLE
            mTryAgainButton.visibility = View.GONE
        }
    }

    /**
     * Register broadcast receiver for listening to download completion.
     * Set the request for downloading the addon, enqueue it in DownloadManager, store download ID and
     * file name, mark download to be in progress, set the title of the download screen and start
     * the download progress checker.
     */
    private fun downloadFile(addonModel: AddonModel) {
        // Register broadcast receiver for download completion.
        Timber.d("Registering broadcast receiver for download completion")
        activity?.registerReceiver(mOnComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        // download the tar file with .tgz extension
        val downloadRequest = generateAddonDownloadRequest(addonModel.dist?.get("tarball")!!, addonModel.name!! + ".tgz")

        // Store unique download ID to be used when onReceive() of BroadcastReceiver gets executed.
        mDownloadId = mDownloadManager.enqueue(downloadRequest)
        // the name is npm package name as id and addonTitle displayed as heading for package name
        mNpmPackageName = addonModel.name
        mAddonTitle = addonModel.addonTitle
        isDownloadInProgress = true
        Timber.d("Download ID -> $mDownloadId")
        Timber.d("File name -> $mNpmPackageName")
        mDownloadAddonTitle.text = getString(R.string.downloading_file, addonModel.addonTitle)
        startDownloadProgressChecker()
    }

    private fun generateAddonDownloadRequest(fileToBeDownloaded: String, currentFileName: String): DownloadManager.Request {
        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(fileToBeDownloaded))

        request.setTitle(currentFileName)

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, currentFileName)

        return request
    }

    /**
     * Registered in downloadFile() method.
     * When onReceive() is called, open the addon file to install it.
     */
    private var mOnComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.i("Download might be complete now, verify and continue with import")

            fun verifyAddonIsInstallable() {
                if (mNpmPackageName == null) {
                    // Send ACRA report
                    AnkiDroidApp.sendExceptionReport(
                        "File name is null",
                        "AddonsDownloadFragment::verifyAddonIsInstallable"
                    )
                    return
                }

                // Return if mDownloadId does not match with the ID of the completed download.
                if (mDownloadId != intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)) {
                    Timber.w(
                        "Download ID did not match with the ID of the completed download. " +
                            "Download completion related to some other download might have been received. " +
                            "Addon package download might still be going on, when it completes then the method would be called again."
                    )
                    // Send ACRA report
                    AnkiDroidApp.sendExceptionReport(
                        "Download ID does not match with the ID of the completed download",
                        "AddonsDownloadFragment::verifyAddonIsInstallable"
                    )
                    return
                }

                stopDownloadProgressChecker()

                // Halt execution if file is not valid addon
                if (!addonModel.isValidAnkiDroidAddon()) {
                    Timber.i("File is not a valid javascript addon package, abort the extract task")
                    checkDownloadStatusAndUnregisterReceiver(isSuccessful = false, isInvalidAddonFile = true)
                    return
                }

                val query = DownloadManager.Query()
                query.setFilterById(mDownloadId)
                val cursor = mDownloadManager.query(query)

                cursor.use {
                    // Return if cursor is empty.
                    if (!it.moveToFirst()) {
                        Timber.i("Empty cursor, cannot continue further with success check and addon install")
                        checkDownloadStatusAndUnregisterReceiver(isSuccessful = false)
                        return
                    }

                    val columnIndex: Int = it.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    // Return if download was not successful.
                    if (it.getInt(columnIndex) != DownloadManager.STATUS_SUCCESSFUL) {
                        Timber.i("Download could not be successful, update UI and unregister receiver")
                        Timber.d("Status code -> ${it.getInt(columnIndex)}")
                        checkDownloadStatusAndUnregisterReceiver(isSuccessful = false)
                        return
                    }
                }
            }

            try {
                verifyAddonIsInstallable()
            } catch (exception: Exception) {
                Timber.w(exception)
                checkDownloadStatusAndUnregisterReceiver(isSuccessful = false)
                return
            }

            if (isVisible) {
                // Setting these since progress checker can stop before progress is updated to represent 100%
                mDownloadPercentageText.text = getString(R.string.percentage, COMPLETED_PROGRESS_PERCENTAGE)
                mDownloadProgressBar.progress = COMPLETED_PROGRESS_PERCENTAGE.toInt()

                // Remove cancel button and show install addon button
                // mCancelButton.visibility = View.GONE
                // mInstallAddonButton.visibility = View.VISIBLE
            }

            Timber.i("Extracted downloaded addon")
            extractDownloadedAddon()

            Timber.d("Checking download status and unregistering receiver")
            checkDownloadStatusAndUnregisterReceiver(isSuccessful = true)
        }
    }

    /**
     * Unregister the mOnComplete broadcast receiver.
     */
    private fun unregisterReceiver() {
        Timber.d("Unregistering receiver")
        try {
            activity?.unregisterReceiver(mOnComplete)
        } catch (exception: IllegalArgumentException) {
            // This might throw an exception in cases where the receiver is already in unregistered state.
            // Log the exception in such cases, there is nothing else to do.
            Timber.w(exception)
            return
        }
    }

    /**
     * Check download progress and update status at intervals of 0.1 second.
     */
    private val mDownloadProgressChecker: Runnable by lazy {
        object : Runnable {
            override fun run() {
                checkDownloadProgress()

                // Keep checking download progress at intervals of 0.1 second.
                mHandler.postDelayed(this, DOWNLOAD_PROGRESS_CHECK_DELAY)
            }
        }
    }

    /**
     * Start checking for download progress.
     */
    private fun startDownloadProgressChecker() {
        Timber.d("Starting download progress checker")
        mDownloadProgressChecker.run()
        mIsProgressCheckerRunning = true
        mDownloadPercentageText.text = getString(R.string.percentage, STARTED_PROGRESS_PERCENTAGE)
        mDownloadProgressBar.progress = STARTED_PROGRESS_PERCENTAGE.toInt()
    }

    /**
     * Stop checking for download progress.
     */
    private fun stopDownloadProgressChecker() {
        Timber.d("Stopping download progress checker")
        mHandler.removeCallbacks(mDownloadProgressChecker)
        mIsProgressCheckerRunning = false
    }

    /**
     * Checks download progress and sets the current progress in ProgressBar.
     */
    private fun checkDownloadProgress() {
        val query = DownloadManager.Query()
        query.setFilterById(mDownloadId)

        val cursor = mDownloadManager.query(query)

        cursor.use {
            // Return if cursor is empty.
            if (!it.moveToFirst()) {
                return
            }

            // Calculate download progress and display it in the ProgressBar.
            val downloadedBytes = it.getLong(it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val totalBytes = it.getInt(it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            // Taking absolute value to prevent case of -0.0 % being shown.
            val downloadProgress: Float = abs(downloadedBytes * 1f / totalBytes * 100)
            val downloadProgressIntValue = downloadProgress.toInt()
            val percentageValue = if (downloadProgressIntValue == 0 || downloadProgressIntValue == 100) {
                // Show 0 % and 100 % instead of 0.0 % and 100.0 %
                downloadProgressIntValue.toString()
            } else {
                // Show download progress percentage up to 1 decimal place.
                "%.1f".format(downloadProgress)
            }
            mDownloadPercentageText.text = getString(R.string.percentage, percentageValue)
            mDownloadProgressBar.progress = downloadProgress.toInt()

            val columnIndexForStatus = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val columnIndexForReason = it.getColumnIndex(DownloadManager.COLUMN_REASON)

            if (columnIndexForStatus == -1) {
                Timber.w("Column for status does not exist")
                return
            }

            if (columnIndexForReason == -1) {
                Timber.w("Column for reason does not exist")
                return
            }

            // Display message if download is waiting for network connection
            if (it.getInt(columnIndexForStatus) == DownloadManager.STATUS_PAUSED &&
                it.getInt(columnIndexForReason) == DownloadManager.PAUSED_WAITING_FOR_NETWORK
            ) {
                mCheckNetworkInfoText.visibility = View.VISIBLE
            } else {
                mCheckNetworkInfoText.visibility = View.GONE
            }
        }
    }

    /**
     * Extract the downloaded addon package using 'mFileName'.
     */
    private fun extractDownloadedAddon() {
        val tarballFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), mNpmPackageName.toString()).path
        Timber.d("Tarball File -> $tarballFile")

        // TODO use cancellable
        TaskManager.launchCollectionTask(
            NpmPackageDownloader.ExtractAddon(requireContext(), tarballFile, addonModel.name!!),
            NpmPackageDownloader.ExtractAddonListener(requireContext(), addonModel.name!!, requireView())
        )
    }

    /**
     * Handle download error scenarios.
     *
     * If there are any pending downloads, continue with them.
     * Else, set mIsPreviousDownloadOngoing as false and unregister mOnComplete broadcast receiver.
     */
    private fun checkDownloadStatusAndUnregisterReceiver(isSuccessful: Boolean, isInvalidAddonFile: Boolean = false) {
        if (isVisible && !isSuccessful) {
            if (isInvalidAddonFile) {
                Timber.i("File is not a valid addon, hence return from the download screen")
                UIUtils.showThemedToast(activity, R.string.import_log_no_apkg, false)
                // Go back if file is not a addon and cannot be installed
                activity?.onBackPressed()
            } else {
                Timber.i("Download failed, update UI and provide option to retry")
                UIUtils.showThemedToast(activity, R.string.something_wrong, false)
                // Update UI if download could not be successful
                mTryAgainButton.visibility = View.VISIBLE
                mCancelButton.visibility = View.GONE
                mDownloadPercentageText.text = getString(R.string.download_failed)
                mDownloadProgressBar.progress = STARTED_PROGRESS_PERCENTAGE.toInt()
            }
        }

        unregisterReceiver()
        isDownloadInProgress = false

        // If the cancel confirmation dialog is being shown and the download is no longer in progress, then remove the dialog.
        removeCancelConfirmationDialog()
    }

    private fun showCancelConfirmationDialog() {
        mDownloadCancelConfirmationDialog = context?.let {
            MaterialDialog.Builder(it)
                .title(R.string.cancel_download_question_title)
                .positiveText(R.string.dialog_cancel)
                .negativeText(R.string.dialog_continue)
                .onPositive { _, _ ->
                    mDownloadManager.remove(mDownloadId)
                    unregisterReceiver()
                    isDownloadInProgress = false
                    activity?.onBackPressed()
                }
                .onNegative { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun removeCancelConfirmationDialog() {
        mDownloadCancelConfirmationDialog?.dismiss()
    }
}
