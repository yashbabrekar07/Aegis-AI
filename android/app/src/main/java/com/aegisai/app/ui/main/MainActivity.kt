package com.aegisai.app.ui.main

import android.content.Intent
import android.os.Bundle
import com.aegisai.app.ui.login.VerifyPhoneActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aegisai.app.AegisApp
import com.aegisai.app.call.CallGuardController
import com.aegisai.app.data.SessionHelper
import com.aegisai.app.databinding.ActivityMainBinding
import com.aegisai.app.ui.login.LoginActivity
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = AegisApp.get(this)
        if (!app.prefs.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (app.prefs.needsPhoneVerification()) {
            startActivity(Intent(this, VerifyPhoneActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            SessionHelper.refreshUserFromToken(this@MainActivity)
        }

        CallGuardController.sync(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(binding.navHost.id) as NavHostFragment
        val navController = navHost.navController

        binding.bottomNav?.let { setupNav(it, navController) }
        binding.navRail?.let { setupNav(it, navController) }

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomNav?.updatePadding(bottom = bars.bottom)
            binding.navRail?.updatePadding(top = bars.top, bottom = bars.bottom)
            binding.navHost.updatePadding(bottom = bars.bottom / 2)
            insets
        }
    }

    private fun setupNav(navView: NavigationBarView, navController: androidx.navigation.NavController) {
        navView.setupWithNavController(navController)
        navView.itemIconSize = (24 * resources.displayMetrics.density).toInt()
    }
}
