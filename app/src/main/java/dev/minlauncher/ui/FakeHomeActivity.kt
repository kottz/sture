package dev.minlauncher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Dummy activity used for the launcher reset trick.
 * This is briefly enabled to trigger the launcher chooser dialog.
 */
class FakeHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
