package com.ichi2.anki.jsaddons;

import android.app.Activity;

public abstract class DownloadAddonBackgroundTask {

    private Activity activity;
    public DownloadAddonBackgroundTask(Activity activity) {
        this.activity = activity;
    }

    private void startBackground() {
        new Thread(() -> {
            doInBackground();
            activity.runOnUiThread(() -> onPostExecute());
        }).start();
    }

    public void execute(){
        startBackground();
    }

    public abstract void doInBackground();
    public abstract void onPostExecute();
}
