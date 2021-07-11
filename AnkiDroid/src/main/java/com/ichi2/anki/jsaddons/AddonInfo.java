/****************************************************************************************
 * Copyright (c) 2021 Mani <infinyte01@gmail.com>                                       *
 *                                                                                      *
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

package com.ichi2.anki.jsaddons;

import android.annotation.SuppressLint;

import java.util.Map;

@SuppressLint("NonPublicNonStaticFieldName")
public class AddonInfo {
    // Update if api get updated
    // TODO Extract to resources from other classes
    public static final String ANKIDROID_JS_API = "0.0.1";
    public static final String ANKIDROID_JS_ADDON_KEYWORDS = "ankidroid-js-addon";
    public static final String REVIEWER_ADDON = "reviewer";
    public static final String NOTE_EDITOR_ADDON = "note-editor";

    private String name;
    private String version;
    private String description;
    private String main;
    private String ankidroidJsApi;
    private String addonType;
    private String[] keywords;
    private Map<String, String> author;
    private String license;
    private String homepage;
    private Map<String, String> dist;


    public String getName() {
        return name;
    }


    public String getVersion() {
        return version;
    }


    public String getDescription() {
        return description;
    }


    public String getMain() {
        return main;
    }


    public String getAnkidroidJsApi() {
        return ankidroidJsApi;
    }


    public String getAddonType() {
        return addonType;
    }


    public String[] getKeywords() {
        return keywords;
    }


    public Map<String, String> getAuthor() {
        return author;
    }


    public String getLicense() {
        return license;
    }


    public String getHomepage() {
        return homepage;
    }


    public Map<String, String> getDist() {
        return dist;
    }


    /**
     * @return boolean, if valid addon then return true else return false
     * if package.json of ankidroid-js-addon... contains valid ankidroid_js_api = 0.0.1 and keywords 'ankidroid-js-addon'
     * addon_type = reviewer or note editor
     */
    public static boolean isValidAnkiDroidAddon(AddonInfo addonInfo) {
        boolean jsAddonKeywordsPresent = false;

        // either fields not present in package.json or failed to parse the fields
        if (addonInfo.getName() == null || addonInfo.getMain() == null || addonInfo.getAnkidroidJsApi() == null
                || addonInfo.getAddonType() == null || addonInfo.getHomepage() == null || addonInfo.getKeywords() == null) {
            return false;
        }

        // if fields are empty
        if (addonInfo.getName().isEmpty() || addonInfo.getMain().isEmpty() || addonInfo.getAnkidroidJsApi().isEmpty()
                || addonInfo.getAddonType().isEmpty() || addonInfo.getHomepage().isEmpty()) {
            return false;
        }


        for (String keyword : addonInfo.getKeywords()) {
            if (keyword.equals(ANKIDROID_JS_ADDON_KEYWORDS)) {
                jsAddonKeywordsPresent = true;
                break;
            }
        }

        // addon package.json should have js_api_version, ankidroid-js-addon keywords and addon type
        return addonInfo.getAnkidroidJsApi().equals(ANKIDROID_JS_API) && jsAddonKeywordsPresent
                && (addonInfo.getAddonType().equals(REVIEWER_ADDON) || addonInfo.getAddonType().equals(NOTE_EDITOR_ADDON));
    }
}
