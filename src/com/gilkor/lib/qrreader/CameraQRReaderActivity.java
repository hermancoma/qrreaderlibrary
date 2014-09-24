/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 * 
 * Created by lisah0 on 2012-02-24
 */
package com.gilkor.lib.qrreader;

import com.gilkor.lib.qrreaderlibrary.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.FrameLayout;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;

/* Import ZBar Class files */
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import net.sourceforge.zbar.Config;

public class CameraQRReaderActivity extends Activity {
	
	// ==================== QR Code
	public static final String EXTRA_QRCODE_RESULT = "EXTRA_QRCODE_RESULT";
	
	private Camera mCamera;
	private CameraPreview mPreview;
	private Handler autoFocusHandler;

	// TextView scanText;
	// Button scanButton;

	ImageScanner scanner;
	FrameLayout preview;

	private boolean barcodeScanned = false;
	private boolean previewing = true;
	private boolean isGilkorQRfound = false;

	static {
		try
		{
		    System.loadLibrary("iconv");
		}
		catch (UnsatisfiedLinkError e)
		{
			Log.e("QRReader Lib", "Cannot Load Lib for QR Reader");
		}
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.qrcodelayout);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		autoFocusHandler = new Handler();
		mCamera = getCameraInstance();

		/* Instance barcode scanner */
		scanner = new ImageScanner();
		scanner.setConfig(0, Config.X_DENSITY, 3);
		scanner.setConfig(0, Config.Y_DENSITY, 3);

		mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
		// mPreview.setLayoutParams(new LayoutParams(400, 400));
		preview = (FrameLayout) findViewById(R.id.cameraPreview);
		preview.addView(mPreview);

	}

	private void restartScanner() {
		if (barcodeScanned) {
			barcodeScanned = false;
			mCamera.setPreviewCallback(previewCb);
			mCamera.startPreview();
			previewing = true;
			mCamera.autoFocus(autoFocusCB);
		}
	}

	public void onPause() {
		super.onPause();
		releaseCamera();
		finish();
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
		}
		return c;
	}

	private void releaseCamera() {
		if (mCamera != null) {
			previewing = false;
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
			if (preview != null)
				preview.removeView(mPreview);
		}
	}

	private Runnable doAutoFocus = new Runnable() {
		public void run() {
			if (previewing)
				mCamera.autoFocus(autoFocusCB);
		}
	};

	PreviewCallback previewCb = new PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera camera) {
			Camera.Parameters parameters = camera.getParameters();
			Size size = parameters.getPreviewSize();

			Image barcode = new Image(size.width, size.height, "Y800");
			barcode.setData(data);

			int result = scanner.scanImage(barcode);

			if (result != 0) {
				String qrResult = "";
				SymbolSet syms = scanner.getResults();
				for (Symbol sym : syms) {
					if ((sym.getData().contains("gilkor.com")) 
							|| (sym.getData().contains("apps.id"))) {
						isGilkorQRfound = true;
						barcodeScanned = true;

						previewing = false;
						mCamera.setPreviewCallback(null);
						mCamera.stopPreview();

						qrResult = sym.getData();
						
					} else {

					}

				}

				if (isGilkorQRfound) {
					// TODO Return to application

					Intent resultIntent = new Intent();
					resultIntent.putExtra(EXTRA_QRCODE_RESULT, qrResult);
					setResult(RESULT_OK, resultIntent);
					finish();
				}
			}
		}
	};

	// Mimic continuous auto-focusing
	AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			autoFocusHandler.postDelayed(doAutoFocus, 1000);
		}
	};

}
