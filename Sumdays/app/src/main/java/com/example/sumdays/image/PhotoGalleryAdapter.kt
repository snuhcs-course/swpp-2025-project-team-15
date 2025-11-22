package com.example.sumdays

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sumdays.databinding.ItemPhotoGalleryBinding
import com.example.sumdays.databinding.ItemPhotoGalleryAddBinding

// 사진첩 아이템 타입 정의 (사진 또는 추가 버튼)
sealed class PhotoGalleryItem {
    data class Photo(val uri: Uri) : PhotoGalleryItem()
    object AddButton : PhotoGalleryItem()
}

class PhotoGalleryAdapter(
    private val onPhotoClick: (Uri) -> Unit,
    private val onAddClick: () -> Unit
) : ListAdapter<PhotoGalleryItem, PhotoGalleryAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    // 뷰 홀더 타입 상수
    companion object {
        private const val TYPE_PHOTO = 0
        private const val TYPE_ADD_BUTTON = 1
    }

    // 뷰 홀더 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return when (viewType) {
            TYPE_PHOTO -> {
                val binding = ItemPhotoGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PhotoViewHolder.PhotoItemViewHolder(binding)
            }
            TYPE_ADD_BUTTON -> {
                val binding = ItemPhotoGalleryAddBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PhotoViewHolder.AddButtonViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    // 뷰 홀더 바인딩
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        when (holder) {
            is PhotoViewHolder.PhotoItemViewHolder -> {
                val item = getItem(position) as PhotoGalleryItem.Photo
                holder.bind(item.uri, onPhotoClick)
            }
            is PhotoViewHolder.AddButtonViewHolder -> {
                holder.bind(onAddClick)
            }
        }
    }

    // 아이템 타입 반환
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PhotoGalleryItem.Photo -> TYPE_PHOTO
            is PhotoGalleryItem.AddButton -> TYPE_ADD_BUTTON
        }
    }

    // 뷰 홀더 클래스
    sealed class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 사진 아이템 뷰 홀더
        class PhotoItemViewHolder(private val binding: ItemPhotoGalleryBinding) : PhotoViewHolder(binding.root) {
            fun bind(uri: Uri, onPhotoClick: (Uri) -> Unit) {
                Glide.with(itemView.context)
                    .load(uri)
                    .centerCrop()
                    .into(binding.galleryImage)
                itemView.setOnClickListener { onPhotoClick(uri) }
            }
        }
        // 추가 버튼 뷰 홀더
        class AddButtonViewHolder(private val binding: ItemPhotoGalleryAddBinding) : PhotoViewHolder(binding.root) {
            fun bind(onAddClick: () -> Unit) {
                itemView.setOnClickListener { onAddClick() }
            }
        }
    }
}

// DiffUtil 콜백
class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoGalleryItem>() {
    override fun areItemsTheSame(oldItem: PhotoGalleryItem, newItem: PhotoGalleryItem): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: PhotoGalleryItem, newItem: PhotoGalleryItem): Boolean {
        return oldItem == newItem
    }
}