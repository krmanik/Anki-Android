package com.ichi2.anki.jsaddons;

public interface DownloadAddonAsyncTaskListener {
    void listAddonsFromDir(String addonType);
    void addonShowProgressBar();
    void addonHideProgressBar();
}
