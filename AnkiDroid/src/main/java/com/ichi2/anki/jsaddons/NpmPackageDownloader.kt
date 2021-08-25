/****************************************************************************************
 * Copyright (c) 2021 Mani <infinyte01></infinyte01>@gmail.com>                                       *
 * *
 * *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 * *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 * *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.                           *
 */
package com.ichi2.anki.jsaddons

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CollectionHelper
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import com.ichi2.anki.web.HttpFetcher
import com.ichi2.async.ProgressSenderAndCancelListener
import com.ichi2.async.TaskDelegate
import com.ichi2.async.TaskListener
import com.ichi2.async.TaskManager
import com.ichi2.libanki.Collection
import org.apache.commons.compress.archivers.ArchiveException
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.UnknownHostException

class NpmPackageDownloader {
    /**
     * Show/hide download button in webview
     * for valid addon npm package - show button
     * for invalid addon npm package - hide button
     */
    class ShowHideInstallButton(private val mContext: Context, private val mNpmPackageName: String) : TaskDelegate<Void?, String?>() {

        override fun task(col: Collection, collectionTask: ProgressSenderAndCancelListener<Void?>): String? {
            val url = URL(mContext.getString(R.string.npmjs_registry, mNpmPackageName))
            return getTarBallUrl(url)
        }

        /**
         * Using Jackson the latest package.json for the addon fetched, then mapped to AddonModel
         * For valid package it gets tarball url from mapped AddonModel,
         * then downloads and extract to AnkiDroid/addons folder using {@code extractAndCopyAddonTgz} and toast with success message returned
         *
         * For invalid addon or for exception occurred, it returns message to respective to the errors from catch block
         *
         * @param url npmjs.org package registry url http://registry.npmjs.org/ankidroid-js-addon-.../latest
         * @return tarballUrl if valid addon else message explaining errors
         */
        fun getTarBallUrl(url: URL): String? {
            try {
                // mapping for json fetched from http://registry.npmjs.org/ankidroid-js-addon-.../latest
                val mapper = ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                val addonModel = mapper.readValue(url, AddonModel::class.java)

                // check if fields like ankidroidJsApi, addonType exists or not
                if (!addonModel.isValidAnkiDroidAddon()) {
                    return mContext.getString(R.string.is_not_valid_js_addon, mNpmPackageName)
                }

                // get tarball url to download it cache folder
                val tarballUrl = addonModel.dist!!["tarball"]
                return tarballUrl

                // addonTitle sent to list the addons in recycler view
            } catch (e: JacksonException) {
                // json format is not valid as required by AnkiDroid JS Addon specifications
                // also ObjectMapper failed to parse the fields for e.g. requested fields in AddonModel is String but
                // package.json contains array, so it may leads to parse exception or mapping exception
                Timber.w(e.localizedMessage)
                return mContext.getString(R.string.is_not_valid_js_addon, mNpmPackageName)
            } catch (e: UnknownHostException) {
                // user not connected to internet
                Timber.w(e.localizedMessage)
                return mContext.getString(R.string.network_no_connection)
            } catch (e: NullPointerException) {
                Timber.w(e.localizedMessage)
                return mContext.getString(R.string.error_occur_downloading_addon, mNpmPackageName)
            } catch (e: IOException) {
                Timber.w(e.localizedMessage)
                return mContext.getString(R.string.error_occur_downloading_addon, mNpmPackageName)
            }
        }
    }

    /**
     * Show/hide download button in webview listener
     *
     * in onPostExecute after page loaded result is tarballUrl, show hidden button and set OnClickListener for calling another Task which download and extract .tgz file
     */
    class ShowHideInstallButtonListener(private val mActivity: Activity, private val mDownloadButton: Button, private val addonName: String) : TaskListener<Void?, String?>() {
        var mContext: Context = mActivity.applicationContext
        override fun onPreExecute() {
            // nothing to do
        }

