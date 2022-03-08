/****************************************************************************************
 * Copyright (c) 2021 Mani <infinyte01@gmail.com>                                       *
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
import com.github.zafarkhaja.semver.Version
import com.ichi2.anki.AnkiDroidJsAPIConstants
import com.ichi2.anki.jsaddons.AddonsConst.ANKIDROID_JS_ADDON_KEYWORDS
import com.ichi2.anki.jsaddons.AddonsConst.NOTE_EDITOR_ADDON
import com.ichi2.anki.jsaddons.AddonsConst.REVIEWER_ADDON
import com.ichi2.anki.jsaddons.NpmUtils.validateName
import timber.log.Timber

/**
 * When package.json fetched from https://registry.npmjs.org/some-addon/latest,
 * all the required fields in package.json mapped to AddonModel in this class.
 * The most important fields in package.json are
 * ankiDroidJsApi, addonType and keywords, these fields distinguish other npm packages
 */

data class AddonModel(
    val name: String? = null, // name of npm package, it unique for each package listed on npm
    val addonTitle: String? = null, // for showing in AnkiDroid
    val icon: String? = null, // only required for note editor (single character recommended)
    val version: String? = null,
    val description: String? = null,
    val main: String? = null,
    val ankidroidJsApi: String? = null,
    val addonType: String? = null,
    val keywords: Array<String>? = null,
    val author: Map<String, String>? = null,
    val license: String? = null,
    val homepage: String? = null,
    val dist: Map<String, String>? = null
) {

    /**
     * Update preferences for addons with boolean remove, the preferences will be used to store the information about
     * enabled and disabled addon. So, that other method will return content of script to reviewer or note editor
     *
     * @param preferences
     * @param jsAddonKey  REVIEWER_ADDON_KEY
     * @param remove    true for removing from prefs
     *
     * Android returns a reference to StringSet in SharedPreferences but does not mark it as modified if any changes made,
     * so commit/apply won't persist it. It needs to make a copy and set the new copy in to persist any StringSet changes
     * in SharedPreferences.
     * https://stackoverflow.com/questions/19949182/android-sharedpreferences-string-set-some-items-are-removed-after-app-restart/19949833
     */
    fun updatePrefs(preferences: SharedPreferences, jsAddonKey: String, remove: Boolean) {
        val reviewerEnabledAddonSet = preferences.getStringSet(jsAddonKey, HashSet())
        val newStrSet: MutableSet<String> = reviewerEnabledAddonSet?.toHashSet()!!

        if (remove) {
            newStrSet.remove(name)
        } else {
            if (name != null) {
                newStrSet.add(name)
            }
        }

        preferences.edit().putStringSet(jsAddonKey, newStrSet).apply()
    }
}

/**
 * Check if npm package is valid or not by fields ankidroidJsApi, keywords (ankidroid-js-addon) and
 * addon_type (reviewer or note editor) in addonModel
 *
 * @return true for valid addon else false
 */
fun AddonModel.isValidAnkiDroidAddon(): Boolean {
    // either fields not present in package.json or failed to parse the fields
    if (name.isNullOrBlank() || addonTitle.isNullOrBlank() || main.isNullOrBlank() ||
        ankidroidJsApi.isNullOrBlank() || addonType.isNullOrBlank() || homepage.isNullOrBlank() ||
        keywords.isNullOrEmpty()
    ) {
        Timber.w("Invalid addon package: fields in package.json are empty or null")
        return false
    }

    // check if name is safe and valid
    if (!validateName(name)) {
        Timber.w("Invalid addon package: package name failed validation")
        return false
    }

    if (addonType != REVIEWER_ADDON && addonType != NOTE_EDITOR_ADDON) {
        Timber.w("Invalid addon package: package.json must have 'addonType' fields of 'reviewer' or 'note-editor'")
        return false
    }

    // if addon type is note editor then it must have icon
    if (addonType == NOTE_EDITOR_ADDON && icon.isNullOrBlank()) {
        Timber.w("Invalid addon package: note editor addon must have 'icon' fields in package.json")
        return false
    }

    // check if ankidroid-js-addon present or not in mapped addonModel
    val jsAddonKeywordsPresent = keywords.any { it == ANKIDROID_JS_ADDON_KEYWORDS }
    if (!jsAddonKeywordsPresent) {
        Timber.w("Invalid addon package: package.json must have 'ankidroid-js-addon' in keywords")
        return false
    }

    try {
        // Check supplied api and current api
        val versionCurrent = Version.valueOf(AnkiDroidJsAPIConstants.sCurrentJsApiVersion)
        val versionSupplied = Version.valueOf(ankidroidJsApi)

        if (!versionSupplied.equals(versionCurrent)) {
            Timber.w("Invalid addon package: supplied js api version must be equal to current js api version")
            return false
        }
    } catch (e: Exception) {
        Timber.w("Invalid addon package: invalid characters in js api version string")
        return false
    }

    return true
}
