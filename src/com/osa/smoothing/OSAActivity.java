package com.osa.smoothing;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

/**
 * 
 * @author mustafa
 * @since 2012-04-03
 * The class implements the activity of outsourcing application. The main.xml contains the 
 * user interface implementation where the user can choose where to smooth the images. 
 * Based on the user selection, the activity will call the camera application by intent to 
 * preview and capture the image. When the image is captured, it returns to the intent result, 
 * smoothed and then stored in the gallery.
 */
public class OSAActivity extends Activity {

	private static final int ACTION_TAKE_PHOTO_B = 1;

	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
	// image object
	private Bitmap mImageBitmap;
	// original image path
	private String mCurrentPhotoPath;
	// smoothed image path
	private String mSmoothedPhotoPath;
	// images prefix and suffix to distinguish from other images
	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";
	
	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

	/* 
	 * Photo album for this application
	 */
	private String getAlbumName() {
		return "album";
	}
	/*
	 * Get album directory
	 */
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {

			storageDir = mAlbumStorageDirFactory
					.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (!storageDir.mkdirs()) {
					if (!storageDir.exists()) {
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}

		} else {
			Log.v(getString(R.string.app_name),
					"External storage is not mounted READ/WRITE.");
		}

		return storageDir;
	}

	/*
	 * Create image object based on image path
	 */
	private File createImageFile(boolean isSmoothedImage) throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String imageFileName;
		if (!isSmoothedImage)
			imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		else
			imageFileName = JPEG_FILE_PREFIX + timeStamp + "_smoothed";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX,
				albumF);
		return imageF;
	}
	
	/*
	 * Create image object based on image source: original or smoothed
	 */
	private File setUpPhotoFile(boolean isSmoothedImage) throws IOException {

		File f = createImageFile(isSmoothedImage);
		if (isSmoothedImage)
			mSmoothedPhotoPath = f.getAbsolutePath();
		else
			mCurrentPhotoPath = f.getAbsolutePath();

		return f;
	}
	/*
	 * Add picture to gallery
	 */
	private void galleryAddPic(String path) {
		Intent mediaScanIntent = new Intent(
				"android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(path);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}	
	/*
	 * Dispatch the camera application to take pictures.
	 */
	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		switch (actionCode) {
		case ACTION_TAKE_PHOTO_B:
			File f = null;			
			try {
				f = setUpPhotoFile(false);
				mCurrentPhotoPath = f.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(f));
			} catch (IOException e) {
				e.printStackTrace();
				f = null;
				mCurrentPhotoPath = null;
			}
			break;

		default:
			break;
		} // switch

		startActivityForResult(takePictureIntent, actionCode);
	}

	/**
	 * Handle the image smoothing locally on the device. Returns true if the smoothing successed
	 * false if it fails
	 */
	private boolean handleImageLocally() {

		if (mCurrentPhotoPath != null) {
			/* Decode the JPEG file into a Bitmap */
			Bitmap src = BitmapFactory.decodeFile(mCurrentPhotoPath);
			if (src == null) {
				mCurrentPhotoPath = null;
				return false;
			}
			MedianFilter median = new MedianFilter();
			Bitmap dst = median.filter(src);
			OutputStream fOut = null;
			try {
				File file = setUpPhotoFile(true);
				fOut = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				mCurrentPhotoPath = null;
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				mCurrentPhotoPath = null;
				return false;
			}

			if (!dst.compress(Bitmap.CompressFormat.JPEG, 100, fOut))
				return false;
			// Add both original and smoothed pictures
			galleryAddPic(mCurrentPhotoPath);
			galleryAddPic(mSmoothedPhotoPath);
			mCurrentPhotoPath = null;
		}
		return true;

	}

	/*
	 * Do the smoothing on the server. 
	 */
	private boolean handleImageServer(String server_url) {
		if (mCurrentPhotoPath != null) {

			Bitmap resImage = serverSmoothing(mCurrentPhotoPath, server_url);

			OutputStream fOut = null;
			File file = null;
			try {
				file = setUpPhotoFile(true);
				fOut = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}

			if (!resImage.compress(Bitmap.CompressFormat.JPEG, 100, fOut))
				return false;
			galleryAddPic(mCurrentPhotoPath);
			galleryAddPic(mSmoothedPhotoPath);
		}
		return true;
	}

	RadioButton.OnClickListener optionCLickListener = new RadioButton.OnClickListener() {
		public void onClick(View v) {
			EditText edit_text = (EditText) findViewById(R.id.editText_server);
			RadioButton radioButon_server = (RadioButton) findViewById(R.id.radioButton_server);
			edit_text.setEnabled(radioButon_server.isChecked());
		}

	};
	// capturing image button listener
	Button.OnClickListener mTakePicOnClickListener = new Button.OnClickListener() {
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
			//onActivityResult(ACTION_TAKE_PHOTO_B, RESULT_OK, null);
		}
	};

	/** 
	 * Called when the activity is first created.
	 * 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mImageBitmap = null;

		EditText edit_text = (EditText) findViewById(R.id.editText_server);
		edit_text.setEnabled(false);
		RadioButton localRadio = (RadioButton) findViewById(R.id.radioButton_locally);
		RadioButton serverRadio = (RadioButton) findViewById(R.id.radioButton_server);
		localRadio.setOnClickListener(optionCLickListener);
		serverRadio.setOnClickListener(optionCLickListener);
		Button picBtn = (Button) findViewById(R.id.button1);
		setBtnListenerOrDisable(picBtn, mTakePicOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean status = true;
		long t1, t2;
		t1 = System.currentTimeMillis();
		switch (requestCode) {
		case ACTION_TAKE_PHOTO_B: {
			if (resultCode == RESULT_OK) {
			/*	mCurrentPhotoPath = "/mnt/sdcard/Pictures/album/test.jpg";
				mSmoothedPhotoPath = "/mnt/sdcard/Pictures/album/test_smoothed.jpg";
				*/
				RadioButton localRadio = (RadioButton) findViewById(R.id.radioButton_locally);
				RadioButton serverRadio = (RadioButton) findViewById(R.id.radioButton_server);

				if (localRadio.isChecked()) {
					status = handleImageLocally();
				} else if (serverRadio.isChecked()) {
					// get server address
					EditText edit_text = (EditText) findViewById(R.id.editText_server);
					if (edit_text.getText().equals("")) {
						Toast.makeText(OSAActivity.this,
								"server url is empty ", Toast.LENGTH_LONG)
								.show();
						return;
					}
					status = handleImageServer(edit_text.getText().toString());
				}

			}

			break;
		} // ACTION_TAKE_PHOTO_B
		} // switch
		t2 = System.currentTimeMillis();
		if (status) {
			Toast.makeText(OSAActivity.this, " Processing time: " + (t2 - t1),
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(OSAActivity.this, "Failed! ", Toast.LENGTH_LONG)
					.show();
		}
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY,
				(mImageBitmap != null));
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		/*
		 * mImageView.setImageBitmap(mImageBitmap); mImageView.setVisibility(
		 * savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
		 * ImageView.VISIBLE : ImageView.INVISIBLE );
		 */
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 * 
	 * @param context
	 *            The application's environment.
	 * @param action
	 *            The Intent action to check for availability.
	 * 
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private void setBtnListenerOrDisable(Button btn,
			Button.OnClickListener onClickListener, String intentName) {
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);
		} else {
			btn.setText("Cannot" + " " + btn.getText());
			btn.setClickable(false);
		}
	}

	// this should be implemented as asychnorized activity
	private Bitmap serverSmoothing(String... urls) {

		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;
		DataInputStream inputStream = null;
		// image_path | server_url
		String pathToOurFile = urls[0];
		String urlServer = urls[1];
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;

		try {
			FileInputStream fileInputStream = new FileInputStream(new File(
					pathToOurFile));

			URL url = new URL(urlServer);

			connection = (HttpURLConnection) url.openConnection();

			// Allow Inputs & Outputs
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			// Enable POST method
			connection.setRequestMethod("POST");

			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + boundary);
			outputStream = new DataOutputStream(connection.getOutputStream());
			outputStream.writeBytes(twoHyphens + boundary + lineEnd);
			outputStream
					.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
							+ pathToOurFile + "\"" + lineEnd);
			outputStream.writeBytes(lineEnd);

			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];

			// Read file
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);

			while (bytesRead > 0) {
				outputStream.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}

			outputStream.writeBytes(lineEnd);
			outputStream.writeBytes(twoHyphens + boundary + twoHyphens
					+ lineEnd);

			// Responses from the server (code and message)
			int serverResponseCode = connection.getResponseCode();
			String serverResponseMessage = connection.getResponseMessage();

			fileInputStream.close();
			outputStream.flush();
			outputStream.close();

			InputStream imageStream;
			Bitmap b;
			try {
				imageStream = (InputStream) connection.getContent();
				BitmapFactory.Options bpo = new BitmapFactory.Options();
				bpo.inSampleSize = 2;
				b = BitmapFactory.decodeStream(
						new PatchInputStream(imageStream), null, bpo);
				return b;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			finally {
				connection.disconnect();
			}
		} catch (Exception ex) {
			// Exception handling
			ex.printStackTrace();
		}
		return null;
	}

	public class PatchInputStream extends FilterInputStream {
		public PatchInputStream(InputStream in) {
			super(in);
		}

		public long skip(long n) throws IOException {
			long m = 0L;
			while (m < n) {
				long _m = in.skip(n - m);
				if (_m == 0L)
					break;
				m += _m;
			}
			return m;
		}
	}

}