        override fun onPostExecute(result: String?) {
            mDownloadButton.setOnClickListener {
                val dialog = Dialog(mActivity)
                dialog.setCancelable(false)
                dialog.setContentView(R.layout.addon_progress_bar_layout)
                val title = dialog.findViewById(R.id.progress_bar_layout_title) as TextView
                val msg = dialog.findViewById(R.id.progress_bar_layout_message) as TextView
                val progress = dialog.findViewById(R.id.progress_bar) as ProgressBar
                val cancelButton = dialog.findViewById(R.id.cancel_action) as Button
                title.text = mContext.getString(R.string.downloading_npm_package)
                msg.text = addonName

                // call another task which download .tgz file and extract and copy to addons folder
                // here result is tarBallUrl
                val cancellable = TaskManager.launchCollectionTask(
                    DownloadAndExtract(mContext, result!!, addonName),
                    DownloadAndExtractListener(mActivity, title, msg, progress, cancelButton)
                )

                cancelButton.setOnClickListener {
                    cancellable.cancel(true)
                    dialog.dismiss()
                }

                dialog.show()
            }

            // result will .tgz url for valid npm package else message explaining errors
            if (result != null) {
                // show download button at bottom right with "Install Addon" when string starts with url
                // the result return from previous collection task
                if (result.startsWith("https://")) {
                    mDownloadButton.visibility = View.VISIBLE
                } else {
                    // show snackbar where to seek help and wiki for the errors
                    val helpUrl = Uri.parse(mContext.getString(R.string.link_help))
                    val activity = mActivity as AnkiActivity?
                    UIUtils.showSnackbar(
                        mActivity,
                        result,
                        false,
                        R.string.help,
                        { v -> activity?.openUrl(helpUrl) },
                        null,
                        null
                    )
                }
            }
        }
    }

    /**
     * Download .tgz file, extract and copy to addons folder
     */
    class DownloadAndExtract(private val mContext: Context, private val tarballUrl: String, private val addonName: String) : TaskDelegate<Void?, String?>() {

        override fun task(col: Collection, collectionTask: ProgressSenderAndCancelListener<Void?>): String? {
            return downloadPackage()
        }

        /**
         * Download .tgz file from provided url
         *
         * @param tarballUrl .tgz file url
         * @param addonName name of the npm addon package
         * @return success message for toast or snackbar
         */
        fun downloadPackage(): String {
            // download the .tgz file in cache folder of AnkiDroid
            val downloadFilePath = HttpFetcher.downloadFileToSdCardMethod(tarballUrl, mContext, "addons", "GET")
            Timber.d("download path %s", downloadFilePath)

            // extract the .tgz file to AnkiDroid/addons dir
            val extracted = extractAndCopyAddonTgz(downloadFilePath, addonName)

            if (!extracted) {
                return mContext.getString(R.string.failed_to_extract_addon_package, addonName)
            } else {
                return mContext.getString(R.string.addon_install_complete, addonName)
            }
        }

        /**
         * Extract npm package .tgz file to folder name 'npmPackageName' in AnkiDroid/addons/
         *
         * @param tarballPath    path to downloaded js-addon.tgz file
         * @param npmPackageName addon name, e.g ankidroid-js-addon-progress-bar
         */
        fun extractAndCopyAddonTgz(tarballPath: String, npmPackageName: String): Boolean {
            if (tarballPath == null) {
                return false
            }

            val currentAnkiDroidDirectory = CollectionHelper.getCurrentAnkiDroidDirectory(mContext)

            // AnkiDroid/addons/js-addons
            // here npmAddonName is id of npm package which may not contain ../ or other bad path
            val addonsDir = File(currentAnkiDroidDirectory, "addons")
            val addonsPackageDir = File(addonsDir, npmPackageName)
            val tarballFile = File(tarballPath)
            if (!tarballFile.exists()) {
                return false
            }

            try {
                NpmPackageTgzExtract.extractTarGzipToAddonFolder(tarballFile, addonsPackageDir)
                Timber.d("js addon .tgz extracted")
            } catch (e: IOException) {
                Timber.w(e.localizedMessage)
                return false
            } catch (e: ArchiveException) {
                Timber.w(e.localizedMessage)
                return false
            } finally {
                tarballFile.delete()
            }
            return true
        }
    }

    class DownloadAndExtractListener(
        private val mActivity: Activity,
        private var mTitle: TextView,
        private var msg: TextView,
        private val progress: ProgressBar,
        private val cancelButton: Button
    ) : TaskListener<Void?, String?>() {
        var mContext: Context = mActivity.applicationContext
        override fun onPreExecute() {
            mTitle.text = mContext.getString(R.string.extracting_npm_package)
        }

        override fun onPostExecute(result: String?) {
            mTitle.text = mContext.getString(R.string.addon_installed)
            msg.text = result
            progress.visibility = View.GONE
            cancelButton.setText(R.string.dialog_ok)
        }
    }
}
