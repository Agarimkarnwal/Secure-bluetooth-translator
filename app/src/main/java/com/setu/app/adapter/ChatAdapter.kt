package com.setu.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.setu.app.R
import com.setu.app.model.ChatMessage

/**
 * RecyclerView adapter for the chat screen.
 * Displays SENT (orange bubble) and RECEIVED (dark bubble) messages.
 * Renders E2EE security badges and on-device NPU translation latency.
 */
class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isSent) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val view = inflater.inflate(R.layout.item_chat_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
        }
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDisplay: TextView = itemView.findViewById(R.id.tvDisplayText)
        private val tvOriginal: TextView = itemView.findViewById(R.id.tvOriginalText)
        private val tvMetaInfo: TextView = itemView.findViewById(R.id.tvMetaInfo)

        fun bind(message: ChatMessage) {
            tvDisplay.text = message.displayText
            if (message.isTranslated && message.originalText != message.displayText) {
                tvOriginal.visibility = View.VISIBLE
                tvOriginal.text = "Original: ${message.originalText}"
            } else {
                tvOriginal.visibility = View.GONE
            }

            val lockIcon = if (message.isEncrypted) "🔒 AES-256 E2EE" else "🔓 Plaintext"
            val perfText = when {
                message.isQuickReply -> "Instant (0ms)"
                message.latencyMs > 0 -> "Hexagon NPU: ${message.latencyMs}ms"
                else -> "On-Device ML"
            }
            tvMetaInfo.text = "$lockIcon • $perfText"
        }
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDisplay: TextView = itemView.findViewById(R.id.tvDisplayText)
        private val tvOriginal: TextView = itemView.findViewById(R.id.tvOriginalText)
        private val tvMetaInfo: TextView = itemView.findViewById(R.id.tvMetaInfo)

        fun bind(message: ChatMessage) {
            tvDisplay.text = message.displayText
            if (message.isTranslated && message.originalText != message.displayText) {
                tvOriginal.visibility = View.VISIBLE
                tvOriginal.text = "Original: ${message.originalText}"
            } else {
                tvOriginal.visibility = View.GONE
            }

            val lockIcon = if (message.isEncrypted) "🔒 AES-256 E2EE" else "🔓 Plaintext"
            val perfText = when {
                message.isQuickReply -> "Instant (0ms)"
                message.latencyMs > 0 -> "Hexagon NPU: ${message.latencyMs}ms"
                else -> "On-Device ML"
            }
            tvMetaInfo.text = "$lockIcon • $perfText"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.id == new.id
        override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
    }
}
