package com.isaiahvonrundstedt.fokus.features.event

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.isaiahvonrundstedt.fokus.R
import com.isaiahvonrundstedt.fokus.features.core.extensions.addStrikeThroughEffect
import com.isaiahvonrundstedt.fokus.features.shared.abstracts.BaseAdapter

class EventAdapter(private var actionListener: ActionListener)
    : BaseAdapter<EventResource, EventAdapter.EventViewHolder>(callback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val rowView: View = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_event,
            parent, false)
        return EventViewHolder(rowView, actionListener)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    override fun onSwipe(position: Int, direction: Int) {
        if (direction == ItemTouchHelper.START)
            actionListener.onActionPerformed(getItem(position), ActionListener.Action.DELETE,
                emptyMap())
    }

    class EventViewHolder(itemView: View, private val actionListener: ActionListener)
        : RecyclerView.ViewHolder(itemView) {

        private val rootView: FrameLayout = itemView.findViewById(R.id.rootView)
        private val tagView: ImageView = itemView.findViewById(R.id.tagView)
        private val locationView: TextView = itemView.findViewById(R.id.locationView)
        private val nameView: TextView = itemView.findViewById(R.id.nameView)
        private val dayView: TextView = itemView.findViewById(R.id.dayView)
        private val timeView: TextView = itemView.findViewById(R.id.timeView)

        fun onBind(resource: EventResource) {
            val id = resource.event.eventID
            nameView.transitionName = transitionEventName + id
            locationView.transitionName = transitionLocation + id

            with(resource.event) {
                locationView.text = location
                nameView.text = name
                dayView.text = formatScheduleDate(rootView.context)
                timeView.text = formatScheduleTime()

                if (schedule!!.isBeforeNow)
                    nameView.addStrikeThroughEffect()
            }

            resource.subject?.let {
                tagView.setImageDrawable(it.tintDrawable(tagView.drawable))
            }
            rootView.setOnClickListener {
                actionListener.onActionPerformed(resource, ActionListener.Action.SELECT,
                    mapOf(transitionEventName + id to nameView, transitionLocation + id to locationView))
            }
        }
    }

    companion object {
        const val transitionEventName = "transition:name:"
        const val transitionLocation = "transition:location:"

        val callback = object: DiffUtil.ItemCallback<EventResource>() {
            override fun areItemsTheSame(oldItem: EventResource, newItem: EventResource): Boolean {
                return oldItem.event.eventID == newItem.event.eventID
            }

            override fun areContentsTheSame(oldItem: EventResource, newItem: EventResource): Boolean {
                return oldItem == newItem
            }
        }
    }

}