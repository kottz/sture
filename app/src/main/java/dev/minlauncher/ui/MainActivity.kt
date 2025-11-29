package dev.minlauncher.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dev.minlauncher.MinLauncherApp
import dev.minlauncher.R
import dev.minlauncher.databinding.ActivityMainBinding
import dev.minlauncher.util.isEinkDisplay
import dev.minlauncher.util.resetDefaultLauncher
import dev.minlauncher.util.toNightMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    lateinit var viewModel: MainViewModel
        private set
    
    companion object {
        const val REQUEST_CODE_LAUNCHER_SELECTOR = 100
        const val REQUEST_CODE_DEVICE_ADMIN = 101
    }
    
    override fun attachBaseContext(newBase: android.content.Context) {
        // Apply text scale before inflation
        val app = newBase.applicationContext as? MinLauncherApp
        val config = Configuration(newBase.resources.configuration)
        
        if (app != null) {
            val scale = kotlinx.coroutines.runBlocking {
                app.settingsDataStore.settings.first().textScale.value
            }
            config.fontScale = scale
        }
        
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        val app = application as MinLauncherApp
        if (isEinkDisplay()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setupNavigation()
        setupWindow()
        setupBackHandler()
        
        viewModel.checkDefaultLauncher()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }
    
    private fun setupWindow() {
        // Transparent system bars, edge to edge
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
    
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id != R.id.homeFragment) {
                    navController.popBackStack(R.id.homeFragment, false)
                }
            }
        })
    }
    
    override fun onStart() {
        super.onStart()
        viewModel.checkDefaultLauncher()
        viewModel.refreshAppList()
    }
    
    override fun onStop() {
        // Return to home when leaving
        if (navController.currentDestination?.id != R.id.homeFragment) {
            navController.popBackStack(R.id.homeFragment, false)
        }
        super.onStop()
    }
    
    override fun onUserLeaveHint() {
        // Return to home when user leaves
        if (navController.currentDestination?.id != R.id.homeFragment) {
            navController.popBackStack(R.id.homeFragment, false)
        }
        super.onUserLeaveHint()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Return to home on new intent (e.g., pressing home button)
        if (navController.currentDestination?.id != R.id.homeFragment) {
            navController.popBackStack(R.id.homeFragment, false)
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Theme might need updating on config change
        lifecycleScope.launch {
            val settings = viewModel.settings.value
            AppCompatDelegate.setDefaultNightMode(settings.theme.toNightMode())
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_LAUNCHER_SELECTOR -> {
                if (resultCode == RESULT_OK) {
                    resetDefaultLauncher()
                }
            }
            REQUEST_CODE_DEVICE_ADMIN -> {
                if (resultCode == RESULT_OK) {
                    viewModel.updateDoubleTapToLock(true)
                }
            }
        }
    }
}
