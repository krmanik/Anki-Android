package com.ichi2.anki;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ichi2.anki.jsaddons.AddonInfo;
import com.ichi2.anki.jsaddons.NpmPackageDownloader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class NpmPackageDownloaderTest extends RobolectricTest {

    private NpmPackageDownloader mNpmPackageDownloader;
    private final String inValidAddonName = "fs";
    private final String validAddonName = "ankidroid-js-addon-progress-bar";
    private final String validTarballNpmUrl = "https://registry.npmjs.org/ankidroid-js-addon-progress-bar/-/ankidroid-js-addon-progress-bar-1.0.6.tgz";
    private AddonInfo mAddonInfo;
    private ObjectMapper mapper;
    private Context context;


    @Mock
    private SharedPreferences mMockSharedPreferences;


    @Before
    public void init() {
        AddonBrowser mAddonBrowser = super.startActivityNormallyOpenCollectionWithIntent(AddonBrowser.class, new Intent());
        context = mAddonBrowser.getApplicationContext();
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void validAddonTest() throws Exception {
        advanceRobolectricLooperWithSleep();
        new NpmPackageDownloader((Activity) context, context, validAddonName).execute();
    }


    @Test
    public void addonDownloadTest() throws Exception {

    }
}
