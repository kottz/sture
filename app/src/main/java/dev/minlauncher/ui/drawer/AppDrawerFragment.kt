package dev.minlauncher.ui.drawer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.minlauncher.R
import dev.minlauncher.databinding.FragmentAppDrawerBinding
import dev.minlauncher.domain.model.App
import dev.minlauncher.domain.model.Gesture
import dev.minlauncher.ui.MainActivity
import dev.minlauncher.util.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.Normalizer

class AppDrawerFragment : Fragment() {
    
    companion object {
        const val ARG_MODE = "mode"
        const val ARG_POSITION = "position"
    }
    
    enum class Mode {
        LAUNCH,
        SELECT_HOME_APP,
        SELECT_SWIPE_LEFT,
        SELECT_SWIPE_RIGHT,
        SELECT_CLOCK_APP,
        SELECT_CALENDAR_APP,
        HIDDEN_APPS
    }
    
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel by lazy { (requireActivity() as MainActivity).viewModel }
    
    private lateinit var adapter: AppListAdapter
    private var mode: Mode = Mode.LAUNCH
    private var homePosition: Int = -1
    
    private var allApps: List<App> = emptyList()
    private var filteredApps: List<App> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mode = arguments?.getString(ARG_MODE)?.let { Mode.valueOf(it) } ?: Mode.LAUNCH
        homePosition = arguments?.getInt(ARG_POSITION, -1) ?: -1
        
        setupSearchView()
        setupRecyclerView()
        observeApps()
        
