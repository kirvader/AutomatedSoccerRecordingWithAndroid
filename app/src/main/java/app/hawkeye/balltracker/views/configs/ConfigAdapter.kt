package app.hawkeye.balltracker.views.configs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.utils.createLogger
import app.hawkeye.balltracker.views.configs.choices.ConfigChoicesInfo

private val LOG = createLogger<ConfigAdapter>()

abstract class ConfigAdapter: RecyclerView.Adapter<ConfigViewHolder>() {
    abstract var choices: List<ConfigChoicesInfo>

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ConfigViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.config_item, parent, false)

        return ConfigViewHolder(view)
    }

    override fun getItemCount(): Int {
        return choices.size
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        val textViewName = holder.name
        textViewName.text = choices[position].stringInfo
        textViewName.setOnClickListener {
            choices[position].onClick()
            notifyDataSetChanged()
        }

        if (choices[position].isChosen()) {
            textViewName.setBackgroundResource(R.drawable.active_item_border)
        } else {
            textViewName.setBackgroundResource(R.drawable.disabled_border)
        }
    }
}