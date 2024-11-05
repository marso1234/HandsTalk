package com.handstalk.signdetect.videocall;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;

import com.handstalk.signdetect.translate.HolisticTrackClient;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;

import org.webrtc.CapturerObserver;
import org.webrtc.GlRectDrawer;
import org.webrtc.GlTextureFrameBuffer;
import org.webrtc.GlUtil;
import org.webrtc.RendererCommon;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrameDrawer;

import java.nio.ByteBuffer;

//Reference From https://blog.csdn.net/xiaowang_lj/article/details/127658087
public class TranslateCaptureObserver {

    public final static CapturerObserver getObserver(CapturerObserver target){
        CapturerObserver result = new CapturerObserver() {
            @Override
            public void onCapturerStarted(boolean b) {
                target.onCapturerStarted(b);
            }

            @Override
            public void onCapturerStopped() {
                target.onCapturerStopped();
            }

            @Override
            public void onFrameCaptured(VideoFrame videoFrame) {
                target.onFrameCaptured(videoFrame);
                Bitmap bitmap = convertBitmap(videoFrame);
                MPImage mpImage = new BitmapImageBuilder(bitmap).build();
                HolisticTrackClient.getInstance().detectMP(mpImage);
            }
        };
        return result;
    }



    private static Bitmap convertBitmap(VideoFrame frame){
        final Matrix drawMatrix = new Matrix();
        // Used for bitmap capturing.
        final GlTextureFrameBuffer bitmapTextureFramebuffer =
                new GlTextureFrameBuffer(GLES20.GL_RGBA);
        drawMatrix.reset();
        drawMatrix.preTranslate(0.5f, 0.5f);
        drawMatrix.preScale( 1f ,  -1f);
        if(!WebRTCClient.getInstance().getIsFrontCamera()){
            drawMatrix.preScale(-1f, 1f); // We want the output to be upside down for Bitmap.
        }
        drawMatrix.preTranslate(-0.5f, -0.5f);

        final int scaledWidth = (int) (1 * frame.getRotatedWidth());
        final int scaledHeight = (int) (1 * frame.getRotatedHeight());
        bitmapTextureFramebuffer.setSize(scaledWidth, scaledHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bitmapTextureFramebuffer.getFrameBufferId());
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, bitmapTextureFramebuffer.getTextureId(), 0);

        GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        VideoFrameDrawer frameDrawer = new VideoFrameDrawer();
        RendererCommon.GlDrawer drawer = new GlRectDrawer();

        frameDrawer.drawFrame(frame, drawer, drawMatrix, 0 /* viewportX */,
                0 /* viewportY */, scaledWidth, scaledHeight);
        frameDrawer.release();
        drawer.release();
        final ByteBuffer bitmapBuffer = ByteBuffer.allocateDirect(scaledWidth * scaledHeight * 4);
        GLES20.glViewport(0, 0, scaledWidth, scaledHeight);
        GLES20.glReadPixels(
                0, 0, scaledWidth, scaledHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.checkNoGLES2Error("EglRenderer.notifyCallbacks");

        final Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(bitmapBuffer);

        return bitmap;
    }
}
