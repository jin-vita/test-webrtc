package org.techtown.testwebrtc.models

data class IceCandidateModel(
    val sdpMid: String,
    val sdpMLineIndex: Double,
    val sdpCandidate: String,
)