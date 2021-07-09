package com.ichi2.anki;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.ichi2.anki.jsaddons.DownloadAddonAsyncTaskListener;
import com.ichi2.anki.jsaddons.NpmPackageDownloader;
import com.ichi2.anki.widgets.DeckDropDownAdapter;

import java8.util.concurrent.CompletableFuture;
import timber.log.Timber;

public class AddonBrowser extends NavigationDrawerActivity implements DeckDropDownAdapter.SubtitleListener, DownloadAddonAsyncTaskListener {
    private String npmAddonName;
    private NpmPackageDownloader mNpmPackageDownloader;

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

        mNpmPackageDownloader = new NpmPackageDownloader(this);
    }


    @Override
    public String getSubtitleText() {
        return getResources().getString(R.string.js_addons);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem installAddon, getMoreAddons, reviewerAddons;

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
                npmAddonName = downloadEditText.getText().toString();

                // if string is:  npm i ankidroid-js-addon-progress-bar
                if (npmAddonName.startsWith("npm i")) {
                    npmAddonName = npmAddonName.substring("npm i".length());
                }

                // if containing space
                npmAddonName = npmAddonName.trim();
                npmAddonName = npmAddonName.replaceAll("\u00A0", "");
                if (npmAddonName.isEmpty()) {
                    return;
                }

                mNpmPackageDownloader.getTarball(npmAddonName)
                        .thenApply(url -> mNpmPackageDownloader.downloadAddonPackageFile(url, npmAddonName));

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

    @Override
    public void listAddonsFromDir(String addonType) {
        Timber.d("List addon from directory.");
    }

    @Override
    public void addonShowProgressBar() {
        runOnUiThread(() -> showProgressBar());
    }


    @Override
    public void addonHideProgressBar() {
        runOnUiThread(() -> hideProgressBar());
    }


    @Override
    public void showToast(String msg) {
        runOnUiThread(() -> UIUtils.showThemedToast(getApplicationContext(), msg, false));
    }
}
