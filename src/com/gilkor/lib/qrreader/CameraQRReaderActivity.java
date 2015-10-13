/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 * 
 * Created by lisah0 on 2012-02-24
 */
package com.gilkor.lib.qrreader;

import com.gilkor.lib.qrcodelibrary.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
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
	public static final String EXTRA_QRCODE_IMAGE = "EXTRA_QRCODE_IMAGE";
	public static final String USER_EMAIL	 = "USER_EMAIL";
	public static final String USER_NAME = "USER_NAME";
	
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

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.qrcodelayout);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Intent requestIntent = this.getIntent();
		String userEmail = requestIntent.getStringExtra(USER_EMAIL);
		String userName = requestIntent.getStringExtra(USER_NAME);
		
		autoFocusHandler = new Handler();
		mCamera = getCameraInstance();
		
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;

		/* Instance barcode scanner */
		scanner = new ImageScanner();
		scanner.setConfig(0, Config.X_DENSITY, 3);
		scanner.setConfig(0, Config.Y_DENSITY, 3);

		mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
		preview = (FrameLayout) findViewById(R.id.cameraPreview);
		preview.addView(mPreview);
		
		TextView userNameTxt = (TextView) findViewById(R.id.userNameText);
		userNameTxt.setText(userName);
		
		View cancelButton = findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		
		
		// QRCode image
		
		ImageView imageContainer = (ImageView) findViewById(R.id.user_qr);
		
		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(
				"gilkor/tradexpo/users/" + userEmail, null,
				Contents.Type.TEXT,
				BarcodeFormat.QR_CODE.toString(), 1000);
		try {
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
    		imageContainer.setImageDrawable(new BitmapDrawable(bitmap));

		} catch (WriterException e) {
			e.printStackTrace();
		}
		
		

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
					if (sym.getData().contains("")){
						isGilkorQRfound = true;
						barcodeScanned = true;

						previewing = false;
						mCamera.setPreviewCallback(null);
						mPreview.buildDrawingCache();

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
