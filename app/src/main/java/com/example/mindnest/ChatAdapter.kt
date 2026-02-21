package com.example.mindnest.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.ItemChatBotBinding
import com.example.mindnest.databinding.ItemChatUserBinding

class ChatAdapter(private var messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_USER = 1
    private val TYPE_BOT = 2

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TYPE_USER) {
            val binding = ItemChatUserBinding.inflate(inflater, parent, false)
            UserVH(binding)
        } else {
            val binding = ItemChatBotBinding.inflate(inflater, parent, false)
            BotVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is UserVH) holder.bind(message)
        if (holder is BotVH) holder.bind(message)
    }

    override fun getItemCount() = messages.size

    class UserVH(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.tvUserMessage.text = msg.text
        }
    }

    class BotVH(private val binding: ItemChatBotBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.tvBotMessage.text = msg.text
        }
    }
}
