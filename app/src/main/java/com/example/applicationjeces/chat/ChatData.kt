package com.example.applicationjeces.chat

import com.google.firebase.Timestamp

data class ChatData(
    val chatroomidx: String,
    val content: String,
    val myid: String,
    val time: Timestamp
    )
