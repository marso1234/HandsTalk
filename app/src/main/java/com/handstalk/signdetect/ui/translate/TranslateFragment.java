package com.handstalk.signdetect.ui.translate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.handstalk.signdetect.R;
import com.handstalk.signdetect.databinding.FragmentTranslateBinding;
import com.handstalk.signdetect.sentenceCompletion.DetectionBuffer;
import com.handstalk.signdetect.sentenceCompletion.SentenceCompletionClient;
import com.handstalk.signdetect.translate.HolisticTrackClient;
import com.handstalk.signdetect.translate.ResultBundle;
import com.handstalk.signdetect.translate.SignDetectClient;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.permissionx.guolindev.PermissionX;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TranslateFragment extends Fragment implements HolisticTrackClient.LandmarkerListener,
        SentenceCompletionClient.SentenceCompletionCallback ,DetectionBuffer.DetectionCallback {

    private FragmentTranslateBinding binding;
    private ExecutorService backgroundExecutor;
    private HolisticTrackClient holisticTrackClient;
    private ProcessCameraProvider cameraProvider;
    private SignDetectClient signDetectClient;
    private Camera camera;
    private int cameraFacing = CameraSelector.LENS_FACING_FRONT;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTranslateBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.setStartTranslating(false);

        signDetectClient = SignDetectClient.getInstance(this.getContext());

        DetectionBuffer.getInstance().subscribe(this);
        SentenceCompletionClient.getInstance().subscribe(this);
        SentenceCompletionClient.getInstance().resetValues();
        askCameraPermission();

        return root;
    }

    private void askCameraPermission(){
        PermissionX.init((FragmentActivity) getContext())
                .permissions(Manifest.permission.CAMERA)
                .request((allGranted, grantedList, deniedList)->{
                    if (allGranted) {
                        Log.d("Log:","Permission Granted");
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage("You must accept camera permission to use the translation function")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User clicked OK button
                                        askCameraPermission();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                    }
                                });
                        // Show Dialog
                        builder.create().show();
                    }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor();

        // Set up the camera and its use cases
        binding.viewFinder.post(()->{
            setUpCamera();
        });


        // Create the HandLandmarkerHelper that will handle the inference
        backgroundExecutor.execute(()->{
            holisticTrackClient = HolisticTrackClient.getInstance();
            holisticTrackClient.initializeClient(getContext(), this);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        DetectionBuffer.getInstance().unsubscribe(this);
        SentenceCompletionClient.getInstance().unsubscribe(this);
    }

    private void setUpCamera(){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(
                ()->{
                        // CameraProvider
                    try {
                        cameraProvider = cameraProviderFuture.get();
                        camera.getCameraInfo();
                    } catch (Exception e) {
                        Log.d("Error","Camera Provider");
                        e.printStackTrace();
                    }

                    // Build and bind the camera use cases
                    bindCameraUseCases();
                }, ContextCompat.getMainExecutor(requireContext())
        );
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {

        if(cameraProvider==null){
            throw new IllegalStateException("Camera initialization failed.");
        }

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        Preview preview = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.viewFinder.getDisplay().getRotation())
                .build();

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        ImageAnalysis imageAnalyzer =
                new ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(binding.viewFinder.getDisplay().getRotation())
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

            // The analyzer can then be assigned to the instance
        imageAnalyzer.setAnalyzer(backgroundExecutor, this::detectHolistic);

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
            );

            // Attach the viewfinder's surface provider to preview use case
            preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
        } catch (Exception e) {
            Log.e("Error", "Use case binding failed", e);
        }
    }

    private void detectHolistic(ImageProxy imageProxy) {
        holisticTrackClient.detectLiveStream(
                imageProxy,
                cameraFacing == CameraSelector.LENS_FACING_FRONT
        );
    }

    @Override
    public void onResults(ResultBundle resultBundle) {
        requireActivity().runOnUiThread(()->{
                if (binding != null) {
                    if (resultBundle.getTypeLandmark().equals(ResultBundle.TYPE_LANDMARK.hand)){
                        binding.overlay.setHandResults(
                                (HandLandmarkerResult) resultBundle.getResults(),
                                resultBundle.getInputImageHeight(),
                                resultBundle.getInputImageWidth());
                        signDetectClient.addHandLandmarks((HandLandmarkerResult) resultBundle.getResults());
                        setLabel(signDetectClient.getPredictedLabel(), signDetectClient.getPredictedProb());
                    } else if(resultBundle.getTypeLandmark().equals(ResultBundle.TYPE_LANDMARK.pose)){
                        binding.overlay.setPoseResult(
                                (PoseLandmarkerResult) resultBundle.getResults(),
                                resultBundle.getInputImageHeight(),
                                resultBundle.getInputImageWidth());
                        signDetectClient.addPoseLandmarks((PoseLandmarkerResult) resultBundle.getResults());
                    }

                    // Force a redraw
                    binding.overlay.invalidate();
                }
        });
    }

    @Override
    public void onError(String error) {
        getActivity().runOnUiThread(()->{
            {
                binding.overlay.clear();
            }
        });
    }

    private void setLabel(String label, float prediction){
        if(prediction!=0f){
            if(SentenceCompletionClient.getInstance().getToggle() || label.equals(SentenceCompletionClient.toggleSymbol)){
                binding.labelPredict.setText(label);
                String displayTxt = String.valueOf(Math.round(prediction*10000)/100)+"%";
                binding.probPredict.setText(displayTxt);
            }else{
                binding.labelPredict.setText("-");
                binding.probPredict.setText("");
                binding.labelContainer.setBackground(getResources().getDrawable(R.color.colorTransparentBox));
            }
        }
    }

    @Override
    public void onNewWordsAdded(String sentence) {
        if(sentence.equals(SentenceCompletionClient.toggleSymbol) && !SentenceCompletionClient.getInstance().isTranslating()) {
            if(SentenceCompletionClient.getInstance().getToggle()){
                if(!SentenceCompletionClient.getInstance().getLastInputIsDelete()){
                    Toast.makeText(getContext(), "Start Input", Toast.LENGTH_SHORT).show();
                }
                if(binding!=null) {
                    binding.setStartTranslating(true);
                    binding.mySentence.setText("");
                }
            }else{
                Toast.makeText(getContext(), "Stop Input", Toast.LENGTH_SHORT).show();
                if(binding!=null) {
                    binding.setStartTranslating(false);
                    binding.mySentence.setText("");
                }
            }
            return;
        }
        sentence = SentenceCompletionClient.getInstance().getRaw();
        if(binding != null){
            binding.setStartTranslating(true);
            binding.mySentence.setText(sentence);
        }
    }

    @Override
    public void onWordConfirmed() {
        if(binding != null){
            if(SentenceCompletionClient.getInstance().getToggle() || binding.labelPredict.getText().toString().equals(SentenceCompletionClient.toggleSymbol)){
                binding.labelContainer.setBackground(getResources().getDrawable(R.color.colorTransparentYellow));
            }
        }
    }

    @Override
    public void onWordUnconfirmed() {
        if(binding != null){
            if(SentenceCompletionClient.getInstance().getToggle() || binding.labelPredict.getText().toString().equals(SentenceCompletionClient.toggleSymbol)){
                binding.labelContainer.setBackground(getResources().getDrawable(R.color.colorTransparentBox));
            }
        }
    }

    @Override
    public void onResultReceived(String result) {
        if(binding != null){
            binding.mySentence.setText(result);
        }
    }
}