/**
 * Copyright 2015 Codename One
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.codename1.demos.cordova.plugins.camera;

import ca.weblite.codename1.json.JSONArray;
import ca.weblite.codename1.json.JSONException;
import com.codename1.capture.Capture;
import com.codename1.cordova.CordovaPlugin;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.Base64;
import com.codename1.util.Callback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shannah
 */
public class CameraPlugin implements CordovaPlugin {
    
    private static final int DATA_URL = 0;              // Return base64 encoded string
    private static final int FILE_URI = 1;              // Return file uri (content://media/external/images/media/2 for Android)
    private static final int NATIVE_URI = 2;                    // On Android, this is the same as FILE_URI

    private static final int PHOTOLIBRARY = 0;          // Choose image from picture library (same as SAVEDPHOTOALBUM for Android)
    private static final int CAMERA = 1;                // Take picture from camera
    private static final int SAVEDPHOTOALBUM = 2;       // Choose image from picture library (same as PHOTOLIBRARY for Android)

    private static final int PICTURE = 0;               // allow selection of still pictures only. DEFAULT. Will return format specified via DestinationType
    private static final int VIDEO = 1;                 // allow selection of video only, ONLY RETURNS URL
    private static final int ALLMEDIA = 2;              // allow selection from all media types

    private static final int JPEG = 0;                  // Take a picture of type JPEG
    private static final int PNG = 1;                   // Take a picture of type PNG
    private static final String GET_PICTURE = "Get Picture";
    private static final String GET_VIDEO = "Get Video";
    private static final String GET_All = "Get All";
    
    private static final String LOG_TAG = "CameraLauncher";

    //Where did this come from?
    private static final int CROP_CAMERA = 100;

    private int mQuality;                   // Compression quality hint (0-100: 0=low quality & high compression, 100=compress of max quality)
    private int targetWidth;                // desired width of the image
    private int targetHeight;               // desired height of the image
    private String imageUri;                   // Uri of captured image
    private int encodingType;               // Type of encoding to use
    private int mediaType;                  // What type of media to retrieve
    private boolean saveToPhotoAlbum;       // Should the picture be saved to the device's photo album
    private boolean correctOrientation;     // Should the pictures orientation be corrected
    private boolean orientationCorrected;   // Has the picture's orientation been corrected
    private boolean allowEdit;              // Should we allow the user to crop the image.

    public Callback callbackContext;
    private int numPics;

