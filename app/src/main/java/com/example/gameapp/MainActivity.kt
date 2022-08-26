package com.example.gameapp

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gameapp.models.MemoryGame
import com.example.gameapp.models.ScreenSize
import com.example.gameapp.models.UserImageList
import com.example.gameapp.utils.EXTRA_GAME_NAME
import com.example.gameapp.utils.EXTRA_SCREEN_SIZE
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var clRoot: CoordinatorLayout
    private lateinit var rvScreen : RecyclerView
    private lateinit var tvNbMoves : TextView
    private lateinit var tvNbPairs : TextView

    private val db = Firebase.firestore
    private var gameName : String? = null
    private var customGameImages : List<String>? = null
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var screenSize  = ScreenSize.EASY

    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 24
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvScreen = findViewById(R.id.rvScreen)
        tvNbMoves = findViewById(R.id.tvNbMoves)
        tvNbPairs = findViewById(R.id.tvNbPairs)

        /*val intent = Intent(this, CreateActivity::class.java)
        intent.putExtra(EXTRA_SCREEN_SIZE, ScreenSize.MEDIUM)
        startActivity(intent)*/

        setUpScreen()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh ->{
                if(memoryGame.getNbMoves() > 0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game ?", null, View.OnClickListener {
                        setUpScreen()
                    })
                }else
                    setUpScreen()
                return true
            }
            R.id.mi_new_size ->{
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom ->{
                showCreationDialog()
                return true
            }
            R.id.mi_download ->{
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null){
                Toast.makeText(this, "couldn't create the custom game",Toast.LENGTH_SHORT).show()
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showDownloadDialog() {
        val screenDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_screen, null)
        showAlertDialog("Fetch memory game", screenDownloadView, View.OnClickListener {
            val etDownloadGame = screenDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document->
            val userImageList = document.toObject(UserImageList::class.java)
            if(userImageList?.images == null){
                Snackbar.make(clRoot,"Sorry, we couldn't find any such game '$gameName'",Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            val nbCards = userImageList.images.size * 2
            screenSize = ScreenSize.getSizeByValue(nbCards)
            customGameImages = userImageList.images
            // to delete the delay of downloading the images
            for(imageUrl in userImageList.images){
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(clRoot,"Now you are playing $customGameName !",Snackbar.LENGTH_LONG).show()
            gameName = customGameName
            setUpScreen()

        }.addOnFailureListener { exception->
            Toast.makeText(this, "couldn't retrieve the game name $exception",Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCreationDialog() {
        val screenSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_screen_size, null)
        val radioGameSize = screenSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory screen", screenSizeView, View.OnClickListener {
            val desiredScreenSize = when (radioGameSize.checkedRadioButtonId){
                R.id.rbEasy -> ScreenSize.EASY
                R.id.rbMedium -> ScreenSize.MEDIUM
                else -> ScreenSize.HARD
            }
            // Navigate to a new activity
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_SCREEN_SIZE,desiredScreenSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
        val screenSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_screen_size, null)
        val radioGameSize = screenSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(screenSize){
            ScreenSize.EASY -> radioGameSize.check(R.id.rbEasy)
            ScreenSize.MEDIUM -> radioGameSize.check(R.id.rbMedium)
            ScreenSize.HARD -> radioGameSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", screenSizeView, View.OnClickListener {
            screenSize = when (radioGameSize.checkedRadioButtonId){
                R.id.rbEasy -> ScreenSize.EASY
                R.id.rbMedium -> ScreenSize.MEDIUM
                else -> ScreenSize.HARD
            }
            gameName = null
            customGameImages = null
            setUpScreen()
        })
    }

    private fun showAlertDialog(title : String, view : View?, positiveClickListener : View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok"){_,_ ->
                positiveClickListener.onClick(null)
            }.show()

    }

    private fun setUpScreen() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (screenSize){
            ScreenSize.EASY -> {
                tvNbMoves.text = "Easy : 4 * 2"
                tvNbPairs.text = "Pairs : 0 / 4"
            }
            ScreenSize.MEDIUM -> {
                tvNbMoves.text = "MEDIUM : 6 * 3"
                tvNbPairs.text = "Pairs : 0 / 9"
            }
            ScreenSize.HARD -> {
                tvNbMoves.text = "HARD : 6 * 4"
                tvNbPairs.text = "Pairs : 0 / 12"
            }
        }
        tvNbPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(screenSize, customGameImages)

        adapter = MemoryBoardAdapter(this, screenSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameImageFaces(position)
            }
        })
        rvScreen.adapter = adapter
        rvScreen.setHasFixedSize(true)
        rvScreen.layoutManager = GridLayoutManager(this, screenSize.getWidth())
    }

    private fun updateGameImageFaces(position: Int) {
        //error checking
        if(memoryGame.haveWonGame()) {
            Snackbar.make(clRoot, "You Have Already Won!", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFacedUp(position)) {
            Snackbar.make(clRoot, "Invalid Move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        if(memoryGame.flipCard(position)){
            Log.i(TAG, "Found a match, Num pairs found : ${memoryGame.nbPairs}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.nbPairs.toFloat() / screenSize.getNbPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tvNbPairs.setTextColor(color)
            tvNbPairs.text = "Pairs: ${memoryGame.nbPairs} / ${screenSize.getNbPairs()}"
            if(memoryGame.haveWonGame()){
                Snackbar.make(clRoot, "You won! Congratulations.", Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot,intArrayOf(Color.GREEN,Color.CYAN,Color.RED)).oneShot()
            }
        }
        tvNbMoves.text = "Moves: ${memoryGame.getNbMoves()}"
        adapter.notifyDataSetChanged()
    }
}