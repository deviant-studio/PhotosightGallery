package ds.photosight.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer

abstract class SimpleAdapter<D : Any>(
    private val layoutId: Int,
    var data: List<D> = emptyList()
) : RecyclerView.Adapter<SimpleViewHolder>() {

    protected lateinit var context: Context

    init {
        @Suppress("LeakingThis")
        setHasStableIds(true)
    }

    fun updateData(new: List<D>) {
        data = new
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = data.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        context = recyclerView.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(layoutId, parent, false)
        return SimpleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        val item = getItem(position)
        onBind(holder, item, position)
    }

    fun getItem(position: Int): D = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    abstract fun onBind(holder: SimpleViewHolder, item: D, position: Int)
}

open class SimpleViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer