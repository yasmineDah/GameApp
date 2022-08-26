package com.example.gameapp.models

import com.google.firebase.firestore.PropertyName

class UserImageList (
    @PropertyName("images") val images : List<String>? = null // this is supposed to be initialized because it holds data coming from firebase
        )
