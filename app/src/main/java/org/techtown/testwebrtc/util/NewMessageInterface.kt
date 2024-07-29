package org.techtown.testwebrtc.util

import org.techtown.testwebrtc.models.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)
}