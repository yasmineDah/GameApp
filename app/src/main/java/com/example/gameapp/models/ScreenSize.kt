package com.example.gameapp.models

enum class ScreenSize (val nbCards : Int){
    EASY(8),
    MEDIUM(18),
    HARD(24);

    companion object{
        fun getSizeByValue(value : Int) = values().first { it.nbCards == value }
    }

    fun getWidth() : Int{
        return when(this){
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    fun getHeight() : Int{
        return nbCards / getWidth()
    }

    fun getNbPairs() : Int{
        return  nbCards / 2
    }
}