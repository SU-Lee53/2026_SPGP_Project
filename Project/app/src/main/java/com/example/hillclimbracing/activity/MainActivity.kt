package com.example.hillclimbracing.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.hillclimbracing.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onBtnStartGame(view: View) {
        startGameActivity()
    }

    private fun startGameActivity() {
        startActivity(Intent(this, HillClimbActivity::class.java))
    }
}