    //private MediaScannerConnection conn;    // Used to update gallery app with newly-written files
    private String scanMe;                     // Uri of image to be added to content store
    private String croppedUri;

    
    
    
    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  A PluginResult object with a status and message.
     */
    public boolean execute(String action, List argsList, Callback callbackContext) {
        JSONArray args = new JSONArray(argsList);
        //System.out.println("In camera plugin execute");
        this.callbackContext = callbackContext;

        if (action.equals("takePicture")) {
             try {
                 int srcType = CAMERA;
                 int destType = FILE_URI;
                 this.saveToPhotoAlbum = false;
                 this.targetHeight = 0;
                 this.targetWidth = 0;
                 this.encodingType = JPEG;
                 this.mediaType = PICTURE;
                 this.mQuality = 80;
                 
                 this.mQuality = args.getInt(0);
                 destType = (int)args.getInt(1);
                 srcType = (int)args.getInt(2);
                 this.targetWidth = (int)args.getInt(3);
                 this.targetHeight = (int)args.getInt(4);
                 this.encodingType = (int)args.getInt(5);
                 this.mediaType = (int)args.getInt(6);
                 this.allowEdit = (boolean)args.getBoolean(7);
                 this.correctOrientation = (boolean)args.getBoolean(8);
                 this.saveToPhotoAlbum = (boolean)args.getBoolean(9);
                 
                 // If the user specifies a 0 or smaller width/height
                 // make it -1 so later comparisons succeed
                 if (this.targetWidth < 1) {
                     this.targetWidth = -1;
                 }
                 if (this.targetHeight < 1) {
                     this.targetHeight = -1;
                 }
                 
                 try {
                     if (srcType == CAMERA) {
                         //System.out.println("Taking picture");
                         this.takePicture(destType, encodingType);
                     }
                     else if ((srcType == PHOTOLIBRARY) || (srcType == SAVEDPHOTOALBUM)) {
                         this.getImage(srcType, destType, encodingType);
                     }
                 }
                 catch (IllegalArgumentException e)
                 {
                     callbackContext.onError(null, null, 0, "Illegal Argument Exception");
                     //PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                     //callbackContext.sendPluginResult(r);
                     return true;
                 }
                 
                 
                 return true;
             }
            catch (JSONException ex)
            {
                callbackContext.onError(this, ex, 0, ex.getMessage());
                return true;
            }
        }
        return false;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

   

    /**
     * Take a picture with the camera.
     * When an image is captured or the camera view is cancelled, the result is returned
     * in CordovaActivity.onActivityResult, which forwards the result to this.onActivityResult.
     *
     * The image can either be returned as a base64 string or a URI that points to the file.
     * To display base64 string in an img tag, set the source to:
     *      img.src="data:image/jpeg;base64,"+result;
     * or to display URI in an img tag
     *      img.src=result;
     *
     * @param quality           Compression quality hint (0-100: 0=low quality & high compression, 100=compress of max quality)
     * @param returnType        Set the type of image to return.
     */
    public void takePicture(int returnType, int encodingType) {
        String path = Capture.capturePhoto(this.targetWidth, this.targetHeight);
        if (path == null) {
            callbackContext.onSucess(null);
            return;
        }
        switch (returnType) {
            case FILE_URI:
            case NATIVE_URI:
                //System.out.println("Returning URI");
                callbackContext.onSucess(path);
                return;
            case DATA_URL: {
                //System.out.println("Returning data url");
                try {
                    byte[] out = Util.readInputStream(FileSystemStorage.getInstance().openInputStream(path));
                    callbackContext.onSucess(Base64.encode(out));
                    return;
                } catch (IOException ex) {
                    Log.e(ex);
                    callbackContext.onError(this, ex, 0, ex.getMessage());
                    return;
                }
            }
            default:
                callbackContext.onError(this, null, 0, "Invalid return type "+returnType);
                
        }
    }



    /**
     * Get image from photo library.
     *
     * @param quality           Compression quality hint (0-100: 0=low quality & high compression, 100=compress of max quality)
     * @param srcType           The album to get image from.
     * @param returnType        Set the type of image to return.
     * @param encodingType 
     */
    // TODO: Images selected from SDCARD don't display correctly, but from CAMERA ALBUM do!
    // TODO: Images from kitkat filechooser not going into crop function
    public void getImage(int srcType, final int returnType, int encodingType) {
        
        int galleryType = 0;
        switch (mediaType) {
            case PICTURE:
                galleryType = Display.GALLERY_IMAGE;
                break;
            case VIDEO:
                galleryType = Display.GALLERY_VIDEO;
                break;
                
            default:
                galleryType = Display.GALLERY_ALL;
                        
        }
        
        Display.getInstance().openGallery(new ActionListener(){
            public void actionPerformed(ActionEvent evt) {
           
                FileSystemStorage fs = FileSystemStorage.getInstance();
                String fsRoot = fs.getAppHomePath();
                String tmp = fsRoot + fs.getFileSystemSeparator() + "tmp";
                if (!fs.exists(tmp)) {
                    fs.mkdir(tmp);
                }

                String path = (String)evt.getSource();
                if (path == null) {
                    callbackContext.onSucess(path);
                    return;
                }


                if (targetWidth > 0 || targetHeight > 0) {
                    int w = targetWidth > 0 ? targetWidth : -1;
                    int h = targetHeight > 0 ? targetHeight : -1;
                    String name = path.substring(path.lastIndexOf(fs.getFileSystemSeparator())+1);
                    String newPath = tmp + fs.getFileSystemSeparator() + System.currentTimeMillis() + name;

                    try {
                        ImageIO.getImageIO().save(path, fs.openOutputStream(newPath), ImageIO.FORMAT_JPEG, w, h, mQuality == 0 ? ((float)mQuality)/100f : 0.7f);
                        path = newPath;
                    } catch (IOException ex) {
                        callbackContext.onError(this, ex, 0, ex.getMessage());
                        return;
                    }

                }

                switch (returnType) {
                    case FILE_URI:
                    case NATIVE_URI:
                        callbackContext.onSucess(path);
                        break;
                    case DATA_URL: {
                        try {
                            byte[] out = Util.readInputStream(FileSystemStorage.getInstance().openInputStream(path));
                            callbackContext.onSucess(Base64.encode(out));
                            return;
                        } catch (IOException ex) {
                            Log.e(ex);
                            callbackContext.onError(this, ex, 0, ex.getMessage());
                            return;
                        }
                    }
                    default:
                        callbackContext.onError(this, null, 0, "Invalid return type "+returnType);


                }
            }
            
            
        }, galleryType);
        
    }

    public boolean execute(String action, String jsonArgs, Callback callback) {
        return false;
    }


  /**
   * Brings up the UI to perform crop on passed image URI
   * 
   * @param picUri
   *
  private void performCrop(Uri picUri, int destType, Intent cameraIntent) {
    try {
      Intent cropIntent = new Intent("com.android.camera.action.CROP");
      // indicate image type and Uri
      cropIntent.setDataAndType(picUri, "image/*");
      // set crop properties
      cropIntent.putExtra("crop", "true");

      // indicate output X and Y
      if (targetWidth > 0) {
          cropIntent.putExtra("outputX", targetWidth);
      }
      if (targetHeight > 0) {
          cropIntent.putExtra("outputY", targetHeight);
      }
      if (targetHeight > 0 && targetWidth > 0 && targetWidth == targetHeight) {
          cropIntent.putExtra("aspectX", 1);
          cropIntent.putExtra("aspectY", 1);
      }
      // create new file handle to get full resolution crop
      croppedUri = Uri.fromFile(new File(getTempDirectoryPath(), System.currentTimeMillis() + ".jpg"));
      cropIntent.putExtra("output", croppedUri);

      // start the activity - we handle returning in onActivityResult

      if (this.cordova != null) {
        this.cordova.startActivityForResult((CordovaPlugin) this,
            cropIntent, CROP_CAMERA + destType);
      }
    } catch (ActivityNotFoundException anfe) {
      Log.e(LOG_TAG, "Crop operation not supported on this device");
      try {
          processResultFromCamera(destType, cameraIntent);
      }
      catch (IOException e)
      {
          e.printStackTrace();
          Log.e(LOG_TAG, "Unable to write to file");
      }
    }
  }
*/
    /**
     * Applies all needed transformation to the image received from the camera.
     *
     * @param destType          In which form should we return the image
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     *
    private void processResultFromCamera(int destType, Intent intent) throws IOException {
        int rotate = 0;

        // Create an ExifHelper to save the exif data that is lost during compression
        ExifHelper exif = new ExifHelper();
        String sourcePath;
        try {
            if(allowEdit && croppedUri != null)
            {
                sourcePath = FileHelper.stripFileProtocol(croppedUri.toString());
            }
            else
            {
                sourcePath = getTempDirectoryPath() + "/.Pic.jpg";
            }

            //We don't support PNG, so let's not pretend we do
            exif.createInFile(getTempDirectoryPath() + "/.Pic.jpg");
            exif.readExifData();
            rotate = exif.getOrientation();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = null;
        Uri uri = null;

        // If sending base64 image back
        if (destType == DATA_URL) {
            if(croppedUri != null) {
                bitmap = getScaledBitmap(FileHelper.stripFileProtocol(croppedUri.toString()));
            }
            else
            {
                bitmap = getScaledBitmap(FileHelper.stripFileProtocol(imageUri.toString()));
            }
            if (bitmap == null) {
                // Try to get the bitmap from intent.
                bitmap = (Bitmap)intent.getExtras().get("data");
            }
            
            // Double-check the bitmap.
            if (bitmap == null) {
                Log.d(LOG_TAG, "I either have a null image path or bitmap");
                this.failPicture("Unable to create bitmap!");
                return;
            }

            if (rotate != 0 && this.correctOrientation) {
                bitmap = getRotatedBitmap(rotate, bitmap, exif);
            }

            this.processPicture(bitmap);
            checkForDuplicateImage(DATA_URL);
        }

        // If sending filename back
        else if (destType == FILE_URI || destType == NATIVE_URI) {
            uri = Uri.fromFile(new File(getTempDirectoryPath(), System.currentTimeMillis() + ".jpg"));

            if (this.saveToPhotoAlbum) {
                //Create a URI on the filesystem so that we can write the file.
                uri = Uri.fromFile(new File(getPicutresPath()));
            } else {
                uri = Uri.fromFile(new File(getTempDirectoryPath(), System.currentTimeMillis() + ".jpg"));
            }

            if (uri == null) {
                this.failPicture("Error capturing image - no media storage found.");
                return;
            }

            // If all this is true we shouldn't compress the image.
            if (this.targetHeight == -1 && this.targetWidth == -1 && this.mQuality == 100 && 
                    !this.correctOrientation) {
                writeUncompressedImage(uri);

                this.callbackContext.success(uri.toString());
            } else {
                bitmap = getScaledBitmap(FileHelper.stripFileProtocol(imageUri.toString()));

                if (rotate != 0 && this.correctOrientation) {
                    bitmap = getRotatedBitmap(rotate, bitmap, exif);
                }

                // Add compressed version of captured image to returned media store Uri
                OutputStream os = this.cordova.getActivity().getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, this.mQuality, os);
                os.close();

                // Restore exif data to file
                if (this.encodingType == JPEG) {
                    String exifPath;
                    exifPath = uri.getPath();
                    exif.createOutFile(exifPath);
                    exif.writeExifData();
                }

                //Broadcast change to File System on MediaStore
                if(this.saveToPhotoAlbum) {
                    refreshGallery(uri);
                }


                // Send Uri back to JavaScript for viewing image
                this.callbackContext.success(uri.toString());

            }
        } else {
            throw new IllegalStateException();
        }

        this.cleanup(FILE_URI, this.imageUri, uri, bitmap);
        bitmap = null;
    }*/
/*
private String getPicutresPath()
{
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "IMG_" + timeStamp + ".jpg";
    File storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES);
    String galleryPath = storageDir.getAbsolutePath() + "/" + imageFileName;
    return galleryPath;
}

private void refreshGallery(Uri contentUri)
{
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    mediaScanIntent.setData(contentUri);
    this.cordova.getActivity().sendBroadcast(mediaScanIntent);
}


private String ouputModifiedBitmap(Bitmap bitmap, Uri uri) throws IOException {
        // Create an ExifHelper to save the exif data that is lost during compression
        String modifiedPath = getTempDirectoryPath() + "/modified.jpg";

        OutputStream os = new FileOutputStream(modifiedPath);
        bitmap.compress(Bitmap.CompressFormat.JPEG, this.mQuality, os);
        os.close();

        // Some content: URIs do not map to file paths (e.g. picasa).
        String realPath = FileHelper.getRealPath(uri, this.cordova);
        ExifHelper exif = new ExifHelper();
        if (realPath != null && this.encodingType == JPEG) {
            try {
                exif.createInFile(realPath);
                exif.readExifData();
                if (this.correctOrientation && this.orientationCorrected) {
                    exif.resetOrientation();
                }
                exif.createOutFile(modifiedPath);
                exif.writeExifData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return modifiedPath;
    }
*/
/**
     * Applies all needed transformation to the image received from the gallery.
     *
     * @param destType          In which form should we return the image
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     *
    private void processResultFromGallery(int destType, Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            if (croppedUri != null) {
                uri = croppedUri;
            } else {
                this.failPicture("null data from photo library");
                return;
            }
        }
        int rotate = 0;

        // If you ask for video or all media type you will automatically get back a file URI
        // and there will be no attempt to resize any returned data
        if (this.mediaType != PICTURE) {
            this.callbackContext.success(uri.toString());
        }
        else {
            // This is a special case to just return the path as no scaling,
            // rotating, nor compressing needs to be done
            if (this.targetHeight == -1 && this.targetWidth == -1 &&
                    (destType == FILE_URI || destType == NATIVE_URI) && !this.correctOrientation) {
                this.callbackContext.success(uri.toString());
            } else {
                String uriString = uri.toString();
                // Get the path to the image. Makes loading so much easier.
                String mimeType = FileHelper.getMimeType(uriString, this.cordova);
                // If we don't have a valid image so quit.
                if (!("image/jpeg".equalsIgnoreCase(mimeType) || "image/png".equalsIgnoreCase(mimeType))) {
                    Log.d(LOG_TAG, "I either have a null image path or bitmap");
                    this.failPicture("Unable to retrieve path to picture!");
                    return;
                }
                Bitmap bitmap = null;
                try {
                    bitmap = getScaledBitmap(uriString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (bitmap == null) {
                    Log.d(LOG_TAG, "I either have a null image path or bitmap");
                    this.failPicture("Unable to create bitmap!");
                    return;
                }

                if (this.correctOrientation) {
                    rotate = getImageOrientation(uri);
                    if (rotate != 0) {
                        Matrix matrix = new Matrix();
                        matrix.setRotate(rotate);
                        try {
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            this.orientationCorrected = true;
                        } catch (OutOfMemoryError oom) {
                            this.orientationCorrected = false;
                        }
                    }
                }

                // If sending base64 image back
                if (destType == DATA_URL) {
                    this.processPicture(bitmap);
                }

                // If sending filename back
                else if (destType == FILE_URI || destType == NATIVE_URI) {
                    // Did we modify the image?
                    if ( (this.targetHeight > 0 && this.targetWidth > 0) ||
                            (this.correctOrientation && this.orientationCorrected) ) {
                        try {
                            String modifiedPath = this.ouputModifiedBitmap(bitmap, uri);
                            // The modified image is cached by the app in order to get around this and not have to delete you
                            // application cache I'm adding the current system time to the end of the file url.
                            this.callbackContext.success("file://" + modifiedPath + "?" + System.currentTimeMillis());
                        } catch (Exception e) {
                            e.printStackTrace();
                            this.failPicture("Error retrieving image.");
                        }
                    }
                    else {
                        this.callbackContext.success(uri.toString());
                    }
                }
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }
                System.gc();
            }
        }
    }*/
    
    /**
     * Called when the camera view exits.
     *
     * @param requestCode       The request code originally supplied to startActivityForResult(),
     *                          allowing you to identify who this result came from.
     * @param resultCode        The integer result code returned by the child activity through its setResult().
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     *
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Get src and dest types from request code for a Camera Activity
        int srcType = (requestCode / 16) - 1;
        int destType = (requestCode % 16) - 1;

        // If Camera Crop
        if (requestCode >= CROP_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {

                // Because of the inability to pass through multiple intents, this hack will allow us
                // to pass arcane codes back.
                destType = requestCode - CROP_CAMERA;
                try {
                    processResultFromCamera(destType, intent);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Unable to write to file");
                }

            }// If cancelled
            else if (resultCode == Activity.RESULT_CANCELED) {
                this.failPicture("Camera cancelled.");
            }

            // If something else
            else {
                this.failPicture("Did not complete!");
            }
        }
        // If CAMERA
        else if (srcType == CAMERA) {
            // If image available
            if (resultCode == Activity.RESULT_OK) {
                try {
                    if(this.allowEdit)
                    {
                        Uri tmpFile = Uri.fromFile(new File(getTempDirectoryPath(), ".Pic.jpg"));
                        performCrop(tmpFile, destType, intent);
                    }
                    else {
                        this.processResultFromCamera(destType, intent);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    this.failPicture("Error capturing image.");
                }
            }

            // If cancelled
            else if (resultCode == Activity.RESULT_CANCELED) {
                this.failPicture("Camera cancelled.");
            }

            // If something else
            else {
                this.failPicture("Did not complete!");
            }
        }
        // If retrieving photo from library
        else if ((srcType == PHOTOLIBRARY) || (srcType == SAVEDPHOTOALBUM)) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                this.processResultFromGallery(destType, intent);
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                this.failPicture("Selection cancelled.");
            }
            else {
                this.failPicture("Selection did not complete!");
            }
        }
    }

    private int getImageOrientation(Uri uri) {
        int rotate = 0;
        String[] cols = { MediaStore.Images.Media.ORIENTATION };
        try {
            Cursor cursor = cordova.getActivity().getContentResolver().query(uri,
                    cols, null, null, null);
            if (cursor != null) {
                cursor.moveToPosition(0);
                rotate = cursor.getInt(0);
                cursor.close();
            }
        } catch (Exception e) {
            // You can get an IllegalArgumentException if ContentProvider doesn't support querying for orientation.
        }
        return rotate;
    }*/

}
