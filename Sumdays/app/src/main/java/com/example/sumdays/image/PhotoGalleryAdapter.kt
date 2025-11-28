package com.example.sumdays.image

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sumdays.R

class PhotoGalleryAdapter(
    private val onPhotoClick: (String) -> Unit,
    private val onPhotoLongClick: (Int) -> Unit,
    private val onAddClick: () -> Unit
) : ListAdapter<GalleryItem, RecyclerView.ViewHolder>(GalleryDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_PHOTO = 1
        private const val VIEW_TYPE_ADD = 2
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is GalleryItem.Photo -> VIEW_TYPE_PHOTO
            is GalleryItem.Add -> VIEW_TYPE_ADD
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PHOTO -> PhotoViewHolder(
                inflater.inflate(R.layout.item_photo_gallery, parent, false)
            )
            VIEW_TYPE_ADD -> AddViewHolder(
                inflater.inflate(R.layout.item_photo_gallery_add, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GalleryItem.Photo -> (holder as PhotoViewHolder).bind(
                url = item.url,
                position = position,
                onClick = onPhotoClick,
                onLongClick = onPhotoLongClick
            )
            is GalleryItem.Add -> (holder as AddViewHolder).bind(onAddClick)
        }
    }

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.gallery_image)

        fun bind(
            url: String,
            position: Int,
            onClick: (String) -> Unit,
            onLongClick: (Int) -> Unit
        ) {
            Glide.with(itemView.context)
                .load(Uri.parse(url))
                .centerCrop()
                .into(imageView)

            itemView.setOnClickListener { onClick(url) }
            itemView.setOnLongClickListener {
                onLongClick(position)
                true
            }
        }
    }

    class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(onClick: () -> Unit) {
            itemView.setOnClickListener { onClick() }
        }
    }
}
