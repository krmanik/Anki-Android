package com.ichi2.anki.jsaddons;

import android.content.Context;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.anki.R;

import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;

import java8.util.StringJoiner;
import java8.util.concurrent.CompletableFuture;
import timber.log.Timber;

import static com.ichi2.anki.web.HttpFetcher.downloadFileToSdCardMethod;

public class NpmPackageDownloader {
    private Context mContext;
    private DownloadAddonAsyncTaskListener mTaskListener;

    public NpmPackageDownloader(Context context) {
        this.mContext = context;
        mTaskListener = (DownloadAddonAsyncTaskListener) context;
    }

    public CompletableFuture<String> getTarball(String addonName) {
        return CompletableFuture.supplyAsync(() -> {
            mTaskListener.addonShowProgressBar();
            try {
                ObjectMapper mapper = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                AddonInfo mAddonInfo = mapper.readValue(new URL(mContext.getString(R.string.npmjs_registry, addonName)), AddonInfo.class);

                if (AddonInfo.isValidAnkiDroidAddon(mAddonInfo)) {
                    return mAddonInfo.getDist().get("tarball");
                }
            } catch (NullPointerException | IOException e) {
                mTaskListener.addonHideProgressBar();
                mTaskListener.showToast(mContext.getString(R.string.error_occur_downloadin_addon));
                e.printStackTrace();
            }

            mTaskListener.addonHideProgressBar();
            return null;
        });
    }

    /**
     * @param tarballUrl   tarball url of addon.tgz package file
     */
    public CompletableFuture<String> downloadAddonPackageFile(String tarballUrl, String addonName) {
        return CompletableFuture.supplyAsync(() -> {
            if (tarballUrl.isEmpty()) {
                mTaskListener.addonHideProgressBar();
                mTaskListener.showToast(mContext.getString(R.string.invalid_js_addon));
                return null;
            }

            String downloadFilePath = downloadFileToSdCardMethod(tarballUrl, mContext, "addons", "GET");
            Timber.d("download path %s", downloadFilePath);

            extractAndCopyAddonTgz(downloadFilePath, addonName);
            return downloadFilePath;
        });
    }

    /**
     * @param tarballPath  path to downloaded js-addon.tgz file
     * @param npmAddonName addon name, e.g ankidroid-js-addon-progress-bar
     *                     extract downloaded .tgz files and copy to AnkiDroid/addons/ folder
     */
    public void extractAndCopyAddonTgz(String tarballPath, String npmAddonName) {

        String currentAnkiDroidDirectory = CollectionHelper.getCurrentAnkiDroidDirectory(mContext);

        // AnkiDroid/addons/js-addons
        // here npmAddonName is id of npm package which may not contain ../ or other bad path
        StringJoiner joinedPath = new StringJoiner("/")
                .add(currentAnkiDroidDirectory)
                .add("addons")
                .add(npmAddonName);

        File addonsDir = new File(joinedPath.toString());
        File tarballFile = new File(tarballPath);

        if (!tarballFile.exists()) {
            return;
        }

        // extracting using library https://github.com/thrau/jarchivelib
        try {
            Archiver archiver = ArchiverFactory.createArchiver(tarballFile);
            archiver.extract(tarballFile, addonsDir);
            Timber.d("js addon .tgz extracted");
            mTaskListener.showToast(mContext.getString(R.string.addon_installed));
            mTaskListener.addonHideProgressBar();
        } catch (IOException e) {
            Timber.e(e.getLocalizedMessage());
        } finally {
            if (tarballFile.exists()) {
                tarballFile.delete();
            }
        }
    }
}
