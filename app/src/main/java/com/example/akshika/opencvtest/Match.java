package com.example.akshika.opencvtest;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Match extends AppCompatActivity {

    ImageView image1, image2;
    Button buttonMatch;
    TextView results;
    View main_layout;

    static final int REQUEST_IMAGE_CAPTURE = 2;
    static final int PICK_IMAGE = 3;
    private String mPath = "";
    private File newFile;

    private static final String TAG = "MAINMATCH";

    private final int MY_PERMISSIONS = 100;

    static {
        if(OpenCVLoader.initDebug()){
            Log.i("OpenCV", "Initialize success");
        }else{
            Log.i("OpenCV", "Initialize failed");
        }
    }

    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "Inialize Async success");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        requierePermiso();

        image1 = (ImageView) findViewById(R.id.image1);
        image2 = (ImageView) findViewById(R.id.image2);

        buttonMatch = (Button) findViewById(R.id.button_match);

        results = (TextView) findViewById(R.id.text_result);

        main_layout = findViewById(R.id.content_layout);


        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallBack);

        image1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });

        image2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureGalleryIntent();
            }
        });

    }

    /**
     * funcion para seleccionar uns foto de la galeria (por medio de un intent :p)
     */
    private void dispatchTakePictureGalleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    /**
     * funcion para abrir la camara del dispositivo
     * TODO Cuidado en esta parte, en ocasiones al hacer un cambio en el Manifest lanza una excepcion aqui, no es necesario hacer cambios
     * TODO en caso de error hacer clean, rebuild del proyecto
     */
    private void openCamera() {
        File file = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES);
        boolean isDirectoryCreated = file.exists();

        if(!isDirectoryCreated)
            isDirectoryCreated = file.mkdirs();

        if(isDirectoryCreated){
            Long timestamp = System.currentTimeMillis() / 1000;
            String imageName = timestamp.toString() + ".jpg";
            mPath = file.getPath() + File.separator + imageName;

            newFile = new File(mPath);

            Uri uri = FileProvider.getUriForFile(Match.this, BuildConfig.APPLICATION_ID + ".fileProvider", newFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(newFile));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * funcion para rotar una imagen
     * @param source Bitmap a rotar
     * @param angle angulo de rotacion
     * @return un bitmap perron bien rotado
     */
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public Bitmap getImageRotated(String path, Bitmap bitmapToRotate){
        Bitmap rotatedBitmap = null;
        try{
            ExifInterface ei = new ExifInterface(mPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmapToRotate, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmapToRotate, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmapToRotate, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmapToRotate;
            }
        } catch(IOException ioe) {
            Log.e(TAG, ioe.getMessage());
        }
        return rotatedBitmap;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            MediaScannerConnection.scanFile(Match.this,
                    new String[]{mPath}, null,
                    (path, uri) -> {});
            Bitmap imageBitmap = BitmapFactory.decodeFile(mPath);
            imageBitmap = getImageRotated(mPath, imageBitmap);
            image1.setImageBitmap(Bitmap.createScaledBitmap(imageBitmap, 500, ((imageBitmap.getHeight()*500)/imageBitmap.getWidth()), false));
            image1.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image1.setVisibility(View.VISIBLE);
        }
        if (requestCode == PICK_IMAGE ) {
            if (resultCode == RESULT_OK) {
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = Match.this.getContentResolver().openInputStream(imageUri);
                    Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    selectedImage = getImageRotated(imageUri.getPath(), selectedImage);
                    image2.setImageBitmap(Bitmap.createScaledBitmap(selectedImage, 500, ((selectedImage.getHeight()*500)/selectedImage.getWidth()), false));
                    image2.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    selectedImage = Bitmap.createScaledBitmap(selectedImage, 200, ((selectedImage.getHeight()*200)/selectedImage.getWidth()), false);
                    selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] bitmapdata = baos.toByteArray();
                } catch (FileNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                }

            }else {
                image2.setScaleType(ImageView.ScaleType.CENTER);
            }
        }
    }






    /**
     * SECCION PARA PERMISOS
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MY_PERMISSIONS){
            if(grantResults.length == 3
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(main_layout, getString(R.string.permisos_aceptados), Snackbar.LENGTH_SHORT).show();
            } else {
                showExplanation();
            }
        }else{
            showExplanation();
        }
    }//onRequestPermissionsResult

    /**
     * Funcion para revisar SOLO si se otorgaron los permisos a la aplicacion
     * @return
     */
    public boolean checkPermission(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        if((checkSelfPermission(INTERNET) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                )
            return true;
        return false;
    }

    /**
     * funcion para requerir los permisos necesarios de la aplicación
     * @return true o false dependiendo si se brindaron los permisos
     */
    public boolean requierePermiso() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        if((checkSelfPermission(INTERNET) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                )
            return true;

        if((shouldShowRequestPermissionRationale(INTERNET))
                && (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE))
                && (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE))
                ){
            Snackbar.make(getWindow().getDecorView().getRootView() , R.string.permisos,
                    Snackbar.LENGTH_INDEFINITE).setAction(android.R.string.ok, new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onClick(View v) {
                    requestPermissions(new String[]{
                            INTERNET,
                            READ_EXTERNAL_STORAGE,
                            WRITE_EXTERNAL_STORAGE
                    }, MY_PERMISSIONS);
                }
            }).show();
        }else{
            requestPermissions(new String[]{
                    INTERNET,
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
            }, MY_PERMISSIONS);
        }

        return false;
    }//requierePermiso

    /**
     * funcion para mostrar una explicacion del por que se requieren los permisos
     */
    private void showExplanation() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(Match.this);
        builder.setTitle(R.string.permisos_denegados);
        builder.setMessage(R.string.permisos_mensaje_denegados);
        builder.setPositiveButton(R.string.permisos_aceptar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requierePermiso();
            }
        });
        builder.setNegativeButton(R.string.permisos_cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        builder.show();
    }//showExplanation

}
