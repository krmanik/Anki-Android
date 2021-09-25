package com.ichi2.anki.jsaddons

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.R

class AddonToolsAdapter(var addonToolsModelList: List<AddonToolsModel>, private val mOnAddonClickListener: OnAddonClickListener) : RecyclerView.Adapter<AddonToolsAdapter.AddonToolsViewHolder>() {
    private var mContext: Context? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddonToolsViewHolder {
        mContext = parent.context
        val inflater = LayoutInflater.from(mContext)
        val view: View = inflater.inflate(R.layout.addon_tool, parent, false)
        return AddonToolsViewHolder(view, mOnAddonClickListener)
    }

    override fun onBindViewHolder(holder: AddonToolsViewHolder, position: Int) {
        val addonToolsModel = addonToolsModelList[position]
        holder.addonBtn.background = addonToolsModel.icon.drawable
    }

    override fun getItemCount(): Int {
        return addonToolsModelList.size
    }

    inner class AddonToolsViewHolder(itemView: View, private var onAddonClickListener: OnAddonClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var addonBtn: ImageView = itemView.findViewById(R.id.addon_btn)
        override fun onClick(v: View) {
            onAddonClickListener.onAddonClick(bindingAdapterPosition)
        }

        init {
            // itemView.setOnClickListener(this);
            addonBtn.setOnClickListener(this)
        }
    }

    interface OnAddonClickListener {
        fun onAddonClick(position: Int)
    }
}
