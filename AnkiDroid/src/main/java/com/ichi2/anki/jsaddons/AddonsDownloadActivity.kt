/****************************************************************************************
 *                                                                                      *
 * Copyright (c) 2021 Mani infinyte01@gmail.com                                         *
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

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.NavigationDrawerActivity
import com.ichi2.anki.R
import com.ichi2.anki.UIUtils
import com.ichi2.async.TaskManager
import timber.log.Timber
import java.io.IOException

/**
 * Download packages json for all addons
 */
class AddonsDownloadActivity : NavigationDrawerActivity() {
    private lateinit var mAddonsListRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return
        }

        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
        setContentView(R.layout.addons_browser)
        initNavigationDrawer(findViewById(android.R.id.content))

        supportActionBar?.title = getString(R.string.download_addons)
        showBackIcon()
        showProgressBar()

        mAddonsListRecyclerView = findViewById(R.id.addons)
        mAddonsListRecyclerView.layoutManager = LinearLayoutManager(this)

        fetchAddonsPackageJson()
    }

    private fun fetchAddonsPackageJson() {
        try {
            TaskManager.launchCollectionTask(
                NpmPackageDownloader.GetAddonsPackageJson(this),
                NpmPackageDownloader.GetAddonsPackageJsonListener(this, mAddonsListRecyclerView)
            )
        } catch (e: IOException) {
            Timber.w(e.localizedMessage)
            UIUtils.showThemedToast(this, getString(R.string.is_not_valid_js_addon), false)
        }
    }
}
