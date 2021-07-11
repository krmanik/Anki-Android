/****************************************************************************************
 * Copyright (c) 2021 Mani <infinyte01@gmail.com>                                       *
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

package com.ichi2.anki;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.ichi2.anki.jsaddons.DownloadAddonListener;
import com.ichi2.anki.jsaddons.NpmPackageDownloader;
import com.ichi2.anki.widgets.DeckDropDownAdapter;
import com.ichi2.async.TaskManager;

import timber.log.Timber;

public class AddonBrowser extends NavigationDrawerActivity implements DeckDropDownAdapter.SubtitleListener {
    private String mNpmAddonName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return;
        }

        super.onCreate(savedInstanceState);
        Timber.d("onCreate()");
        setContentView(R.layout.addons_browser);
        initNavigationDrawer(findViewById(android.R.id.content));

        // Add a home button to the actionbar
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.js_addons));
        showBackIcon();

        hideProgressBar();
    }


    @Override
    public String getSubtitleText() {
        return getResources().getString(R.string.js_addons);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem installAddon, getMoreAddons;

        getMenuInflater().inflate(R.menu.addon_browser, menu);
        installAddon = menu.findItem(R.id.action_install_addon);
        getMoreAddons = menu.findItem(R.id.action_get_more_addons);

        Dialog mDownloadDialog = new Dialog(this);

        installAddon.setOnMenuItemClickListener(item -> {

            mDownloadDialog.setCanceledOnTouchOutside(true);
            mDownloadDialog.setContentView(R.layout.addon_install_from_npm);

            EditText downloadEditText = mDownloadDialog.findViewById(R.id.addon_download_edit_text);
            Button downloadButton = mDownloadDialog.findViewById(R.id.addon_download_button);

            downloadButton.setOnClickListener(v -> {
                mNpmAddonName = downloadEditText.getText().toString();

                // if string is:  npm i ankidroid-js-addon-progress-bar
                if (mNpmAddonName.startsWith("npm i")) {
                    mNpmAddonName = mNpmAddonName.substring("npm i".length());
                }

                // if containing space
                mNpmAddonName = mNpmAddonName.trim();
                mNpmAddonName = mNpmAddonName.replaceAll("\u00A0", "");
                if (mNpmAddonName.isEmpty()) {
                    return;
                }

                // get tarball and download npm package then extract and copy to addons folder
                TaskManager.launchCollectionTask(new NpmPackageDownloader.DownloadAddon(this, mNpmAddonName), new DownloadAddonListener(this));

                mDownloadDialog.dismiss();
            });

            mDownloadDialog.show();
            return true;
        });

        getMoreAddons.setOnMenuItemClickListener(item -> {
            openUrl(Uri.parse(getResources().getString(R.string.ankidroid_js_addon_npm_search_url)));
            return true;
        });

        return super.onCreateOptionsMenu(menu);
    }

    public void listAddonsFromDir() {
        Timber.d("List addon from directory.");
    }
}