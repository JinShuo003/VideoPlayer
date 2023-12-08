package com.jinshuo.cvte.videoplayer;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    Button btnChooseVideo;
    Button btnPlayVideo;
    TextureView textureView;
    Surface surface;

    MediaCodec decoder;
    MediaFormat format;
    int displayWidth = 720;
    int displayHeight = 1280;

    Uri videoUri;
    BufferedInputStream inputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            Log.d(TAG, "onSurfaceTextureAvailable: ");
            surface = new Surface(surfaceTexture);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            Log.d(TAG, "onSurfaceTextureSizeChanged: ");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            Log.d(TAG, "onSurfaceTextureDestroyed: ");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    ActivityResultCallback<ActivityResult> chooseVideoCallback = new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult activityResult) {
            int resultCode = activityResult.getResultCode();
            if (resultCode == RESULT_OK) {
                videoUri = activityResult.getData().getData();
                try {
                    inputStream = new BufferedInputStream(getContentResolver().openInputStream(videoUri));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    ActivityResultLauncher launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), chooseVideoCallback);

    View.OnClickListener chooseVideoOnClickListener = v -> {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//筛选器
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(intent);
    };

    MediaCodec.Callback callback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            Log.d(TAG, "onInputBufferAvailable: ");
            ByteBuffer byteBuffer = codec.getInputBuffer(index);
            try {
                byte[] buffer = new byte[1024];
                int length = inputStream.read(buffer);
                if (length > 0) {
                    byteBuffer.put(buffer);
                    codec.queueInputBuffer(index, 0,
                            length, 0, 0);
                } else {
                    decoder.stop();
                    decoder.reset();
                    inputStream = new BufferedInputStream(getContentResolver().openInputStream(videoUri));
                    Log.d(TAG, "onInputBufferAvailable: ");
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            Log.d(TAG, "onOutputBufferAvailable: ");
            codec.releaseOutputBuffer(index, true);
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, "onError: ");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            Log.d(TAG, "onOutputFormatChanged: ");
        }
    };

    private void initDecoder() {
        if (decoder == null) {
            try {
                decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, displayWidth, displayHeight);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, displayWidth * displayHeight);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        decoder.setCallback(callback);
        decoder.configure(format, surface, null, 0);
    }

    View.OnClickListener playVideoOnClickListener = v -> {
        try {
            initDecoder();
            decoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    private void init() {
        btnChooseVideo = findViewById(R.id.choose_video);
        btnPlayVideo = findViewById(R.id.play_video);
        textureView = findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(surfaceTextureListener);

        btnChooseVideo.setOnClickListener(chooseVideoOnClickListener);
        btnPlayVideo.setOnClickListener(playVideoOnClickListener);
    }


}