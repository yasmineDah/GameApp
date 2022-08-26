package com.example.gameapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.nfc.Tag
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gameapp.models.ScreenSize
import com.example.gameapp.utils.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.util.*

class CreateActivity : AppCompatActivity() {

    companion object{
        private const val TAG  = "CreateActivity"
        private const val PICK_PHOTO_CODE = 65
        private const val READ_EXTERNAL_PHOTOS_CODE = 24
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }

    private lateinit var rvImagePiker : RecyclerView
    private lateinit var btnSave : Button
    private lateinit var etGameName :EditText
    private lateinit var pbUploading : ProgressBar

    private lateinit var screenSize : ScreenSize
    private lateinit var adapter : ImagePickerAdapter
    private var nbImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePiker = findViewById(R.id.rvImagePicker)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)
        etGameName = findViewById(R.id.etGameName)


        supportActionBar?.setDisplayHomeAsUpEnabled(true) // this to enable to come back to the main activity

        screenSize = intent.getSerializableExtra(EXTRA_SCREEN_SIZE) as ScreenSize
        nbImagesRequired = screenSize.getNbPairs()
        supportActionBar?.title = "Choose pics (0/$nbImagesRequired)"

        btnSave.setOnClickListener {
            saveDataToFireBase()
        }

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

         adapter = ImagePickerAdapter(this, chosenImageUris, screenSize, object : ImagePickerAdapter.ImageClickListener{
            override fun onPlaceholderClick() {
                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION))
                    launchIntentForImages()
                else
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
            }
        })
        rvImagePiker.adapter = adapter
        rvImagePiker.setHasFixedSize(true) // this makes sure that the RecyclerView has a fixed dimensions
        rvImagePiker.layoutManager = GridLayoutManager(this,screenSize.getWidth())
    }

    override fun onRequestPermissionsResult(  // this is a callback which will be generated after the response of the user on the permission dialog
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                launchIntentForImages()
            else
                Toast.makeText(this, "In order to create a custom game, you need tp provide access to your photos", Toast.LENGTH_LONG).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveOneImage(){
        val uri = chosenImageUris[1]
        val fileName = UUID.randomUUID().toString() + ".jpg"
        val ref = storage.reference.child(fileName)
        val uploadTask = ref.putFile(uri)

        uploadTask.continueWithTask { task ->
            if(!task.isSuccessful)
                Log.e(TAG,"exception is ", task.exception)
            ref.downloadUrl
        }.addOnCompleteListener { task ->
            if(task.isSuccessful)
                Log.i(TAG, "we are the best")
        }
    }


    private fun saveDataToFireBase() {
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        // check that we're not over writing someone else's data
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if(document != null && document.data != null){
                AlertDialog.Builder(this)
                    .setTitle("Name Taken")
                    .setMessage("A game already exists with the name $customGameName. Please choose another")
                    .setPositiveButton("OK",null)
                    .show()
                btnSave.isEnabled = true
            }else
                handleImageUploading(customGameName)
        }.addOnFailureListener { exception ->
            Log.e(TAG,"Couldn't retrieve the game name from fireStore",exception)
            Toast.makeText(this,"Couldn't retrieve the game name from fireStore",Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }
    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for((index, photoUri) in chosenImageUris.withIndex()){
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/${gameName}/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask{ photoUploadTask ->
                    Log.i(TAG,"Uploaded bytes : ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    // we have add this listener to be notified once an image have been successfully downloaded to storage
                    if(!downloadUrlTask.isSuccessful){
                        Log.e(TAG,"exception with Firebase Storage ", downloadUrlTask.exception)
                        Toast.makeText(this,"Failed to upload the image",Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
                    Log.i(TAG, "Finished uploading $photoUri, num uploaded ${uploadedImageUrls.size}")
                    if(uploadedImageUrls.size == chosenImageUris.size)
                        putImagesToFirestore(gameName,uploadedImageUrls)
                }
        }
    }

    private fun putImagesToFirestore(gameName: String, imageUrls: MutableList<String>) {
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                pbUploading.visibility = View.GONE
                if(!gameCreationTask.isSuccessful){
                    Log.e(TAG,"Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this,"Failed game creation",Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Let's play your game '$gameName'")
                    .setPositiveButton("Ok"){_,_ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK,resultData)
                        finish()
                    }.show()
            }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        // here we are getting the bitmap from the URI
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            // p means android Pie
            val source = ImageDecoder.createSource(contentResolver,photoUri)
            ImageDecoder.decodeBitmap(source)
        }else
            MediaStore.Images.Media.getBitmap(contentResolver,photoUri)
        Log.i(TAG, "the original width is ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaleBitmap = BitmapScaler().scaleToFitHeight(originalBitmap,250)
        Log.i(TAG,"the scaled width is ${scaleBitmap.width} and height ${scaleBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaleBitmap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream) // we have downscale the quality of the images
        return byteOutputStream.toByteArray()
    }

    private fun launchIntentForImages() {
        // this is an implicit intent
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // allow the user to pick many images
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if( requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null){
            Log.w(TAG, "Did not get data back from the launched activity, user likely canceled flow")
            return
        }
        val selectedUri = data.data // the case when the user choose only one photo
        val clipData = data.clipData // the case when the user choose multiple photos
        if(clipData != null){
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount){
                val clipItem = clipData.getItemAt(i)
                if(chosenImageUris.size < nbImagesRequired)
                    chosenImageUris.add(clipItem.uri)
            }
        }else if(selectedUri != null){
            Log.i(TAG, "data : $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $nbImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if(chosenImageUris.size != nbImagesRequired)
            return false
        if(etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH)
            return false
        return true
    }
}
