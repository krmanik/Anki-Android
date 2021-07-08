package com.ichi2.anki.jsaddons;

import android.content.Context;
import android.os.AsyncTask;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ichi2.anki.R;
import com.ichi2.anki.UIUtils;

import java.io.IOException;
import java.net.URL;

public class DownloadAddonAsyncTask extends AsyncTask<String, Void, Void> {

    private NpmPackageDownloader mNpmPackageDownloader;
    private String tarball;
    private String addonName;
    private String addonType;
    private AddonInfo mAddonInfo;
    private DownloadAddonAsyncTaskListener mTaskListener;
    private boolean isValidAddon = false;

    private final Context mContext;


    public DownloadAddonAsyncTask(final Context context) {
        mContext = context;
        mTaskListener = (DownloadAddonAsyncTaskListener) context;
    }


    @Override
    public void onPreExecute() {
        mTaskListener.addonShowProgressBar();
    }


    @Override
    protected Void doInBackground(String... strings) {
        try {
            addonName = strings[0];
            mNpmPackageDownloader = new NpmPackageDownloader(mContext);

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            mAddonInfo = mapper.readValue(new URL(mContext.getString(R.string.npmjs_registry, addonName)), AddonInfo.class);

            tarball = mAddonInfo.getDist().get("tarball");
            addonType = mAddonInfo.getAddonType();

            if (AddonInfo.isValidAnkiDroidAddon(mAddonInfo)) {
                mNpmPackageDownloader.downloadAddonPackageFile(tarball, addonName);
                isValidAddon = true;
            } else {
                isValidAddon = false;
            }

        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        mTaskListener.listAddonsFromDir(addonType);
        mTaskListener.addonHideProgressBar();

        if (isValidAddon) {
            UIUtils.showThemedToast(mContext, mContext.getString(R.string.addon_installed), true);
        } else {
            UIUtils.showThemedToast(mContext, mContext.getString(R.string.invalid_js_addon), true);
        }

        super.onPostExecute(aVoid);
    }
}
