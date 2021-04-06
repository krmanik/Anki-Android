/*
 *  Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.anki.cardviewer;

import android.content.Context;

import com.ichi2.anki.AddonsNpmUtility;
import com.ichi2.anki.AnkiDroidApp;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

public class CardTemplate {
    private final String mPreStyle;
    private final String mPreClass;
    private final String mPreContent;
    private final String mPostContent;
    private final String mAddonsContent;
    private final String mEnabledAddons;

    public CardTemplate(@NonNull String template, Context context) {
        // Note: This refactoring means the template must be in the specific order of style, class, content.
        // Since this is a const loaded from an asset file, I'm fine with this.
        String classDelim = "::class::";
        String styleDelim = "::style::";
        String contentDelim = "::content::";
        String addonContentDelim = "::addons::";
        // get enabled addons content and set to addons
        mEnabledAddons = setAddons(context);

        int styleIndex = template.indexOf(styleDelim);
        int classIndex = template.indexOf(classDelim);
        int contentIndex = template.indexOf(contentDelim);
        int addonsIndex = template.indexOf(addonContentDelim);

        try {
            this.mPreStyle = template.substring(0, styleIndex);
            this.mPreClass = template.substring(styleIndex + styleDelim.length(), classIndex);
            this.mPreContent = template.substring(classIndex + classDelim.length(), contentIndex);
            this.mPostContent = template.substring(contentIndex + contentDelim.length(), addonsIndex);
            this.mAddonsContent = template.substring(addonsIndex + addonContentDelim.length());
        } catch (StringIndexOutOfBoundsException ex) {
            throw new IllegalStateException("The card template had replacement string order, or content changed", ex);
        }
    }

    @CheckResult
    @NonNull
    public String render(String content, String style, String cardClass) {
        return mPreStyle + style + mPreClass + cardClass + mPreContent + content + mPostContent + mEnabledAddons + mAddonsContent;
    }

    public String setAddons(Context context) {
        if (AnkiDroidApp.getSharedPrefs(context).getBoolean("javascript_addons_support_prefs", false)) {
            return AddonsNpmUtility.getEnabledAddonsContent(context);
        }
        return "";
    }
}
