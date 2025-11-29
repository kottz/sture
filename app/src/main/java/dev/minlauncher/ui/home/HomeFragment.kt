package dev.minlauncher.ui.home

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dev.minlauncher.R
import dev.minlauncher.databinding.FragmentHomeBinding
import dev.minlauncher.domain.model.*
import dev.minlauncher.ui.MainActivity
import dev.minlauncher.ui.drawer.AppDrawerFragment
import dev.minlauncher.util.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel by lazy { (requireActivity() as MainActivity).viewModel }
    private val deviceManager by lazy {
        requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    
    private val homeAppViews = mutableListOf<TextView>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupGestures()
        setupClickListeners()
        observeState()
    }
    
    override fun onResume() {
        super.onResume()
        updateDateTime()
        viewModel.refreshScreenTime()
        viewModel.checkDefaultLauncher()
        
        lifecycleScope.launch {
            val settings = viewModel.settings.value
            if (settings.showStatusBar) showStatusBar() else hideStatusBar()
        }
    }
    
    private fun setupGestures() {
        binding.root.setOnTouchListener(object : GestureTouchListener(requireContext()) {
            override fun onClick() {
                // Do nothing
            }
            
            override fun onDoubleClick() {
                val settings = viewModel.settings.value
                if (settings.doubleTapToLock) {
                    lockScreen()
                }
            }
            
            override fun onLongClick() {
                findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
            }
            
            override fun onSwipeUp() {
                openAppDrawer()
            }
            
            override fun onSwipeDown() {
                val settings = viewModel.settings.value
                when (settings.swipeDownAction) {
                    SwipeDownAction.NOTIFICATIONS -> requireContext().expandNotificationDrawer()
                    SwipeDownAction.SEARCH -> requireContext().openWebSearch()
                }
            }
            
            override fun onSwipeLeft() {
                val settings = viewModel.settings.value
                if (settings.swipeLeftEnabled) {
                    launchGestureApp(viewModel.swipeLeftApp.value)
                }
            }
            
            override fun onSwipeRight() {
                val settings = viewModel.settings.value
                if (settings.swipeRightEnabled) {
                    launchGestureApp(viewModel.swipeRightApp.value)
                }
            }
        })
    }
    
    private fun setupClickListeners() {
        binding.clock.setOnClickListener { openClockApp() }
        binding.date.setOnClickListener { openCalendarApp() }
        binding.clock.setOnLongClickListener {
            openAppDrawerForSelection(AppDrawerFragment.Mode.SELECT_CLOCK_APP)
            true
        }
        binding.date.setOnLongClickListener {
            openAppDrawerForSelection(AppDrawerFragment.Mode.SELECT_CALENDAR_APP)
            true
        }
        
        binding.setDefaultLauncher.setOnClickListener {
            (requireActivity() as MainActivity).showLauncherSelector(
                MainActivity.REQUEST_CODE_LAUNCHER_SELECTOR
            )
        }
        binding.setDefaultLauncher.setOnLongClickListener {
            binding.setDefaultLauncher.isVisible = false
            true
        }
        
        binding.screenTime.setOnClickListener {
            openDigitalWellbeing()
        }
    }
    
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Combine settings and home apps for efficient updates
                launch {
                    combine(
                        viewModel.settings,
                        viewModel.homeApps,
                        viewModel.isDefaultLauncher
                    ) { settings, homeApps, isDefault ->
                        Triple(settings, homeApps, isDefault)
                    }.collectLatest { (settings, homeApps, isDefault) ->
                        updateHomeScreen(settings, homeApps)
                        binding.setDefaultLauncher.isVisible = !isDefault
                    }
                }
                
                launch {
                    viewModel.screenTime.collectLatest { stats ->
                        val settings = viewModel.settings.value
                        if (settings.showScreenTime && stats != null) {
                            binding.screenTime.text = stats.formatted
                            binding.screenTime.isVisible = true
                        } else {
                            binding.screenTime.isVisible = false
                        }
                    }
                }
                
                launch {
                    viewModel.isFirstLaunch.collectLatest { isFirst ->
                        binding.firstRunTips.isVisible = isFirst
                    }
                }
            }
        }
    }
    
    private fun updateHomeScreen(settings: LauncherSettings, homeApps: List<HomeApp>) {
        // Update date/time visibility
        updateDateTime()
        binding.dateTimeLayout.isVisible = settings.showDateTime != DateTimeVisibility.OFF
        binding.clock.isVisible = settings.showDateTime == DateTimeVisibility.ON
        binding.date.isVisible = settings.showDateTime != DateTimeVisibility.OFF
        
        // Update alignment
        val verticalGravity = if (settings.homeBottomAligned) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = settings.homeAlignment or verticalGravity
        binding.dateTimeLayout.gravity = settings.homeAlignment
        
        // Rebuild home app views if count changed
        if (homeAppViews.size != settings.homeAppCount) {
            rebuildHomeAppViews(settings.homeAppCount, settings.homeAlignment)
        } else {
            // Just update alignment
            homeAppViews.forEach { tv ->
                tv.gravity = settings.homeAlignment
            }
        }
        
        // Update home app labels
        val homeAppMap = homeApps.associateBy { it.position }
        homeAppViews.forEachIndexed { index, textView ->
            val homeApp = homeAppMap[index]
            if (homeApp != null && !homeApp.isEmpty) {
                // Check if app is still installed
                if (viewModel.isPackageInstalled(homeApp.packageName!!, homeApp.userHandle)) {
                    textView.text = homeApp.customLabel ?: ""
                } else {
                    textView.text = ""
                }
            } else {
                textView.text = ""
            }
        }
    }
    
    private fun rebuildHomeAppViews(count: Int, alignment: Int) {
        binding.homeAppsLayout.removeAllViews()
        homeAppViews.clear()
        
        repeat(count) { position ->
            val textView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTextAppearance(R.style.TextLarge)
                gravity = alignment
                val padding = requireContext().dpToPx(12)
                setPadding(0, padding, 0, padding)
                setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
                
                setOnClickListener {
                    val homeApps = viewModel.homeApps.value
                    val homeApp = homeApps.find { it.position == position }
                    if (homeApp != null && !homeApp.isEmpty) {
                        launchHomeApp(homeApp)
                    } else {
                        requireContext().showToast(R.string.long_press_to_select)
                    }
                }
                
                setOnLongClickListener {
                    openAppDrawerForSelection(AppDrawerFragment.Mode.SELECT_HOME_APP, position)
                    true
                }
            }
            
            homeAppViews.add(textView)
            binding.homeAppsLayout.addView(textView)
        }
    }
    
    private fun updateDateTime() {
        val settings = viewModel.settings.value
        
        // Format date with battery if status bar is hidden
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        var dateText = dateFormat.format(Date())
        
        if (!settings.showStatusBar) {
            val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (battery > 0) {
                dateText = "$dateText, $battery%"
            }
        }
        
        binding.date.text = dateText.replace(".,", ",")
    }
    
    private fun launchHomeApp(homeApp: HomeApp) {
        if (homeApp.packageName.isNullOrBlank()) return
        
        val userHandle = viewModel.getUserHandle(homeApp.userHandle)
        requireContext().launchApp(homeApp.packageName, homeApp.activityName, userHandle)
    }
    
    private fun launchGestureApp(gestureApp: GestureApp?) {
        if (gestureApp == null || gestureApp.packageName.isNullOrBlank()) {
            // Fall back to default apps
            when (gestureApp?.gesture) {
                Gesture.SWIPE_LEFT -> requireContext().openCameraApp()
                Gesture.SWIPE_RIGHT -> requireContext().openDialerApp()
                else -> {}
            }
            return
        }
        
        val userHandle = viewModel.getUserHandle(gestureApp.userHandle)
        requireContext().launchApp(gestureApp.packageName, gestureApp.activityName, userHandle)
    }
    
    private fun openClockApp() {
        val settings = viewModel.settings.value
        if (settings.clockAppPackage.isNotBlank()) {
            val userHandle = viewModel.getUserHandle(settings.clockAppUser)
            requireContext().launchApp(settings.clockAppPackage, settings.clockAppActivity, userHandle)
        } else {
            requireContext().openAlarmApp()
        }
    }
    
    private fun openCalendarApp() {
        val settings = viewModel.settings.value
        if (settings.calendarAppPackage.isNotBlank()) {
            val userHandle = viewModel.getUserHandle(settings.calendarAppUser)
            requireContext().launchApp(settings.calendarAppPackage, settings.calendarAppActivity, userHandle)
        } else {
            requireContext().openCalendarApp()
        }
    }
    
    private fun openAppDrawer() {
        findNavController().navigate(
            R.id.action_homeFragment_to_appDrawerFragment,
            bundleOf(AppDrawerFragment.ARG_MODE to AppDrawerFragment.Mode.LAUNCH.name)
        )
        viewModel.setFirstLaunchComplete()
    }
    
    private fun openAppDrawerForSelection(mode: AppDrawerFragment.Mode, position: Int = -1) {
        findNavController().navigate(
            R.id.action_homeFragment_to_appDrawerFragment,
            bundleOf(
                AppDrawerFragment.ARG_MODE to mode.name,
                AppDrawerFragment.ARG_POSITION to position
            )
        )
    }
    
    private fun openDigitalWellbeing() {
        try {
            val intent = android.content.Intent().apply {
                setClassName(
                    "com.google.android.apps.wellbeing",
                    "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity"
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = android.content.Intent().apply {
                    setClassName(
                        "com.samsung.android.forest",
                        "com.samsung.android.forest.launcher.LauncherActivity"
                    )
                }
                startActivity(intent)
            } catch (e: Exception) {
                requireContext().showToast(R.string.app_not_found)
            }
        }
    }
    
    private fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use accessibility service on P+
            binding.lockTrigger.contentDescription = LockAccessibilityService.LOCK_DESCRIPTION
            binding.lockTrigger.performClick()
            binding.lockTrigger.contentDescription = null
        } else {
            // Use device admin on older versions
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast(R.string.enable_double_tap_lock, long = true)
                findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
            }
        }
    }
    
    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = 
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }
    
    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = 
                View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        homeAppViews.clear()
        _binding = null
    }
}
