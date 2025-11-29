package dev.minlauncher.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dev.minlauncher.BuildConfig
import dev.minlauncher.R
import dev.minlauncher.databinding.FragmentSettingsBinding
import dev.minlauncher.domain.model.*
import dev.minlauncher.ui.MainActivity
import dev.minlauncher.ui.drawer.AppDrawerFragment
import dev.minlauncher.util.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel by lazy { (requireActivity() as MainActivity).viewModel }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeSettings()
    }
    
    private fun setupClickListeners() {
        // Header
        binding.hiddenAppsButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_appDrawerFragment,
                bundleOf(AppDrawerFragment.ARG_MODE to AppDrawerFragment.Mode.HIDDEN_APPS.name)
            )
        }
        
        binding.appInfoButton.setOnClickListener {
            requireContext().openAppInfo(
                BuildConfig.APPLICATION_ID,
                android.os.Process.myUserHandle()
            )
        }
        
        binding.setLauncherButton.setOnClickListener {
            (requireActivity() as MainActivity).showLauncherSelector(
                MainActivity.REQUEST_CODE_LAUNCHER_SELECTOR
            )
        }
        
        // Home screen
        binding.homeAppsCountButton.setOnClickListener {
            binding.homeAppsCountPicker.isVisible = !binding.homeAppsCountPicker.isVisible
        }
        
        setupNumberPicker(binding.homeAppsCountPicker, 0..12) { count ->
            viewModel.updateHomeAppCount(count)
            binding.homeAppsCountPicker.isVisible = false
        }
        
        binding.alignmentButton.setOnClickListener {
            binding.alignmentPicker.isVisible = !binding.alignmentPicker.isVisible
        }
        
        binding.alignmentLeft.setOnClickListener {
            viewModel.updateHomeAlignment(Gravity.START)
            binding.alignmentPicker.isVisible = false
        }
        binding.alignmentCenter.setOnClickListener {
            viewModel.updateHomeAlignment(Gravity.CENTER)
            binding.alignmentPicker.isVisible = false
        }
        binding.alignmentRight.setOnClickListener {
            viewModel.updateHomeAlignment(Gravity.END)
            binding.alignmentPicker.isVisible = false
        }
        binding.alignmentBottom.setOnClickListener {
            val settings = viewModel.settings.value
            viewModel.updateHomeBottomAligned(!settings.homeBottomAligned)
        }
        
        binding.dateTimeButton.setOnClickListener {
            binding.dateTimePicker.isVisible = !binding.dateTimePicker.isVisible
        }
        
        binding.dateTimeOn.setOnClickListener {
            viewModel.updateDateTimeVisibility(DateTimeVisibility.ON)
            binding.dateTimePicker.isVisible = false
        }
        binding.dateTimeOff.setOnClickListener {
            viewModel.updateDateTimeVisibility(DateTimeVisibility.OFF)
            binding.dateTimePicker.isVisible = false
        }
        binding.dateTimeDate.setOnClickListener {
            viewModel.updateDateTimeVisibility(DateTimeVisibility.DATE_ONLY)
            binding.dateTimePicker.isVisible = false
        }
        
        binding.screenTimeButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val screenTimeHelper = ScreenTimeHelper(requireContext())
                if (screenTimeHelper.hasPermission()) {
                    val settings = viewModel.settings.value
                    viewModel.updateShowScreenTime(!settings.showScreenTime)
                } else {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
        }
        
        binding.statusBarButton.setOnClickListener {
            val settings = viewModel.settings.value
            viewModel.updateShowStatusBar(!settings.showStatusBar)
        }
        
        // Appearance
        binding.themeButton.setOnClickListener {
            binding.themePicker.isVisible = !binding.themePicker.isVisible
        }
        
        binding.themeLight.setOnClickListener {
            viewModel.updateTheme(AppTheme.LIGHT)
            binding.themePicker.isVisible = false
            applyTheme(AppTheme.LIGHT)
        }
        binding.themeDark.setOnClickListener {
            viewModel.updateTheme(AppTheme.DARK)
            binding.themePicker.isVisible = false
            applyTheme(AppTheme.DARK)
        }
        binding.themeSystem.setOnClickListener {
            viewModel.updateTheme(AppTheme.SYSTEM)
            binding.themePicker.isVisible = false
            applyTheme(AppTheme.SYSTEM)
        }
        
        binding.textSizeButton.setOnClickListener {
            binding.textSizePicker.isVisible = !binding.textSizePicker.isVisible
        }
        
        binding.textSize1.setOnClickListener {
            viewModel.updateTextScale(TextScale.TINY)
            binding.textSizePicker.isVisible = false
            requireActivity().recreate()
        }
        binding.textSize2.setOnClickListener {
            viewModel.updateTextScale(TextScale.SMALL)
            binding.textSizePicker.isVisible = false
            requireActivity().recreate()
        }
        binding.textSize3.setOnClickListener {
            viewModel.updateTextScale(TextScale.NORMAL)
            binding.textSizePicker.isVisible = false
            requireActivity().recreate()
        }
        binding.textSize4.setOnClickListener {
            viewModel.updateTextScale(TextScale.LARGE)
            binding.textSizePicker.isVisible = false
            requireActivity().recreate()
        }
        binding.textSize5.setOnClickListener {
            viewModel.updateTextScale(TextScale.XLARGE)
            binding.textSizePicker.isVisible = false
            requireActivity().recreate()
        }
        
        binding.keyboardButton.setOnClickListener {
            val settings = viewModel.settings.value
            viewModel.updateAutoShowKeyboard(!settings.autoShowKeyboard)
        }
        
        // Gestures
        binding.swipeLeftButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_appDrawerFragment,
                bundleOf(AppDrawerFragment.ARG_MODE to AppDrawerFragment.Mode.SELECT_SWIPE_LEFT.name)
            )
        }
        binding.swipeLeftButton.setOnLongClickListener {
            val settings = viewModel.settings.value
            viewModel.toggleGestureEnabled(Gesture.SWIPE_LEFT, !settings.swipeLeftEnabled)
            true
        }
        
        binding.swipeRightButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_appDrawerFragment,
                bundleOf(AppDrawerFragment.ARG_MODE to AppDrawerFragment.Mode.SELECT_SWIPE_RIGHT.name)
            )
        }
        binding.swipeRightButton.setOnLongClickListener {
            val settings = viewModel.settings.value
            viewModel.toggleGestureEnabled(Gesture.SWIPE_RIGHT, !settings.swipeRightEnabled)
            true
        }
        
        binding.swipeDownButton.setOnClickListener {
            binding.swipeDownPicker.isVisible = !binding.swipeDownPicker.isVisible
        }
        
        binding.swipeDownNotifications.setOnClickListener {
            viewModel.updateSwipeDownAction(SwipeDownAction.NOTIFICATIONS)
            binding.swipeDownPicker.isVisible = false
        }
        binding.swipeDownSearch.setOnClickListener {
            viewModel.updateSwipeDownAction(SwipeDownAction.SEARCH)
            binding.swipeDownPicker.isVisible = false
        }
        
        binding.doubleTapLockButton.setOnClickListener {
            toggleDoubleTapLock()
        }
        
        // Footer links
        binding.githubButton.setOnClickListener {
            requireContext().openUrl("https://github.com/kottz/minlauncher")
        }
        binding.privacyButton.setOnClickListener {
            requireContext().openUrl("https://github.com/kottz/minlauncher")
        }
    }
    
    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.settings.collectLatest { settings ->
                        updateUI(settings)
                    }
                }
                
                launch {
                    viewModel.swipeLeftApp.collectLatest { app ->
                        binding.swipeLeftValue.text = app?.label ?: getString(R.string.camera)
                        val settings = viewModel.settings.value
                        binding.swipeLeftValue.alpha = if (settings.swipeLeftEnabled) 1f else 0.5f
                    }
                }
                
                launch {
                    viewModel.swipeRightApp.collectLatest { app ->
                        binding.swipeRightValue.text = app?.label ?: getString(R.string.phone)
                        val settings = viewModel.settings.value
                        binding.swipeRightValue.alpha = if (settings.swipeRightEnabled) 1f else 0.5f
                    }
                }
                
                launch {
                    viewModel.isDefaultLauncher.collectLatest { isDefault ->
                        binding.setLauncherButton.text = if (isDefault) {
                            getString(R.string.change_default_launcher)
                        } else {
                            getString(R.string.set_as_default_launcher)
                        }
                    }
                }
            }
        }
    }
    
    private fun updateUI(settings: LauncherSettings) {
        // Home screen
        binding.homeAppsCountValue.text = settings.homeAppCount.toString()
        
        binding.alignmentValue.text = when (settings.homeAlignment) {
            Gravity.START -> getString(R.string.left)
            Gravity.CENTER -> getString(R.string.center)
            Gravity.END -> getString(R.string.right)
            else -> getString(R.string.left)
        }
        binding.alignmentBottom.text = if (settings.homeBottomAligned) {
            getString(R.string.bottom_on)
        } else {
            getString(R.string.bottom_off)
        }
        
        binding.dateTimeValue.text = when (settings.showDateTime) {
            DateTimeVisibility.ON -> getString(R.string.on)
            DateTimeVisibility.OFF -> getString(R.string.off)
            DateTimeVisibility.DATE_ONLY -> getString(R.string.date_only)
        }
        
        binding.screenTimeValue.text = if (settings.showScreenTime) {
            getString(R.string.on)
        } else {
            getString(R.string.off)
        }
        binding.screenTimeLayout.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        
        binding.statusBarValue.text = if (settings.showStatusBar) {
            getString(R.string.on)
        } else {
            getString(R.string.off)
        }
        
        // Appearance
        binding.themeValue.text = when (settings.theme) {
            AppTheme.LIGHT -> getString(R.string.light)
            AppTheme.DARK -> getString(R.string.dark)
            AppTheme.SYSTEM -> getString(R.string.system)
        }
        
        binding.textSizeValue.text = TextScale.entries.indexOf(settings.textScale).plus(1).toString()
        
        binding.keyboardValue.text = if (settings.autoShowKeyboard) {
            getString(R.string.on)
        } else {
            getString(R.string.off)
        }
        
        // Gestures
        binding.swipeDownValue.text = when (settings.swipeDownAction) {
            SwipeDownAction.NOTIFICATIONS -> getString(R.string.notifications)
            SwipeDownAction.SEARCH -> getString(R.string.search)
        }
        
        binding.doubleTapLockValue.text = if (settings.doubleTapToLock || isAccessibilityServiceEnabled()) {
            getString(R.string.on)
        } else {
            getString(R.string.off)
        }
        
        // Update status bar based on settings
        if (settings.showStatusBar) showStatusBar() else hideStatusBar()
    }
    
    private fun setupNumberPicker(
        container: ViewGroup,
        range: IntRange,
        onSelected: (Int) -> Unit
    ) {
        container.removeAllViews()
        for (i in range) {
            val button = layoutInflater.inflate(R.layout.item_picker_number, container, false) as android.widget.TextView
            button.text = i.toString()
            button.setOnClickListener { onSelected(i) }
            container.addView(button)
        }
    }
    
    private fun applyTheme(theme: AppTheme) {
        AppCompatDelegate.setDefaultNightMode(theme.toNightMode())
    }
    
    private fun toggleDoubleTapLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Open accessibility settings
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            // Use device admin on older versions
            val deviceManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(requireContext(), dev.minlauncher.util.DeviceAdminReceiver::class.java)
            
            if (deviceManager.isAdminActive(componentName)) {
                deviceManager.removeActiveAdmin(componentName)
                viewModel.updateDoubleTapToLock(false)
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_explanation))
                }
                (requireActivity() as MainActivity).startActivityForResult(
                    intent,
                    MainActivity.REQUEST_CODE_DEVICE_ADMIN
                )
            }
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val serviceName = "${requireContext().packageName}/${LockAccessibilityService::class.java.name}"
        return enabledServices.contains(serviceName)
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
        _binding = null
    }
}
