package com.ichi2.anki.jsaddons;

import android.content.Context;

import com.ichi2.anki.CollectionHelper;

import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import java.io.File;
import java.io.IOException;

import java8.util.StringJoiner;
import timber.log.Timber;

import static com.ichi2.anki.web.HttpFetcher.downloadFileToSdCardMethod;

public class NpmPackageDownloader {
    private Context mContext;

    public NpmPackageDownloader(Context context) {
        this.mContext = context;
    }

    /**
     * @param npmAddonName addon name, e.g ankidroid-js-addon-progress-bar
     * @param tarballUrl   tarball url of addon.tgz package file
     */
    public void downloadAddonPackageFile(String tarballUrl, String npmAddonName) {
        String downloadFilePath = downloadFileToSdCardMethod(tarballUrl, mContext, "addons", "GET");
        Timber.d("download path %s", downloadFilePath);
        extractAndCopyAddonTgz(downloadFilePath, npmAddonName);
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
        } catch (IOException e) {
            Timber.e(e.getLocalizedMessage());
        } finally {
            if (tarballFile.exists()) {
                tarballFile.delete();
            }
        }
    }
}
