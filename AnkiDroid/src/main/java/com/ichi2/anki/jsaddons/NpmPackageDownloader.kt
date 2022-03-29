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
 ****************************************************************************************/

package com.ichi2.anki.jsaddons

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.core.type.TypeReference
import com.ichi2.anki.*
import com.ichi2.async.ProgressSenderAndCancelListener
import com.ichi2.async.TaskDelegate
import com.ichi2.async.TaskListener
import com.ichi2.libanki.Collection
import org.apache.commons.compress.archivers.ArchiveException
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.UnknownHostException

class NpmPackageDownloader {

    /**
     * Get all packages json info
     *
     * @param context
     */
    class GetAddonsPackageJson(var context: Context) : TaskDelegate<Void?, MutableList<AddonModel>?>() {
        override fun task(col: Collection, collectionTask: ProgressSenderAndCancelListener<Void?>): MutableList<AddonModel>? {
            val url = URL(context.getString(R.string.ankidroid_js_addon_json))
            return getJson(url)
        }

        private fun getJson(url: URL): MutableList<AddonModel>? {
            try {

                val mapper = AnkiSerialization.objectMapper
                return mapper.readValue(url, object : TypeReference<MutableList<AddonModel>>() {})
            } catch (e: UnknownHostException) {
                // user not connected to internet
                Timber.w(e.localizedMessage)
            } catch (e: NullPointerException) {
                Timber.w(e.localizedMessage)
            } catch (e: IOException) {
                Timber.w(e.localizedMessage)
            }
            return null
        }
    }

    class GetAddonsPackageJsonListener(
        private val activity: AnkiActivity,
        private val addonsListRecyclerView: RecyclerView
    ) : TaskListener<Void?, MutableList<AddonModel>?>() {
        var context: Context = activity.applicationContext
        override fun onPreExecute() {
            // nothing to do
        }

        override fun onPostExecute(result: MutableList<AddonModel>?) {
            activity.hideProgressBar()

            if (result.isNullOrEmpty()) {
                activity.runOnUiThread {
                    UIUtils.showSimpleSnackbar(activity, context.getString(R.string.error_occur_downloading_addon), false)
                }
                return
            }

            addonsListRecyclerView.adapter = AddonsDownloadAdapter(result)
        }
    }

    /**
     * Extract .tgz file, and copy to addons folder
     *
     * @param context
     * @param tarballPath path to downloaded js-addon.tgz file
     * @param addonName
     */
    class ExtractAddon(
        private val context: Context,
        private val tarballPath: String,
        private val addonName: String,
    ) : TaskDelegate<Void?, String?>() {

        override fun task(col: Collection, collectionTask: ProgressSenderAndCancelListener<Void?>): String {
            return extractAddonPackage(tarballPath, addonName)
        }

        /**
         * Extract npm package .tgz file to folder name 'npmPackageName' in AnkiDroid/addons/
         *
         * @param tarballPath    path to downloaded js-addon.tgz file
         * @param npmPackageName addon name, e.g ankidroid-js-addon-progress-bar
         */
        private fun extractAddonPackage(tarballPath: String, npmPackageName: String): String {
            val currentAnkiDroidDirectory = CollectionHelper.getCurrentAnkiDroidDirectory(context)

            // AnkiDroid/addons/js-addons
            // here npmAddonName is id of npm package which may not contain ../ or other bad path
            val addonsDir = File(currentAnkiDroidDirectory, "addons")
            val addonsPackageDir = File(addonsDir, npmPackageName)
            val tarballFile = File(tarballPath)

            if (!tarballFile.exists()) {
                return context.getString(R.string.failed_to_extract_addon_package, addonName)
            }

            try {

                TgzPackageExtract(context).extractTarGzipToAddonFolder(tarballFile, addonsPackageDir)
                Timber.d("js addon .tgz extracted")
            } catch (e: IOException) {
                Timber.w(e.localizedMessage)
                return context.getString(R.string.failed_to_extract_addon_package, addonName)
            } catch (e: ArchiveException) {
                Timber.w(e.localizedMessage)
                return context.getString(R.string.failed_to_extract_addon_package, addonName)
            } finally {
                tarballFile.delete()
            }
            return context.getString(R.string.addon_install_complete, addonName)
        }
    }

    class ExtractAddonListener(
        private val context: Context,
        private val addonName: String,
        private val view: View
    ) : TaskListener<Void?, String?>() {

        override fun onPreExecute() {
            view.findViewById<TextView>(R.id.download_percentage).text = "0"
            view.findViewById<ProgressBar>(R.id.download_progress).progress = 0
            view.findViewById<TextView>(R.id.downloading_addon_title).text = "Extracting...$addonName"
        }

        override fun onPostExecute(result: String?) {
            if (result.equals(context.getString(R.string.addon_install_complete, addonName))) {
                view.findViewById<Button>(R.id.cancel_addon_download).visibility = View.GONE
                view.findViewById<Button>(R.id.install_addon_btn).visibility = View.GONE
            }
        }
    }
}