        updateHint()
    }
    
    override fun onStart() {
        super.onStart()
        val settings = viewModel.settings.value
        if (settings.autoShowKeyboard) {
            binding.searchEditText.showKeyboard()
        }
    }
    
    override fun onStop() {
        binding.searchEditText.hideKeyboard()
        super.onStop()
    }
    
    private fun setupSearchView() {
        val settings = viewModel.settings.value
        binding.searchEditText.gravity = settings.appLabelAlignment
        
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
        })
        
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = binding.searchEditText.text?.toString() ?: ""
                if (query.startsWith("!")) {
                    // DuckDuckGo bang search
                    requireContext().openUrl("https://duckduckgo.com/?q=${query.drop(1)}")
                } else if (filteredApps.isEmpty()) {
                    requireContext().openWebSearch(query)
                } else {
                    launchFirstApp()
                }
                true
            } else false
        }
        
        binding.renameButton.setOnClickListener {
            val name = binding.searchEditText.text?.toString()?.trim()
            if (name.isNullOrBlank()) {
                requireContext().showToast(R.string.enter_name_first)
                return@setOnClickListener
            }
            // This is for home app renaming
            if (mode == Mode.SELECT_HOME_APP && homePosition >= 0) {
                viewModel.setHomeAppLabel(homePosition, name)
                findNavController().popBackStack()
            }
        }
    }
    
    private fun setupRecyclerView() {
        val settings = viewModel.settings.value
        
        adapter = AppListAdapter(
            alignment = settings.appLabelAlignment,
            isHiddenMode = mode == Mode.HIDDEN_APPS,
            onAppClick = { app -> onAppSelected(app) },
            onAppLongClick = { app -> showAppOptions(app) },
            onHideClick = { app -> toggleAppHidden(app) },
            onInfoClick = { app -> openAppInfo(app) },
            onDeleteClick = { app -> deleteApp(app) },
            onRenameClick = { app, name -> renameApp(app, name) }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        if (!recyclerView.canScrollVertically(-1)) {
                            binding.searchEditText.hideKeyboard()
                        }
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1)) {
                            binding.searchEditText.hideKeyboard()
                        } else if (!recyclerView.canScrollVertically(-1)) {
                            val settings = viewModel.settings.value
                            if (settings.autoShowKeyboard) {
                                binding.searchEditText.showKeyboard()
                            }
                        }
                    }
                }
                
                // Swipe down to dismiss
                if (!recyclerView.canScrollVertically(-1) && 
                    newState == RecyclerView.SCROLL_STATE_DRAGGING
                ) {
                    // Allow overscroll to trigger back
                }
            }
        })
    }
    
    private fun observeApps() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (mode == Mode.HIDDEN_APPS) {
                    viewModel.hiddenApps.collectLatest { apps ->
                        allApps = apps
                        filterApps(binding.searchEditText.text?.toString() ?: "")
                    }
                } else {
                    viewModel.allApps.collectLatest { apps ->
                        allApps = apps
                        filterApps(binding.searchEditText.text?.toString() ?: "")
                    }
                }
            }
        }
    }
    
    private fun filterApps(query: String) {
        val normalizedQuery = query.trim().lowercase()
        
        filteredApps = if (normalizedQuery.isEmpty()) {
            allApps
        } else {
            allApps.filter { app ->
                val label = app.displayLabel.lowercase()
                val normalizedLabel = Normalizer.normalize(label, Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                
                label.contains(normalizedQuery) || normalizedLabel.contains(normalizedQuery)
            }
        }
        
        adapter.submitList(filteredApps)
        
        // Auto-launch if single match and in launch mode
        if (filteredApps.size == 1 && 
            mode == Mode.LAUNCH && 
            !query.startsWith(" ") && // Space prefix disables auto-launch
            !query.startsWith("!") // Bang search
        ) {
            onAppSelected(filteredApps[0])
        }
        
        // Show rename button in select mode
        binding.renameButton.isVisible = mode == Mode.SELECT_HOME_APP && query.isNotBlank()
    }
    
    private fun launchFirstApp() {
        if (filteredApps.isNotEmpty()) {
            onAppSelected(filteredApps[0])
        }
    }
    
    private fun onAppSelected(app: App) {
        binding.searchEditText.hideKeyboard()
        
        when (mode) {
            Mode.LAUNCH, Mode.HIDDEN_APPS -> {
                requireContext().launchApp(app.packageName, app.activityName, app.userHandle)
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            Mode.SELECT_HOME_APP -> {
                if (homePosition >= 0) {
                    viewModel.setHomeApp(homePosition, app)
                }
                findNavController().popBackStack()
            }
            Mode.SELECT_SWIPE_LEFT -> {
                viewModel.setGestureApp(Gesture.SWIPE_LEFT, app)
                findNavController().popBackStack()
            }
            Mode.SELECT_SWIPE_RIGHT -> {
                viewModel.setGestureApp(Gesture.SWIPE_RIGHT, app)
                findNavController().popBackStack()
            }
            Mode.SELECT_CLOCK_APP -> {
                viewModel.updateClockApp(app)
                findNavController().popBackStack()
            }
            Mode.SELECT_CALENDAR_APP -> {
                viewModel.updateCalendarApp(app)
                findNavController().popBackStack()
            }
        }
    }
    
    private fun showAppOptions(app: App) {
        adapter.showOptionsFor(app)
    }
    
    private fun toggleAppHidden(app: App) {
        viewModel.setAppHidden(app, !app.isHidden)
        if (mode == Mode.HIDDEN_APPS && app.isHidden) {
            // If we're in hidden apps and unhiding, the list will update
        }
    }
    
    private fun openAppInfo(app: App) {
        requireContext().openAppInfo(app.packageName, app.userHandle)
        findNavController().popBackStack(R.id.homeFragment, false)
    }
    
    private fun deleteApp(app: App) {
        if (requireContext().isSystemApp(app.packageName)) {
            requireContext().showToast(R.string.cannot_delete_system_app)
        } else {
            requireContext().uninstallApp(app.packageName)
        }
    }
    
    private fun renameApp(app: App, newName: String) {
        viewModel.setAppLabel(app, newName.takeIf { it.isNotBlank() })
    }
    
    private fun updateHint() {
        binding.searchEditText.hint = when (mode) {
            Mode.LAUNCH -> getString(R.string.search_apps)
            Mode.SELECT_HOME_APP -> getString(R.string.select_app)
            Mode.SELECT_SWIPE_LEFT -> getString(R.string.select_swipe_left_app)
            Mode.SELECT_SWIPE_RIGHT -> getString(R.string.select_swipe_right_app)
            Mode.SELECT_CLOCK_APP -> getString(R.string.select_clock_app)
            Mode.SELECT_CALENDAR_APP -> getString(R.string.select_calendar_app)
            Mode.HIDDEN_APPS -> getString(R.string.hidden_apps)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
