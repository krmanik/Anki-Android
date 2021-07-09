package com.ichi2.anki;

import android.content.Context;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ichi2.anki.jsaddons.AddonInfo;
import com.ichi2.anki.jsaddons.NpmPackageDownloader;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java8.util.concurrent.CompletableFuture;

import static android.os.Looper.getMainLooper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

@RunWith(AndroidJUnit4.class)
public class NpmPackageDownloaderTest {

    private NpmPackageDownloader mNpmPackageDownloader;
    private final String inValidAddonName = "fs";
    private final String validAddonName = "ankidroid-js-addon-progress-bar";
    private final String validTarballNpmUrl = "https://registry.npmjs.org/ankidroid-js-addon-progress-bar/-/ankidroid-js-addon-progress-bar-1.0.6.tgz";
    private AddonInfo mAddonInfo;
    private ObjectMapper mapper;
    private Context context;
    private AddonBrowser mAddonBrowser;


    public void init() {
        mAddonBrowser = new AddonBrowser();
        context = mAddonBrowser.getContext();
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mNpmPackageDownloader = new NpmPackageDownloader(context);
    }

    @Test
    public void validAddonTest() throws Exception {
        init();
        shadowOf(getMainLooper()).idle();

        CompletableFuture<String> result1 = mNpmPackageDownloader.getTarball(validAddonName);
        assertEquals(result1.get(), is(validTarballNpmUrl));
    }

    @Test
    public void addonDownloadTest() throws Exception {
        init();
        shadowOf(getMainLooper()).idle();

        CompletableFuture<String> result = mNpmPackageDownloader.downloadAddonPackageFile(validTarballNpmUrl, validAddonName);
        String filePath = result.get();
        assertThat(filePath, is("^(.+)\\/([^\\/]+)$"));
    }
}
