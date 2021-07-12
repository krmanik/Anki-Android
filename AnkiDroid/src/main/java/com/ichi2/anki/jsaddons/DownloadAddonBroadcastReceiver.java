package com.ichi2.anki.jsaddons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.ichi2.anki.R;
import com.ichi2.anki.UIUtils;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskManager;

import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class DownloadAddonBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        String url = uri.toString();
        String addonName = getAddonName(url);
        if (addonName.isEmpty()) {
            UIUtils.showThemedToast(context, context.getString(R.string.invalid_js_addon), false);
            return;
        }

        UIUtils.showThemedToast(context, context.getString(R.string.checking_valid_addon), false);
        Timber.d("Addon Url::" + uri.toString());
        CollectionTask<Void, String> ct = TaskManager.launchCollectionTask(new NpmPackageDownloader.DownloadAddon(context, addonName));

        try {
            if (context.getString(R.string.addon_installed).equals(ct.get())) {
                UIUtils.showThemedToast(context, context.getString(R.string.addon_installed), false);
            } else {
                UIUtils.showThemedToast(context, context.getString(R.string.invalid_js_addon), false);
            }
        } catch (ExecutionException e) {
            UIUtils.showThemedToast(context, context.getString(R.string.error_occur_downloading_addon), false);
            Timber.w(e.getLocalizedMessage());
        } catch (InterruptedException e) {
            UIUtils.showThemedToast(context, context.getString(R.string.error_occur_downloading_addon), false);
            Timber.w(e.getLocalizedMessage());
        }
    }


    private String getAddonName(String url) {
        String addonName = "";

        // if url is npm package url
        if (!url.startsWith("https://www.npmjs.com/package/")) {
            return "";
        }
        // get addon name removing package url
        addonName = url.replaceFirst("https://www.npmjs.com/package/", "");

        // may be addon name ends with slash
        if (addonName.contains("/")) {
            String[] s = addonName.split("/");
            addonName = s[0];
        }

        return addonName;
    }
}
