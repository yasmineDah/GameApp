package com.example.gameapp

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.gameapp.models.ScreenSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val screenSize: ScreenSize,
    private val imageClickListener: ImageClickListener) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener{
        fun onPlaceholderClick()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent,false)
        val cardWidth = parent.width / screenSize.getWidth()
        val cardHeight  = parent.height / screenSize.getHeight()
        val cardSideLength = min(cardHeight,cardWidth)
        val layoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position < imageUris.size)
            holder.bind(imageUris[position])
        else
            holder.bind()
    }

    override fun getItemCount() = screenSize.getNbPairs()

    inner class ViewHolder (itemView :View) : RecyclerView.ViewHolder(itemView){
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri : Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null) // that means that we'll no more respond to a click
        }

        fun bind() {
            ivCustomImage.setOnClickListener {
                imageClickListener.onPlaceholderClick()
            }
        }
    }

}
