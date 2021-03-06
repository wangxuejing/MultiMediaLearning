package com.richie.multimedialearning.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.utils.CameraUtils;
import com.richie.multimedialearning.utils.ThreadHelper;

import java.io.IOException;

/**
 * @author Richie on 2018.08.01
 * 使用 SurfaceView 预览
 */
public class CameraSurfacePreview extends SurfaceView implements SurfaceHolder.Callback {
    private final ILogger logger = LoggerFactory.getLogger(CameraSurfacePreview.class);
    private static final int MAX_PREVIEW_WIDTH = 1280;
    private static final int MAX_PREVIEW_HEIGHT = 720;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Activity mActivity;

    public CameraSurfacePreview(Context context) {
        super(context);
        init();
    }

    public CameraSurfacePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraSurfacePreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mActivity = (Activity) getContext();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        logger.info("surfaceCreated");
        ThreadHelper.getInstance().runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                // 异步打开相机，创建预览画面
                openCamera();
                startPreviewDisplay();
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        logger.info("surfaceChanged: format:{}, width:{}, height:{}", format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        logger.info("surfaceDestroyed");
        ThreadHelper.getInstance().runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                releaseCamera();
            }
        });
    }

    private void openCamera() {
        logger.debug("openCamera");
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int number = Camera.getNumberOfCameras();

        for (int i = 0; i < number; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    mCamera = Camera.open(i);
                    CameraUtils.setCameraDisplayOrientation(mActivity, i, mCamera);
                    Camera.Parameters params = mCamera.getParameters();
                    CameraUtils.choosePreviewSize(params, MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT);
                    mCamera.setParameters(params);
                    logger.info("Camera opened");
                } catch (Exception e) {
                    logger.error("openCamera error", e);
                }
                break;
            }
        }
    }

    private void startPreviewDisplay() {
        if (mCamera != null) {
            try {
                // 设置 NV21 数据回调
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        logger.verbose("onPreviewFrame data length:{}", data != null ? data.length : 0);
                    }
                });
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                logger.error("startPreviewDisplay error", e);
            }
        }
    }

    private void releaseCamera() {
        logger.debug("releaseCamera");
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewDisplay(null);
                mCamera.release();
            } catch (IOException e) {
                logger.error("releaseCamera error", e);
            }
            mCamera = null;
        }
    }

}
