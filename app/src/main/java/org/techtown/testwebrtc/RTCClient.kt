package org.techtown.testwebrtc

import android.app.Application
import com.google.gson.Gson
import org.techtown.testwebrtc.models.MessageModel
import org.webrtc.*

class RTCClient(
    private val application: Application,
    private val userName: String,
    private val socketRepository: SocketRepository,
    private val observer: PeerConnection.Observer,
) {
    init {
        initPeerConnectionFactory(application)
    }

    companion object {
        private const val TAG: String = "RTCClient"
    }

    private val gson by lazy { Gson() }
    private val eglBase by lazy { EglBase.create() }
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val peerConnection by lazy { createPeerConnection(observer) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private val videoCapture by lazy { getVideoCapture(application) }
    private val localVideoTrack by lazy {
        peerConnectionFactory.createVideoTrack("local_track", localVideoSource)
    }
    private val localAudioTrack by lazy {
        peerConnectionFactory.createAudioTrack("local_track_audio", localAudioSource)
    }
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer(),
    )

    private fun initPeerConnectionFactory(application: Application) {
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection {
        return peerConnectionFactory.createPeerConnection(iceServers, observer)!!
    }

    fun initializeSurfaceView(surface: SurfaceViewRenderer) {
        surface.release()
        surface.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglBase.eglBaseContext, null)
        }
    }

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglBase.eglBaseContext)
        videoCapture.initialize(surfaceTextureHelper, surface.context, localVideoSource.capturerObserver)
        videoCapture.startCapture(320, 240, 30)
        localVideoTrack.addSink(surface)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)
        peerConnection.addStream(localStream)
    }

    private fun getVideoCapture(application: Application): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }

    fun call(target: String) {
        AppData.error(TAG, "call called. target: $target")
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                AppData.debug(TAG, "peer createOffer onCreateSuccess ${gson.toJson(description)}")
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        AppData.debug(TAG, "peer createOffer local onCreateSuccess ${gson.toJson(p0)}")
                    }

                    override fun onSetSuccess() {
                        AppData.error(TAG, "peer createOffer local onSetSuccess")
                        val offer = hashMapOf(
                            "sdp" to description.description,
                            "type" to description.type
                        )
                        socketRepository.sendMessage(
                            MessageModel(
                                type = "create_offer",
                                name = userName,
                                target = target,
                                data = offer,
                            )
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                        AppData.debug(TAG, "peer createOffer local onCreateFailure ${gson.toJson(p0)}")
                    }

                    override fun onSetFailure(p0: String?) {
                        AppData.debug(TAG, "peer createOffer local onSetFailure ${gson.toJson(p0)}")
                    }
                }, description)
            }

            override fun onSetSuccess() {
                AppData.debug(TAG, "peer createOffer onSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                AppData.debug(TAG, "peer createOffer onCreateFailure ${gson.toJson(p0)}")
            }

            override fun onSetFailure(p0: String?) {
                AppData.debug(TAG, "peer createOffer onSetFailure ${gson.toJson(p0)}")
            }
        }, mediaConstraints)
    }

    fun onRemoteSessionReceived(session: SessionDescription) {
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                AppData.debug(TAG, "peer onCreateSuccess ${gson.toJson(p0)}")
            }

            override fun onSetSuccess() {
                AppData.debug(TAG, "peer onSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                AppData.debug(TAG, "peer onCreateFailure ${gson.toJson(p0)}")
            }

            override fun onSetFailure(p0: String?) {
                AppData.debug(TAG, "peer onSetFailure ${gson.toJson(p0)}")
            }
        }, session)
    }

    fun answer(target: String) {
        val constraints: MediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                AppData.debug(TAG, "peer createAnswer onCreateSuccess ${gson.toJson(description)}")
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        AppData.debug(TAG, "peer createAnswer local onCreateSuccess ${gson.toJson(description)}")
                    }

                    override fun onSetSuccess() {
                        AppData.debug(TAG, "peer createAnswer local onSetSuccess")
                        val answer = hashMapOf(
                            "sdp" to description.description,
                            "type" to description.type
                        )
                        socketRepository.sendMessage(
                            MessageModel(
                                type = "create_answer",
                                name = userName,
                                target = target,
                                data = answer,
                            )
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                        AppData.debug(TAG, "peer createAnswer local onCreateFailure ${gson.toJson(p0)}")
                    }

                    override fun onSetFailure(p0: String?) {
                        AppData.debug(TAG, "peer createAnswer local onSetFailure ${gson.toJson(p0)}")
                    }
                }, description)
            }

            override fun onSetSuccess() {
                AppData.debug(TAG, "peer createAnswer onSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                AppData.debug(TAG, "peer createAnswer onCreateFailure ${gson.toJson(p0)}")
            }

            override fun onSetFailure(p0: String?) {
                AppData.debug(TAG, "peer createAnswer onSetFailure ${gson.toJson(p0)}")
            }
        }, constraints)
    }

    fun addIceCandidate(ice: IceCandidate?) {
        AppData.debug(TAG, "addIceCandidate called. ice: ${gson.toJson(ice)}")
        peerConnection.addIceCandidate(ice)
    }

    fun switchCamera() {
        videoCapture.switchCamera(null)
    }

    fun toggleAudio(isMute: Boolean) {
        localAudioTrack.setEnabled(isMute)
    }

    fun toggleCamera(isCameraPause: Boolean) {
        localVideoTrack.setEnabled(isCameraPause)
    }

    fun endCall() {
        peerConnection.close()
        videoCapture.stopCapture()
        videoCapture.dispose()
    }
}
