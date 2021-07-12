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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ichi2.anki.jsaddons.AddonInfo;
import com.ichi2.anki.jsaddons.AddonsAdapter;
import com.ichi2.anki.jsaddons.DownloadAddonListener;
import com.ichi2.anki.jsaddons.NpmPackageDownloader;
import com.ichi2.anki.widgets.DeckDropDownAdapter;
import com.ichi2.async.TaskManager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

public class AddonBrowser extends NavigationDrawerActivity implements DeckDropDownAdapter.SubtitleListener {
    private String mNpmAddonName;
    private RecyclerView mAddonsListRecyclerView;


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
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.js_addons));
        showBackIcon();

        mAddonsListRecyclerView = findViewById(R.id.addons);
        mAddonsListRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        listAddonsFromDir();
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


    /*
    list addons with valid package.json, i.e contains
    ankidroid_js_api = 0.0.1
    keywords='ankidroid-js-addon'
    and non empty string.
    Then that addon will available for enable/disable
    */
    public void listAddonsFromDir() {
        String currentAnkiDroidDirectory = CollectionHelper.getCurrentAnkiDroidDirectory(this);
        File addonsHomeDir = new File(currentAnkiDroidDirectory, "addons");
        List<AddonInfo> addonsList = new ArrayList<>();

        boolean success = true;
        if (!addonsHomeDir.exists()) {
            success = addonsHomeDir.mkdirs();
        }

        if (success) {
            try {
                File[] files = addonsHomeDir.listFiles();

                for (File file : files) {
                    Timber.d("Addons: %s", file.getName());

                    // Read package.json from
                    // AnkiDroid/addons/ankidroid-addon-../package/package.json
                    ObjectMapper mapper = new ObjectMapper()
                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                    AddonInfo mAddonInfo = mapper.readValue(new File(file, "package/package.json"), AddonInfo.class);

                    if (AddonInfo.isValidAnkiDroidAddon(mAddonInfo)) {
                        addonsList.add(mAddonInfo);
                    }
                }
                mAddonsListRecyclerView.setAdapter(new AddonsAdapter(addonsList));

            } catch (JsonParseException | JsonMappingException | MalformedURLException e) {
                Timber.w(e.getLocalizedMessage());
                UIUtils.showThemedToast(this, getString(R.string.invalid_js_addon), false);
            } catch (NullPointerException | IOException e) {
                Timber.w(e.getLocalizedMessage());
                UIUtils.showThemedToast(this, getString(R.string.invalid_js_addon), false);
            }
        }

        hideProgressBar();
    }


    @Override
    protected void onResume() {
        super.onResume();
        listAddonsFromDir();
    }
}