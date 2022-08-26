package app.hawkeye.balltracker.views.configs

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.hawkeye.balltracker.R

class ConfigViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var name: TextView
    init {
        name = itemView.findViewById(R.id.config_item_name)
    }
}