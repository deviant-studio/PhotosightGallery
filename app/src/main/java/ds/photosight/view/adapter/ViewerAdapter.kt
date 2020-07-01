package ds.photosight.view.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import ds.photosight.R
import ds.photosight.parser.PhotoInfo
import ds.photosight.view.SharedElementsHelper
import kotlinx.android.synthetic.main.item_gallery_photo.*
import kotlinx.android.synthetic.main.item_gallery_photo.photoImage
import kotlinx.android.synthetic.main.item_viewer_photo.*
import timber.log.Timber

class ViewerAdapter(
    val transitionHelper: SharedElementsHelper,
    val onClick: (PhotoInfo) -> Unit
) : PagingDataAdapter<PhotoInfo, SimpleViewHolder>(PhotoInfoDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val holder = SimpleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_viewer_photo, parent, false))
        holder.itemView.setOnClickListener {
            onClick(getItem(holder.bindingAdapterPosition)!!)
        }
        return holder
    }

    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        val item = getItem(position)!!

        transitionHelper.bindView(holder.photoImage, item.id.toString())

        val thumbnailRequest = Glide
            .with(holder.itemView)
            .load(item.thumb)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)


        val url = item.large

        Glide.with(holder.itemView)
            .load(url)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            //.thumbnail(thumbnailRequest)
            .transition(properTransition)
            .listener(object : RequestListener<Drawable> {
                private fun onCompleted(): Boolean {
                    Timber.v("$position: loaded")
                    transitionHelper.animate(position)
                    holder.photoProgress.hide()
                    return false
                }

                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean =
                    onCompleted()

                override fun onResourceReady(resource: Drawable, model: Any?, target: Target<Drawable>, dataSource: DataSource?, isFirstResource: Boolean): Boolean =
                    onCompleted()
            })
            .into(holder.photoImage)

    }


}