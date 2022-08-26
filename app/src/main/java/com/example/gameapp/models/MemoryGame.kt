package com.example.gameapp.models

import com.example.gameapp.utils.DEFAULT_ICONS

class MemoryGame(private val screenSize: ScreenSize, private val customImages: List<String>?){
    val cards : List<MemoryCard>
    var nbPairs = 0

    private var indexOfSingleSelectedCard : Int? = null
    private var nbCardsFlips = 0

    init {
        if(customImages == null){
            val chosenImages = DEFAULT_ICONS.shuffled().take(screenSize.getNbPairs()) // the function shuffled() is used to take a random images from the list of icons
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it) }
        }else{
            val randomizedImages = (customImages + customImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it.hashCode(),it) }
        }
    }

    fun flipCard(position: Int) : Boolean{
        nbCardsFlips++
        val card = cards[position]
        var foundMatch = false
        if (indexOfSingleSelectedCard == null){
            // 0 or 2 cards previously flipped over
            restoreCard()
            indexOfSingleSelectedCard = position
        } else {
            // exactly one card previously flipped over
            foundMatch = checkIfPositionsMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }

        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkIfPositionsMatch(pos1: Int, pos2: Int) : Boolean {
        if(cards[pos1].identifier != cards[pos2].identifier)
            return false
        cards[pos1].isMatched = true
        cards[pos2].isMatched = true
        nbPairs++
        return true
    }

    private fun restoreCard() {
        for(card in cards) {
            if(!card.isMatched)
                card.isFaceUp = false
        }
    }

    fun haveWonGame(): Boolean {
        return nbPairs == screenSize.getNbPairs()

    }

    fun isCardFacedUp(position: Int): Boolean {
        return cards[position].isFaceUp

    }

    fun getNbMoves(): Int {
        return nbCardsFlips / 2
    }
}
