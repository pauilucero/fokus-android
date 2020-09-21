package com.isaiahvonrundstedt.fokus.features.task

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.isaiahvonrundstedt.fokus.R
import com.isaiahvonrundstedt.fokus.components.custom.ItemDecoration
import com.isaiahvonrundstedt.fokus.components.custom.ItemSwipeCallback
import com.isaiahvonrundstedt.fokus.components.extensions.android.createSnackbar
import com.isaiahvonrundstedt.fokus.components.extensions.android.getParcelableListExtra
import com.isaiahvonrundstedt.fokus.components.extensions.jdk.toArrayList
import com.isaiahvonrundstedt.fokus.components.utils.PreferenceManager
import com.isaiahvonrundstedt.fokus.features.attachments.Attachment
import com.isaiahvonrundstedt.fokus.features.shared.abstracts.BaseFragment
import com.isaiahvonrundstedt.fokus.features.shared.abstracts.BaseListAdapter
import com.isaiahvonrundstedt.fokus.features.task.editor.TaskEditor
import com.isaiahvonrundstedt.fokus.features.task.finished.FinishedTasksActivity
import kotlinx.android.synthetic.main.fragment_task.*
import kotlinx.android.synthetic.main.layout_empty_tasks.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import java.io.File

class TaskFragment : BaseFragment(), BaseListAdapter.ActionListener, TaskAdapter.TaskCompletionListener {

    private val adapter = TaskAdapter(this, this)

    private val viewModel: TaskViewModel by lazy {
        ViewModelProvider(this).get(TaskViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.addItemDecoration(ItemDecoration(requireContext()))
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(ItemSwipeCallback(requireContext(), adapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        viewModel.pendingTasks.observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.noPendingTasks.observe(viewLifecycleOwner) { emptyView.isVisible = it }
    }

    override fun onResume() {
        super.onResume()

        actionButton.setOnClickListener {
            startActivityForResult(Intent(context, TaskEditor::class.java),
                TaskEditor.REQUEST_CODE_INSERT)
        }
    }

    // Update the task in the database then show
    // snackbar feedback and also if the sounds if turned on
    // play a fokus sound.
    override fun onTaskCompleted(taskPackage: TaskPackage, isChecked: Boolean) {
        viewModel.update(taskPackage.task)
        if (isChecked) {
            createSnackbar(R.string.feedback_task_marked_as_finished, recyclerView)

            with(PreferenceManager(context)) {
                if (confetti) {
                    confettiView.build()
                        .addColors(Color.YELLOW, Color.MAGENTA, Color.CYAN)
                        .setDirection(0.0, 359.0)
                        .setSpeed(1f, 5f)
                        .setFadeOutEnabled(true)
                        .setTimeToLive(1000L)
                        .addShapes(Shape.Square, Shape.Circle)
                        .addSizes(Size(12, 5f))
                        .setPosition(confettiView.x + confettiView.width / 2,
                            confettiView.y + confettiView.height / 3)
                        .burst(100)
                }

                if (sounds)
                    RingtoneManager.getRingtone(requireContext(),
                        Uri.parse(PreferenceManager.DEFAULT_SOUND)).play()
            }
        }
    }

    // Callback from the RecyclerView Adapter
    override fun <T> onActionPerformed(t: T, action: BaseListAdapter.ActionListener.Action,
                                       views: Map<String, View>) {
        if (t is TaskPackage) {
            when (action) {
                // Create the intent to the editorUI and pass the extras
                // and wait for the result.
                BaseListAdapter.ActionListener.Action.SELECT -> {
                    val intent = Intent(context, TaskEditor::class.java).apply {
                        putExtra(TaskEditor.EXTRA_TASK, t.task)
                        putExtra(TaskEditor.EXTRA_SUBJECT, t.subject)
                        putExtra(TaskEditor.EXTRA_ATTACHMENTS, t.attachments.toArrayList())
                    }
                    startActivityWithTransition(views, intent, TaskEditor.REQUEST_CODE_UPDATE)
                }
                // The item has been swiped down from the recyclerView
                // remove the item from the database and show a snackbar
                // feedback
                BaseListAdapter.ActionListener.Action.DELETE -> {
                    viewModel.remove(t.task)

                    createSnackbar(R.string.feedback_task_removed, recyclerView).run {
                        addCallback(object: Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)

                                if (event != DISMISS_EVENT_ACTION)
                                    t.attachments.forEach { attachment ->
                                        attachment.target?.also { File(it).delete() }
                                    }
                            }
                        })
                        setAction(R.string.button_undo) {
                            viewModel.insert(t.task, t.attachments)
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK)
            return

        // Check the request code first if the data was from TaskEditor
        // so that it doesn't crash when casting the Parcelable object
        if (requestCode == TaskEditor.REQUEST_CODE_INSERT
            || requestCode == TaskEditor.REQUEST_CODE_UPDATE) {

            val task: Task? = data?.getParcelableExtra(TaskEditor.EXTRA_TASK)
            val attachments: List<Attachment>? = data?.getParcelableListExtra(TaskEditor.EXTRA_ATTACHMENTS)

            task?.also {
                when (requestCode) {
                    TaskEditor.REQUEST_CODE_INSERT ->
                        viewModel.insert(it, attachments ?: emptyList())
                    TaskEditor.REQUEST_CODE_UPDATE ->
                        viewModel.update(it, attachments ?: emptyList())
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_task, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_finished -> {
                startActivity(Intent(context, FinishedTasksActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
