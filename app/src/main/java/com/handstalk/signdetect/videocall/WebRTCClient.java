package com.handstalk.signdetect.videocall;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.handstalk.signdetect.network.IceServerRetrofitClient;
import com.handstalk.signdetect.network.RetrofitService;
import com.handstalk.signdetect.utilities.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CapturerObserver;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//https://github.com/codewithkael/JavaWebRTCYouTube
//https://blog.csdn.net/chzphoenix/article/details/121532032
//https://getstream.io/blog/webrtc-on-android/#conclusion
//https://getstream.io/blog/webrtc-jetpack-compose/
public class WebRTCClient {

    private Context context;
    private EglBase.Context eglBaseContext = EglBase.create().getEglBaseContext();
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private List<PeerConnection.IceServer> iceServer = new ArrayList<>();

    private CameraVideoCapturer videoCapturer;
    private VideoSource localVideoSource;
    private AudioSource localAudioSource;
    private String localTrackId = "local_track";
    private String localStreamId = "local_stream";
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private MediaStream localStream;
    private SurfaceViewRenderer localViewRenderer;
    private SurfaceViewRenderer remoteViewRenderer;

    private MediaConstraints mediaConstraints = new MediaConstraints();
    private static WebRTCClient Client;
    private static int ice_candidate_count = 0;
    private boolean isInitialized=false;
    private boolean isFrontCamera=false;
    private static List<IceCandidate> iceCandidatesQueue = new ArrayList<>();
    public WebRTCClient(){}

