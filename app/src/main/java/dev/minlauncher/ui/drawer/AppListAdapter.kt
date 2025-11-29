package dev.minlauncher.ui.drawer

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.minlauncher.R
import dev.minlauncher.domain.model.App
import dev.minlauncher.util.hideKeyboard
import dev.minlauncher.util.showKeyboard

class AppListAdapter(
    private val alignment: Int,
    private val isHiddenMode: Boolean,
    private val onAppClick: (App) -> Unit,
    private val onAppLongClick: (App) -> Unit,
    private val onHideClick: (App) -> Unit,
    private val onInfoClick: (App) -> Unit,
    private val onDeleteClick: (App) -> Unit,
    private val onRenameClick: (App, String) -> Unit
) : ListAdapter<App, AppListAdapter.AppViewHolder>(AppDiffCallback()) {
    
    private var expandedPosition: Int = RecyclerView.NO_POSITION
    private var renamePosition: Int = RecyclerView.NO_POSITION
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = getItem(position)
        holder.bind(app, position)
    }
    
    fun showOptionsFor(app: App) {
        val position = currentList.indexOf(app)
        if (position != RecyclerView.NO_POSITION) {
            val oldExpanded = expandedPosition
            expandedPosition = if (expandedPosition == position) RecyclerView.NO_POSITION else position
            renamePosition = RecyclerView.NO_POSITION
            
            if (oldExpanded != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldExpanded)
            }
            if (expandedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(expandedPosition)
            }
        }
    }
    
    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appLabel: TextView = itemView.findViewById(R.id.appLabel)
        private val newIndicator: View = itemView.findViewById(R.id.newIndicator)
        private val workIndicator: View = itemView.findViewById(R.id.workIndicator)
        private val optionsLayout: LinearLayout = itemView.findViewById(R.id.optionsLayout)
        private val renameLayout: LinearLayout = itemView.findViewById(R.id.renameLayout)
        private val renameEditText: EditText = itemView.findViewById(R.id.renameEditText)
        
        private val hideButton: TextView = itemView.findViewById(R.id.hideButton)
        private val infoButton: TextView = itemView.findViewById(R.id.infoButton)
        private val deleteButton: TextView = itemView.findViewById(R.id.deleteButton)
        private val renameButton: TextView = itemView.findViewById(R.id.renameButton)
        private val closeButton: TextView = itemView.findViewById(R.id.closeButton)
        
        private val saveRenameButton: TextView = itemView.findViewById(R.id.saveRenameButton)
        private val cancelRenameButton: TextView = itemView.findViewById(R.id.cancelRenameButton)
        
        fun bind(app: App, position: Int) {
            // Main label
            val displayText = buildString {
                append(app.displayLabel)
                if (app.isNew) append(" ✦")
            }
            appLabel.text = displayText
            appLabel.gravity = alignment
            
            // Indicators
            newIndicator.isVisible = app.isNew
            workIndicator.isVisible = app.userHandle != android.os.Process.myUserHandle()
            
            // Options visibility
            val isExpanded = position == expandedPosition
            val isRenaming = position == renamePosition
            
            appLabel.isVisible = !isExpanded && !isRenaming
            optionsLayout.isVisible = isExpanded && !isRenaming
            renameLayout.isVisible = isRenaming
            
            // Click handlers
            appLabel.setOnClickListener { onAppClick(app) }
            appLabel.setOnLongClickListener {
                onAppLongClick(app)
                true
            }
            
            // Options buttons
            hideButton.text = if (isHiddenMode || app.isHidden) {
                itemView.context.getString(R.string.show)
            } else {
                itemView.context.getString(R.string.hide)
            }
            hideButton.setOnClickListener {
                onHideClick(app)
                collapseOptions()
            }
            
            infoButton.setOnClickListener {
                onInfoClick(app)
                collapseOptions()
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(app)
                collapseOptions()
            }
            
            renameButton.setOnClickListener {
                expandedPosition = RecyclerView.NO_POSITION
                renamePosition = position
                notifyItemChanged(position)
                renameEditText.setText(app.displayLabel)
                renameEditText.selectAll()
                renameEditText.showKeyboard()
            }
            
            closeButton.setOnClickListener {
                collapseOptions()
            }
            
            // Rename handlers
            renameEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveRename(app)
                    true
                } else false
            }
            
            saveRenameButton.setOnClickListener {
                saveRename(app)
            }
            
            cancelRenameButton.setOnClickListener {
                renameEditText.hideKeyboard()
                renamePosition = RecyclerView.NO_POSITION
                notifyItemChanged(position)
            }
        }
        
        private fun saveRename(app: App) {
            val newName = renameEditText.text.toString().trim()
            renameEditText.hideKeyboard()
            onRenameClick(app, newName)
            renamePosition = RecyclerView.NO_POSITION
            val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
            notifyItemChanged(position)
        }
        
        private fun collapseOptions() {
            val oldPosition = expandedPosition
            expandedPosition = RecyclerView.NO_POSITION
            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition)
            }
        }
    }
    
    class AppDiffCallback : DiffUtil.ItemCallback<App>() {
        override fun areItemsTheSame(oldItem: App, newItem: App): Boolean {
            return oldItem.uniqueId == newItem.uniqueId
        }
        
        override fun areContentsTheSame(oldItem: App, newItem: App): Boolean {
            return oldItem == newItem
        }
    }
}
