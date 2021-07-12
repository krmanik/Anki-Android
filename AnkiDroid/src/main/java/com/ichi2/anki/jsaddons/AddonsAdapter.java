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

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ichi2.anki.AnkiActivity;
import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.BackupManager;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.anki.R;
import com.ichi2.anki.UIUtils;
import com.ichi2.anki.dialogs.AddonInfoDialog;
import com.ichi2.anki.dialogs.ConfirmationDialog;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static com.ichi2.anki.AnkiActivity.showDialogFragment;

public class AddonsAdapter extends RecyclerView.Adapter<AddonsAdapter.AddonsViewHolder> {
    private SharedPreferences mPreferences;
    private Context mContext;
    List<AddonInfo> mAddonList;

    public AddonsAdapter(List<AddonInfo> addonList) {
        this.mAddonList = addonList;
    }


    @NonNull
    @Override
    public AddonsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        mPreferences = AnkiDroidApp.getSharedPrefs(mContext);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.addon_item, parent, false);
        return new AddonsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddonsAdapter.AddonsViewHolder holder, int position) {
        AddonInfo addonInfo = mAddonList.get(position);
        holder.mAddonTitleTextView.setText(addonInfo.getAddonTitle());
        holder.mAddonVersion.setText(addonInfo.getVersion());
        holder.mAddonDescription.setText(addonInfo.getDescription());

        // while binding viewholder if preferences w.r.t viewholder store true value or enabled status then
        // turn on switch status else it is off by default

        // store enabled/disabled status as boolean true/false value in SharedPreferences
        String jsAddonKey = AddonInfo.JS_ADDON_KEY;
        Set<String> enabledAddonSet = mPreferences.getStringSet(jsAddonKey, new HashSet<String>());

        for (String s : enabledAddonSet) {
            if (s.equals(addonInfo.getName())) {
                holder.mAddonActivate.setChecked(true);
            }
        }

        holder.mAddonActivate.setOnClickListener(v -> {
            if (holder.mAddonActivate.isChecked()) {
                addonInfo.updatePrefs(mPreferences, jsAddonKey, addonInfo.getName(), false);
                UIUtils.showThemedToast(mContext, mContext.getString(R.string.addon_enabled, addonInfo.getAddonTitle()), true);
            } else {
                addonInfo.updatePrefs(mPreferences, jsAddonKey, addonInfo.getName(), true);
                UIUtils.showThemedToast(mContext, mContext.getString(R.string.addon_disabled, addonInfo.getAddonTitle()), true);
            }
        });

        holder.mDetailsBtn.setOnClickListener(view -> {
            AddonInfoDialog addonInfoDialog = new AddonInfoDialog(mContext, addonInfo);
            showDialogFragment((AnkiActivity) mContext, addonInfoDialog);
        });

        // remove addon from directory and update prefs
        holder.mRemoveBtn.setOnClickListener(view -> {
            ConfirmationDialog dialog = new ConfirmationDialog();
            String title = addonInfo.getAddonTitle();
            String message = mContext.getString(R.string.confirm_remove_addon, addonInfo.getAddonTitle());
            dialog.setArgs(title, message);
            Runnable confirm = () -> {
                Timber.i("AddonsAdapter:: Delete addon pressed at %s", position);
                deleteAddonDir(addonInfo, position);
            };
            dialog.setConfirm(confirm);
            // AnkiActivity typecast here
            showDialogFragment((AnkiActivity) mContext, dialog);
        });
    }


    private void deleteAddonDir(AddonInfo addonInfo, int position) {
        // remove the js addon folder
        String currentAnkiDroidDirectory = CollectionHelper.getCurrentAnkiDroidDirectory(mContext);
        File mAddonsHomeDir = new File(currentAnkiDroidDirectory, "addons");
        File dir = new File(mAddonsHomeDir, addonInfo.getName());

        boolean deleted = BackupManager.removeDir(dir);

        if (!deleted) {
            UIUtils.showThemedToast(mContext, mContext.getString(R.string.failed_to_remove_addon), false);
            return;
        }

        addonInfo.updatePrefs(mPreferences, AddonInfo.JS_ADDON_KEY, addonInfo.getName(), true);
        mAddonList.remove(addonInfo);
        notifyDataSetChanged();
        notifyItemRemoved(position);
    }


    @Override
    public int getItemCount() {
        return mAddonList.size();
    }

    public class AddonsViewHolder extends RecyclerView.ViewHolder {
        TextView mAddonTitleTextView, mAddonDescription, mAddonVersion;
        Button mRemoveBtn, mDetailsBtn;
        SwitchCompat mAddonActivate;

        public AddonsViewHolder(@NonNull View itemView) {
            super(itemView);
            mAddonTitleTextView = itemView.findViewById(R.id.addon_title);
            mAddonTitleTextView = itemView.findViewById(R.id.addon_title);
            mAddonDescription = itemView.findViewById(R.id.addon_description);
            mAddonVersion = itemView.findViewById(R.id.addon_version);
            mAddonActivate = itemView.findViewById(R.id.activate_addon);
            mRemoveBtn = itemView.findViewById(R.id.addon_remove);
            mDetailsBtn = itemView.findViewById(R.id.addon_details);
        }
    }
}