    public void initClient(Context context, PeerConnection.Observer observer){
        Client = this;
        this.context = context;
        if(isInitialized){
            Log.d("Log:","Client is already initialized, potential error may occur");
        }
        Log.d("Log:","Initializing Client");

        initPeerConnectionFactory();
        peerConnectionFactory = createPeerConnectionFactory();


        getIceServers((jsonString)->{
            try {
                JSONArray jsonArray = new JSONArray((String) jsonString);
                for(int i=0; i<jsonArray.length(); ++i){
                    JSONObject iceServerInfo = jsonArray.getJSONObject(i);
                    String url = iceServerInfo.getString("urls");
                    PeerConnection.IceServer.Builder builder = PeerConnection.IceServer.builder(url);
                    if(url.startsWith("turn")){
                        builder.setUsername(iceServerInfo.getString("username"));
                        builder.setPassword(iceServerInfo.getString("credential"));
                    }
                    iceServer.add(builder.createIceServer());
                }

                peerConnection = createPeerConnection(observer);
                localVideoSource = peerConnectionFactory.createVideoSource(false);
                localAudioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
                mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"));
                initLocalVideoStreaming();
                ice_candidate_count = 0;
                isInitialized = true;
                Client = this;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void getIceServers(SetSuccessCallback setSuccessCallback){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IceServerRetrofitClient.getClient().create(RetrofitService.class).fetchIceServer(Constants.ICE_SERVER_API_KEY).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                            if(response.isSuccessful()){
                                setSuccessCallback.onSetSuccess(response.body());
                            }else{
                                try {
                                    Log.d("Fail To get Ice Server",response.errorBody().string());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                            Log.d("Fail To get Ice Server",t.getMessage());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public static WebRTCClient getInstance(){
        // Singleton Class
        if(Client==null){
            Client = new WebRTCClient();
        }
        return Client;
    }

    private void initPeerConnectionFactory(){
        // FieldTrails: https://blog.csdn.net/epubcn/article/details/82690850
        PeerConnectionFactory.InitializationOptions options =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                        .setEnableInternalTracer(false).createInitializationOptions();
        PeerConnectionFactory.initialize(options);
    }

    private PeerConnectionFactory createPeerConnectionFactory(){
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableEncryption = false;
        options.disableNetworkMonitor = false;
        return PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBaseContext,true,true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBaseContext))
                .setOptions(options).createPeerConnectionFactory();
    }

    private PeerConnection createPeerConnection(PeerConnection.Observer observer){
        return peerConnectionFactory.createPeerConnection(iceServer, observer);
    }

    // Initialize UI
    private void initSurfaceViewRenderer(SurfaceViewRenderer viewRenderer){
        viewRenderer.setEnableHardwareScaler(true);
        viewRenderer.setMirror(true);
        viewRenderer.init(eglBaseContext,null);
    }

    private void initLocalVideoStreaming(){
        SurfaceTextureHelper helper = SurfaceTextureHelper.create(
                Thread.currentThread().getName(), eglBaseContext
        );

        videoCapturer = getVideoCapturer();
        CapturerObserver capturerObserver = TranslateCaptureObserver.getObserver(localVideoSource.getCapturerObserver());
        videoCapturer.initialize(helper, context, capturerObserver);

        localVideoTrack = peerConnectionFactory.createVideoTrack(
                localTrackId+"_video",localVideoSource
        );

        localAudioTrack = peerConnectionFactory.createAudioTrack(localTrackId+"_audio",localAudioSource);

        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId);
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(localAudioTrack);
        peerConnection.addStream(localStream);
    }

    public void startVideoCapture(SurfaceViewRenderer v){
        localVideoTrack.addSink(v);
        videoCapturer.startCapture(480,360,15);
    }

    private CameraVideoCapturer getVideoCapturer(){
        Camera2Enumerator enumerator = new Camera2Enumerator(context);

        String[] deviceNames = enumerator.getDeviceNames();

        for (String device: deviceNames) {
            if(enumerator.isFrontFacing(device)){
                return enumerator.createCapturer(device, null);
            }
        }
        throw new IllegalStateException("Front Camera not Found");
    }


    public void initLocalSurfaceView(SurfaceViewRenderer v){
        localViewRenderer = v;
        initSurfaceViewRenderer(v);
    }

    public void initRemoteSurfaceView(SurfaceViewRenderer v){
        remoteViewRenderer = v;
        initSurfaceViewRenderer(v);
    }

    
    // Negotiation (Call and Answer)
    public void createOffer(SetSuccessCallback setSuccessCallback){
        try{
            peerConnection.createOffer(new MySdpObserver(){
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription){
                    super.onCreateSuccess(sessionDescription);

                    peerConnection.setLocalDescription(new MySdpObserver(){
                        @Override
                        public void onSetSuccess(){
                            Log.d("Log:","Local Description Set");
                            super.onSetSuccess();
                            //Transfer This SDP to other peer
                            setSuccessCallback.onSetSuccess(sessionDescription);
                        }
                    },sessionDescription);

                }

                @Override
                public void onCreateFailure(String s) {
                    Log.d("Fail Creating Offer",s);
                }
            }, mediaConstraints);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void acceptOffer(SetSuccessCallback setSuccessCallback){
        try{
            Log.d("Accepted Call","");
            peerConnection.createAnswer(new MySdpObserver(){
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription){
                    super.onCreateSuccess(sessionDescription);
                    Log.d("Creating Answer","");
                    peerConnection.setLocalDescription(new MySdpObserver(){
                        @Override
                        public void onSetSuccess(){
                            super.onSetSuccess();
                            Log.d("Log:","Local Description Set");
                            //Transfer This SDP to other peer
                            setSuccessCallback.onSetSuccess(sessionDescription);
                        }
                    },sessionDescription);

                }

                @Override
                public void onCreateFailure(String s) {
                    Log.d("Fail Creating Answer",s);
                }
            }, mediaConstraints);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setRemoteSessionDescription(SessionDescription.Type type, String sd, SetSuccessCallback setSuccessCallback){
        SessionDescription sessionDescription = new SessionDescription(
                type,
                sd
        );
        peerConnection.setRemoteDescription(new MySdpObserver(){
            @Override
            public void onSetSuccess(){
                    setSuccessCallback.onSetSuccess(sessionDescription);
            }
        },sessionDescription);
    }

    public void addIceCandidateQueue(IceCandidate iceCandidate){
        // Add the Ice Candidates to queue
        iceCandidatesQueue.add(iceCandidate);
    }

    public void addIceCandidate(IceCandidate iceCandidate){
        Log.d("Adding Ice Candidate",String.valueOf(++ice_candidate_count));
        Log.d("Ice Candidate:",iceCandidate.serverUrl);
        peerConnection.addIceCandidate(iceCandidate);
    }

    public void handleIceCandidate(){
        // Add IceCandidates in Queue
        for(IceCandidate iceCandidate: iceCandidatesQueue){
            addIceCandidate(iceCandidate);
        }
        Log.d("Log:","Added Ice Candidate");
        Log.d("Ice candidate Queue Length:",String.valueOf(iceCandidatesQueue.size()));
        iceCandidatesQueue.clear();
    }

    public void switchCamera(){
        localVideoTrack.removeSink(localViewRenderer);
        if(isFrontCamera){
            // Set to Face
            isFrontCamera = false;
            localViewRenderer.setMirror(true);
        }else{
            isFrontCamera = true;
            localViewRenderer.setMirror(false);
        }

        localVideoTrack.addSink(localViewRenderer);
        videoCapturer.switchCamera(null);
    }

    public void toggleVideo(Boolean shouldBeMuted){
        localVideoTrack.setEnabled(shouldBeMuted);
    }

    public void toggleAudio(Boolean audioEnable){
        localAudioTrack.setEnabled(audioEnable);
    }

    public void closeConnection(){
        try{
            localVideoTrack.dispose();
            videoCapturer.stopCapture();
            videoCapturer.dispose();
            peerConnection.close();
            iceCandidatesQueue.clear();
            Client = null;
            Log.d("WebRTC Close","Connection Clear");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void state(){
        Log.d("State: Ice Candidate Count", String.valueOf(ice_candidate_count));
        Log.d("State: PeerConnection Connection State", peerConnection.connectionState().toString());
        Log.d("State: PeerConnection Signaling State", peerConnection.signalingState().toString());
        Log.d("State: Ice Connection State", peerConnection.iceConnectionState().toString());
        Log.d("State: Ice Gathering State", peerConnection.iceGatheringState().toString());
    }

    public int getIceCandidateQueueSize(){
        return iceCandidatesQueue.size();
    }

    public interface SetSuccessCallback{
        void onSetSuccess(Object sessionDescription);

    }

    public boolean getIsFrontCamera() { return isFrontCamera; }

    public boolean isInitialized(){
        return isInitialized;
    }
}
