package com.ichi2.anki;

import android.Manifest;
import android.content.Context;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ichi2.anki.jsaddons.AddonInfo;
import com.ichi2.anki.jsaddons.NpmPackageDownloader;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import java8.util.concurrent.CompletableFuture;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NpmPackageDownloaderTest {

    private NpmPackageDownloader mNpmPackageDownloader;
    private final String inValidAddonName = "fs";
    private final String validAddonName = "ankidroid-js-addon-progress-bar";
    private AddonInfo mAddonInfo;
    private ObjectMapper mapper;
    private Context context;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);


    @Before
    public void setUp() {
        context = getTargetContext();
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mNpmPackageDownloader = new NpmPackageDownloader(context);
    }

    @Test
    public void invalidAddonTest() throws Exception {
            CompletableFuture<String> result = mNpmPackageDownloader.getTarball(inValidAddonName);
            // for invalid addon package the function does not parse tarball url, it return null
            assertNull(result.get());
    }

    // test valid addon, here ankidroid-js-addon-progress-bar used to test
    @Test
    public void validAddonTest() throws Exception {

        mAddonInfo = mapper.readValue(new URL(context.getString(R.string.npmjs_registry, validAddonName)), AddonInfo.class);
        assertTrue(AddonInfo.isValidAnkiDroidAddon(mAddonInfo));

        CompletableFuture<String> result = mNpmPackageDownloader.getTarball(validAddonName);

        // for invalid addon package the function does not parse tarball url, it return empty string
        assertThat(result.get(), is(mAddonInfo.getDist().get("tarball")));
    }

    @Test
    public void addonDownloadTest() throws Exception {
        mAddonInfo = mapper.readValue(new URL(context.getString(R.string.npmjs_registry, validAddonName)), AddonInfo.class);
        assertTrue(AddonInfo.isValidAnkiDroidAddon(mAddonInfo));

        String tarball = mAddonInfo.getDist().get("tarball");
        String addonName = mAddonInfo.getName();

        CompletableFuture<String> result = mNpmPackageDownloader.downloadAddonPackageFile(tarball, addonName);
        // addon download path : /data/user/0/com.ichi2.anki/cache/addons1600361764659861995.tgz
        // file name in : addons followed by random numbers

        String filePath = result.get();
        assertThat(filePath, is("^(.+)\\/([^\\/]+)$"));

        // test extract and copy of addon .tgz file
        // mNpmPackageDownloader.extractAndCopyAddonTgz(filePath, addonName);
    }

    protected Context getTargetContext() {
        return getInstrumentation().getTargetContext();
    }
}
