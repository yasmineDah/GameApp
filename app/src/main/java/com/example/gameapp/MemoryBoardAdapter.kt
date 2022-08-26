package com.example.gameapp

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gameapp.models.MemoryCard
import com.example.gameapp.models.ScreenSize
import com.squareup.picasso.Picasso
import kotlin.math.min

class MemoryBoardAdapter(
    private val context: Context,
    private val screenSize: ScreenSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"
    }

    interface  CardClickListener{
        fun onCardClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width / screenSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / screenSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)

        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = screenSize.nbCards

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val cardMemory = cards[position]
            if(cardMemory.isFaceUp){
                if(cardMemory.imageUrl != null)
                    // we have added the placeholder function to fix the problem of the delay
                    Picasso.get().load(cardMemory.imageUrl).placeholder(R.drawable.ic_image).into(imageButton)
                else
                    imageButton.setImageResource(cardMemory.identifier)
            }
            else
                imageButton.setImageResource(R.drawable.trending1)

            imageButton.alpha = if (cardMemory.isMatched) .4f else 1.0f

            //******************** understand this
            val colorStateList = if(cardMemory.isMatched) ContextCompat.getColorStateList(context, R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)

            imageButton.setOnClickListener {
                Log.i(TAG, "clicked on the position is $position")
                cardClickListener.onCardClicked(position) // here where the position is transferred to mainActivity
            }
        }
    }
}
