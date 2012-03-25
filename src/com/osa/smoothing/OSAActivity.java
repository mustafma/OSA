package com.osa.smoothing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;


public class OSAActivity extends Activity {

	private static final int ACTION_TAKE_PHOTO_B = 1;
	
	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
	private ImageView mImageView;
	private RadioGroup mRadioGroupMode;
	private Bitmap mImageBitmap;

	private String mCurrentPhotoPath;

	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

	
	/* Photo album for this application */
	private String getAlbumName() {
		return "album";
	}

	
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
			
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}

	private File setUpPhotoFile() throws IOException {
		
		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();
		
		return f;
	}

	private boolean setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
	/*	int targetW = mImageView.getWidth();
		int targetH = mImageView.getHeight();
*/
		/* Get the size of the image */
/*		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;
	*/	
		/* Figure out which way needs to be reduced less */
/*		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
		}
*/
		/* Set bitmap options to scale the image decode target */
	/*	bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;
*/
		/* Decode the JPEG file into a Bitmap */
		Bitmap src = BitmapFactory.decodeFile(mCurrentPhotoPath);
		if(src == null)
			return false;
		MedianFilter median =new MedianFilter();
		Bitmap dst = median.filter(src);
		OutputStream fOut = null;
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File file = new File(getAlbumDir().getPath(), 
				 JPEG_FILE_PREFIX + timeStamp + "_smoothed" + JPEG_FILE_SUFFIX);
		try {
			fOut = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		if(!dst.compress(Bitmap.CompressFormat.JPEG, 100, fOut))
			return false;
		return true;
		/* Associate the Bitmap to the ImageView */
//		mImageView.setImageBitmap(bitmap);
	//	mImageView.setVisibility(View.VISIBLE);
	}

	private void galleryAddPic() {
		    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			File f = new File(mCurrentPhotoPath);
		    Uri contentUri = Uri.fromFile(f);
		    mediaScanIntent.setData(contentUri);
		    this.sendBroadcast(mediaScanIntent);
	}

	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch(actionCode) {
		case ACTION_TAKE_PHOTO_B:
			File f = null;
			
			try {
				f = setUpPhotoFile();
				mCurrentPhotoPath = f.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
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
	 * 
	 */
	private boolean handleImageLocally() {

		if (mCurrentPhotoPath != null) {
			if(!setPic()) {
				mCurrentPhotoPath = null;
				return false;
			}
			galleryAddPic();
			mCurrentPhotoPath = null;
		
		}
		return true;

	}
	
	/**
	 * 
	 */
	private boolean handleImageServer() {
		return false;
		
	}
	

	 RadioButton.OnClickListener optionCLickListener =
			 new RadioButton.OnClickListener(){
			  public void onClick(View v) {
				  EditText edit_text = (EditText)findViewById(R.id.editText_server);
				  RadioButton radioButon_server = (RadioButton)findViewById(R.id.radioButton_server);
				  edit_text.setEnabled(radioButon_server.isChecked());
			  }	  
			  
	 };	
	Button.OnClickListener mTakePicOnClickListener = 
		new Button.OnClickListener() {
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mImageView = (ImageView) findViewById(R.id.imageView1);
		mImageBitmap = null;
		 
		EditText edit_text = (EditText)findViewById(R.id.editText_server);
		edit_text.setEnabled(false);
		RadioButton localRadio = (RadioButton) findViewById(R.id.radioButton_locally);
		RadioButton serverRadio = (RadioButton) findViewById(R.id.radioButton_server);
		localRadio.setOnClickListener(optionCLickListener);
		serverRadio.setOnClickListener(optionCLickListener);
		Button picBtn = (Button) findViewById(R.id.button1);
		setBtnListenerOrDisable( 
				picBtn, 
				mTakePicOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean status = true;
		switch (requestCode) {
		case ACTION_TAKE_PHOTO_B: {
			if (resultCode == RESULT_OK) {
				RadioButton localRadio = (RadioButton) findViewById(R.id.radioButton_locally);
				RadioButton serverRadio = (RadioButton) findViewById(R.id.radioButton_server);
				
				if(localRadio.isChecked()) {
					status = handleImageLocally();
				}
				else if(serverRadio.isChecked()){
					// get server address 
					EditText edit_text = (EditText)findViewById(R.id.editText_server);
					if(edit_text.getText().equals("")) {
						Toast.makeText(OSAActivity.this,"server url is empty ",
							     Toast.LENGTH_LONG).show();
						return;
					}
					status = handleImageServer();
				}
				
				
			}

			break;
		} // ACTION_TAKE_PHOTO_B
		} // switch
		if(status){
		Toast.makeText(OSAActivity.this,"Image smoothed successfully! ",
			     Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(OSAActivity.this,"Failed! ",
				     Toast.LENGTH_LONG).show();			
		}
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null) );
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
	/*	mImageView.setImageBitmap(mImageBitmap);
		mImageView.setVisibility(
				savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ? 
						ImageView.VISIBLE : ImageView.INVISIBLE
		);*/
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	

	private void setBtnListenerOrDisable( 
			Button btn, 
			Button.OnClickListener onClickListener,
			String intentName
	) {
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);        	
		} else {
			btn.setText( 
				"Cannot" + " " + btn.getText());
			btn.setClickable(false);
		}
	}

}

class UploadDownloadSmoothed extends AsyncTask<String, Void, Bitmap> {
    protected Bitmap doInBackground(String... urls) {
    	
        return null;
    }


    protected void onPostExecute(Bitmap result) {
    }
}