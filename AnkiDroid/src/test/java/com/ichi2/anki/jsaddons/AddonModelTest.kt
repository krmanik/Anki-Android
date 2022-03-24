/****************************************************************************************
 * Copyright (c) 2021 Mani infinyte01@gmail.com                                         *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki.jsaddons

import android.content.SharedPreferences
import android.os.Looper.getMainLooper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.AnkiSerialization
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.jsaddons.AddonsConst.REVIEWER_ADDON
import junit.framework.TestCase.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.io.IOException
import java.net.URL

@RunWith(AndroidJUnit4::class)
class AddonModelTest : RobolectricTest() {
    private val VALID_NPM_PACKAGE_NAME_URL = "https://registry.npmjs.org/valid-ankidroid-js-addon-test/latest"
    private val NOT_VALID_NPM_PACKAGE_NAME_URL = "https://registry.npmjs.org/not-valid-ankidroid-js-addon-test/latest"

    private lateinit var mPrefs: SharedPreferences

    @Before
    fun before() {
        mPrefs = AnkiDroidApp.getSharedPrefs(targetContext)
    }

    @Test
    @Throws(IOException::class)
    fun isValidAnkiDroidAddonTest() {
        // for testing locally it can be uncommented
        // assumeThat(Connection.isOnline(), is(true));
        shadowOf(getMainLooper()).idle()

        // mapping for json fetched from http://registry.npmjs.org/ankidroid-js-addon-.../latest
        val mapper = AnkiSerialization.objectMapper

        // fetch package.json for the addon and read value to AddonModel
        val addonModel: AddonModel = mapper.readValue(URL(VALID_NPM_PACKAGE_NAME_URL), AddonModel::class.java)

        // test addon is valid or not
        assertTrue(addonModel.isValidAnkiDroidAddon())
    }

    @Test
    @Throws(IOException::class)
    fun notValidAnkiDroidAddonTest() {
        // for testing locally it can be uncommented
        // assumeThat(Connection.isOnline(), is(true));
        shadowOf(getMainLooper()).idle()

        // mapping for json fetched from http://registry.npmjs.org/ankidroid-js-addon-.../latest
        val mapper = AnkiSerialization.objectMapper

        // fetch package.json for the addon and read value to addonModel
        val addonModel: AddonModel = mapper.readValue(URL(NOT_VALID_NPM_PACKAGE_NAME_URL), AddonModel::class.java)

        // test that it is not a valid addon for AnkiDroid
        assertFalse(addonModel.isValidAnkiDroidAddon())
    }

    @Test
    fun updatePrefsTest() {
        shadowOf(getMainLooper()).idle()

        // test that prefs hashset for reviewer is empty
        var reviewerEnabledAddonSet = mPrefs.getStringSet(REVIEWER_ADDON, HashSet())
        assertEquals(0, reviewerEnabledAddonSet?.size)

        // mapping for json fetched from http://registry.npmjs.org/ankidroid-js-addon-.../latest
        val mapper = AnkiSerialization.objectMapper
        val addonModel: AddonModel = mapper.readValue(URL(VALID_NPM_PACKAGE_NAME_URL), AddonModel::class.java)

        // update the prefs make it enabled
        addonModel.updatePrefs(mPrefs, REVIEWER_ADDON, false)

        // test that new prefs added and size is 1 and the prefs hashset contains enabled addons name
        reviewerEnabledAddonSet = mPrefs.getStringSet(REVIEWER_ADDON, HashSet())
        assertEquals(1, reviewerEnabledAddonSet?.size)
        assertTrue(reviewerEnabledAddonSet!!.contains(addonModel.name))

        // now remove the addons from prefs
        addonModel.updatePrefs(mPrefs, REVIEWER_ADDON, true)

        // prefs hashset size for reviewer should be zero and prefs will not have addon name
        reviewerEnabledAddonSet = mPrefs.getStringSet(REVIEWER_ADDON, HashSet())
        assertEquals(0, reviewerEnabledAddonSet?.size)
        assertFalse(reviewerEnabledAddonSet!!.contains(addonModel.name))
    }
}
