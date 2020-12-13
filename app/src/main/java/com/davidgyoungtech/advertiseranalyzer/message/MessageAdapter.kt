package com.davidgyoungtech.advertiseranalyzer.message

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.davidgyoungtech.advertiseranalyzer.databinding.ItemMessageBinding

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

	private val messageMap = hashMapOf<String, MessageEvent>()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(
				ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun getItemCount(): Int = messageMap.keys.size

	private fun getItem(position: Int): MessageEvent {
		var index = 0
		var found: MessageEvent? = null
		for (k in messageMap.keys) {
			if (position == index) {
				found = messageMap[k]
				break
			}
			index++
		}
		return found ?: MessageEvent()
	}

	fun appendToList(uuid: String, messageEvent: MessageEvent) {
		messageMap[uuid] = messageEvent
		notifyDataSetChanged()
	}

	inner class ViewHolder(
			private val binding: ItemMessageBinding
	) : RecyclerView.ViewHolder(binding.root) {

		fun bind(messageEvent: MessageEvent) {
			binding.apply {
				messageText.text = messageEvent.message
			}
		}
	}
}