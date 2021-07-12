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

package com.ichi2.anki.dialogs;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.ichi2.anki.AddonBrowser;
import com.ichi2.anki.AnkiActivity;
import com.ichi2.anki.R;
import com.ichi2.anki.UIUtils;
import com.ichi2.anki.jsaddons.AddonInfo;
import com.ichi2.anki.jsaddons.DownloadAddonListener;
import com.ichi2.anki.jsaddons.NpmPackageDownloader;
import com.ichi2.async.TaskManager;

import java.util.Objects;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

public class AddonInfoDialog extends DialogFragment {
    private final Context mContext;
    private final AddonInfo mAddonInfo;


    public AddonInfoDialog(Context context, AddonInfo addonInfo) {
        this.mContext = context;
        this.mAddonInfo = addonInfo;
    }


    @Override
    public void onStart() {
        super.onStart();
        Objects.requireNonNull(getDialog()).getWindow()
                .setLayout(getScreenWidth(),
                        WindowManager.LayoutParams.WRAP_CONTENT);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setCancelable(true);

        Objects.requireNonNull(getDialog()).setTitle(mAddonInfo.getAddonTitle());

        View view = inflater.inflate(R.layout.addon_details, null, false);

        Toolbar toolbar = view.findViewById(R.id.addon_toolbar);
        toolbar.setNavigationOnClickListener(v -> dismiss());

        TextView addonTitle = view.findViewById(R.id.detail_addon_title);
        addonTitle.setText(mAddonInfo.getAddonTitle());

        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.addon_details_linear_layout);

        linearLayout.addView(createTitleTextView(mContext.getString(R.string.npm_package_name)));
        linearLayout.addView(createContentTextView(mAddonInfo.getName()));

        linearLayout.addView(createSeparator());
        linearLayout.addView(createTitleTextView(mContext.getString(R.string.addon_description)));
        linearLayout.addView(createContentTextView(mAddonInfo.getDescription()));

        linearLayout.addView(createSeparator());
        linearLayout.addView(createTitleTextView(mContext.getString(R.string.addon_version)));
        linearLayout.addView(createContentTextView(mAddonInfo.getVersion()));

        linearLayout.addView(createSeparator());
        linearLayout.addView(createTitleTextView(mContext.getString(R.string.ankidroid_js_api_version)));
        linearLayout.addView(createContentTextView(mAddonInfo.getAnkidroidJsApi()));

        linearLayout.addView(createSeparator());
        linearLayout.addView(createTitleTextView(mContext.getString(R.string.addon_type)));
        linearLayout.addView(createContentTextView(mAddonInfo.getAddonType()));

        linearLayout.addView(createSeparator());
        linearLayout.addView(createTitleTextView(mContext.getString(R.string.addon_author)));
        linearLayout.addView(createContentTextView(mAddonInfo.getAuthor().get("name")));

        linearLayout.addView(createSeparator());
        linearLayout.addView(createTitleTextView(mContext.getString(R.string.addon_license)));
        linearLayout.addView(createContentTextView(mAddonInfo.getLicense()));

        linearLayout.addView(createSeparator());

        Button homepageBtn = view.findViewById(R.id.view_addon_homepage_button);
        Button updateBtn = view.findViewById(R.id.addon_update_button);

        AnkiActivity ankiActivity = (AnkiActivity) mContext;
        homepageBtn.setOnClickListener(v -> {
            dismiss();
            Uri uri = Uri.parse(mAddonInfo.getHomepage());
            ankiActivity.openUrl(uri);
        });

        updateBtn.setOnClickListener(v -> {
            dismiss();
            AddonBrowser addonBrowser = (AddonBrowser) mContext;
            UIUtils.showThemedToast(mContext, mContext.getString(R.string.updating_addon, mAddonInfo.getAddonTitle()), true);
            TaskManager.launchCollectionTask(new NpmPackageDownloader.DownloadAddon(addonBrowser, mAddonInfo.getName()), new DownloadAddonListener(addonBrowser));
        });
        return view;
    }


    public int getScreenWidth() {
        Point size = new Point();
        Activity activity = (Activity) mContext;
        activity.getWindowManager().getDefaultDisplay().getSize(size);
        return size.x;
    }


    private TextView createTitleTextView(String text) {
        TextView textView = new TextView(mContext);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return textView;
    }


    private TextView createContentTextView(String text) {
        TextView textView = new TextView(mContext);
        textView.setText(text);
        textView.setTextSize(18);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return textView;
    }


    private View createSeparator() {
        View viewDivider = new View(mContext);
        int dividerHeightDP = (int) getResources().getDisplayMetrics().density * 2;
        int marginDP = (int) getResources().getDisplayMetrics().density * 10;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dividerHeightDP);
        params.setMargins(0, marginDP, 0, marginDP);
        viewDivider.setLayoutParams(params);
        viewDivider.setBackgroundColor(ContextCompat.getColor(mContext, R.color.material_blue_grey_050));
        return viewDivider;
    }
}
