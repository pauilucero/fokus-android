package com.isaiahvonrundstedt.fokus.features.core

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.isaiahvonrundstedt.fokus.R
import com.isaiahvonrundstedt.fokus.features.shared.abstracts.BaseActivity
import com.isaiahvonrundstedt.fokus.features.shared.components.menu.NavigationBottomSheet
import com.isaiahvonrundstedt.fokus.features.task.TaskEditorActivity
import com.isaiahvonrundstedt.fokus.features.task.TaskFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_appbar.*

class MainActivity: BaseActivity() {

    private var controller: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setPersistentActionBar(toolbar)
        setToolbarTitle(R.string.activity_main)

        if (intent?.action == TaskFragment.action)
            startActivityForResult(Intent(this, TaskEditorActivity::class.java),
                TaskEditorActivity.insertRequestCode)

        toolbar?.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_android_menu)
        toolbar?.setNavigationOnClickListener {
            NavigationBottomSheet().invoke(supportFragmentManager)
        }

        val navigationHost = supportFragmentManager.findFragmentById(R.id.navigationHostFragment)
        controller = navigationHost?.findNavController()
        NavigationUI.setupWithNavController(navigationView, controller!!)
    }

    override fun onResume() {
        super.onResume()
        controller?.addOnDestinationChangedListener(navigationListener)
    }

    override fun onPause() {
        super.onPause()
        controller?.removeOnDestinationChangedListener(navigationListener)
    }

    private val navigationListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        when (destination.id) {
            R.id.navigation_tasks -> { setToolbarTitle(R.string.activity_main) }
            R.id.navigation_events -> { setToolbarTitle(R.string.activity_events) }
            R.id.navigation_subjects -> { setToolbarTitle(R.string.activity_subjects) }
        }
    }

    override fun onSupportNavigateUp(): Boolean = true
}