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
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Match extends AppCompatActivity {

    ImageView image1, image2, imageResult, imageGauss, imageCanny;
    Button buttonMatch;
    TextView results;
    View main_layout;

    //Identificacion de objetos
    Bitmap bitmap1;
    Mat img1;
    Mat descriptors1;
    MatOfKeyPoint keypoints1;

    Bitmap bitmap2;
    Mat img2;
    Mat descriptors2;
    MatOfKeyPoint keypoints2;

    Bitmap bitmapResult;
    Bitmap bitmapResultGauss;
    Bitmap bitmapResultCanny;

    FeatureDetector detector;
    DescriptorExtractor descriptor;
    DescriptorMatcher matcher;

    Scalar RED = new Scalar(255, 0, 0);
    Scalar GREEN = new Scalar(0, 255, 0);

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
        imageResult = (ImageView) findViewById(R.id.imageResult);
        imageGauss = (ImageView) findViewById(R.id.imageGauss);
        imageCanny = (ImageView) findViewById(R.id.imageCanny);

        buttonMatch = (Button) findViewById(R.id.button_match);

        results = (TextView) findViewById(R.id.text_result);

        main_layout = findViewById(R.id.content_layout);

        mPath = "/storage/emulated/0/Pictures/1525274387.jpg";

        Bitmap imageBitmap = BitmapFactory.decodeFile(mPath);
        imageBitmap = getImageRotated(mPath, imageBitmap);
        bitmap1 = imageBitmap;
        image1.setImageBitmap(Bitmap.createScaledBitmap(imageBitmap, 500, ((imageBitmap.getHeight()*500)/imageBitmap.getWidth()), false));
        image1.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image1.setVisibility(View.VISIBLE);

        mPath = "/storage/emulated/0/Pictures/1525274240.jpg";

        imageBitmap = BitmapFactory.decodeFile(mPath);
        imageBitmap = getImageRotated(mPath, imageBitmap);
        bitmap2 = imageBitmap;
        image2.setImageBitmap(Bitmap.createScaledBitmap(imageBitmap, 500, ((imageBitmap.getHeight()*500)/imageBitmap.getWidth()), false));
        image2.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image2.setVisibility(View.VISIBLE);


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

        buttonMatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Inicializamos

                detector = FeatureDetector.create(FeatureDetector.ORB);
                descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
                matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                img2 = new Mat();

                //Bitmap1
                Bitmap bmp32 = bitmap1.copy(Bitmap.Config.ARGB_8888, true);
                img1 = new Mat(bitmap1.getWidth(), bitmap1.getHeight(), CvType.CV_8U, new Scalar(4));
                Utils.bitmapToMat(bmp32, img1);
                Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGB2GRAY);
                img1.convertTo(img1, 0); //converting the image to match with the type of the cameras image
                descriptors1 = new Mat();
                keypoints1 = new MatOfKeyPoint();
                detector.detect(img1, keypoints1);
                descriptor.compute(img1, keypoints1, descriptors1);

                //Bitmap2
                bmp32 = bitmap2.copy(Bitmap.Config.ARGB_8888, true);
                img2 = new Mat(bitmap2.getWidth(), bitmap2.getHeight(), CvType.CV_8U, new Scalar(4));
                Utils.bitmapToMat(bmp32, img2);
                Imgproc.cvtColor(img2, img2, Imgproc.COLOR_RGB2GRAY);
                descriptors2 = new Mat();
                keypoints2 = new MatOfKeyPoint();
                detector.detect(img2, keypoints2);
                descriptor.compute(img2, keypoints2, descriptors2);

                Mat objectDetection = new Mat((int)img1.size().width, (int)img1.size().height, CvType.CV_8U, new Scalar(4));
                Imgproc.matchTemplate(img1, img2, objectDetection, Imgproc.TM_SQDIFF);
                bitmapResultGauss = Bitmap.createBitmap(objectDetection.cols(), objectDetection.rows(), Bitmap.Config.ARGB_8888);
                Imgproc.cvtColor(objectDetection, objectDetection, Imgproc.COLOR_GRAY2RGBA, 4);
                //Core.normalize(objectDetection, objectDetection, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);
                //Core.normalize(objectDetection, objectDetection, 0, 1, Core.NORM_MINMAX, -1, new Mat());
                Utils.matToBitmap(objectDetection, bitmapResultGauss);
                imageGauss.setImageBitmap(bitmapResultGauss);

                // Matching
                // Matching
                MatOfDMatch matches = new MatOfDMatch();
                if (img1.type() == img2.type()) {
                    try {
                        matcher.match(descriptors1, descriptors2, matches);
                    } catch(Exception e) {
                        Log.i(TAG, e.getMessage());
                    }
                }

                List<DMatch> matchesList = matches.toList();

                Double max_dist = 0.0;
                Double min_dist = 100.0;

                for (int i = 0; i < matchesList.size(); i++) {
                    Double dist = (double) matchesList.get(i).distance;
                    if (dist < min_dist)
                        min_dist = dist;
                    if (dist > max_dist)
                        max_dist = dist;
                }

                LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
                for (int i = 0; i < matchesList.size(); i++) {
                    if (matchesList.get(i).distance <= (1.5 * min_dist))
                        good_matches.addLast(matchesList.get(i));
                }

                MatOfDMatch goodMatches = new MatOfDMatch();
                goodMatches.fromList(good_matches);

                Mat outputImg = new Mat();
                MatOfByte drawnMatches = new MatOfByte();
                Features2d.drawMatches(img1, keypoints1, img2, keypoints2, goodMatches, outputImg, GREEN, RED, drawnMatches, Features2d.DRAW_RICH_KEYPOINTS);
                Size tam = img2.size();
                tam.width = tam.width*2;
                Imgproc.resize(outputImg, outputImg, tam);

                //PRUEBAS
                Mat cannedImage = null;
                Mat grayImage = null;
                grayImage = new Mat();
                cannedImage = new Mat();


                bitmapResultCanny = Bitmap.createBitmap((int)img2.size().width, (int)img2.size().height, Bitmap.Config.ARGB_8888);

                bitmapResult = Bitmap.createBitmap((int)tam.width, (int)tam.height, Bitmap.Config.ARGB_8888);

                Utils.bitmapToMat(bitmap1, grayImage);

                Imgproc.cvtColor(grayImage, grayImage, Imgproc.COLOR_RGB2GRAY);
                Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
                Imgproc.Canny(grayImage, cannedImage, 0, 200);
                Utils.matToBitmap(cannedImage, bitmapResultCanny);
                imageCanny.setImageBitmap(bitmapResultCanny);


                Utils.matToBitmap(outputImg, bitmapResult);

                imageResult.setImageBitmap(bitmapResult);

                results.setText("");
                results.append("Size" + matches.size());
                results.append("\nSizeList" + matchesList.size());
                results.append("\nSizeGood" + good_matches.size());
                results.append("\nSizeGoodM" + goodMatches.size());

                for (int i = 0; i < good_matches.size(); i++) {
                    results.append("\nPoint" + good_matches.get(i));
                }
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
        Log.i("path", path);
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
            bitmap1 = imageBitmap;
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
                    bitmap2 = selectedImage;
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
