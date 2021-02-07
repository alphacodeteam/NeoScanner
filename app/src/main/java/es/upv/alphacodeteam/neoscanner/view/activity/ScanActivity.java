package es.upv.alphacodeteam.neoscanner.view.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import es.upv.alphacodeteam.neoscanner.BuildConfig;
import es.upv.alphacodeteam.neoscanner.R;
import es.upv.alphacodeteam.neoscanner.model.repository.ResLocalRepo;
import es.upv.alphacodeteam.neoscanner.view.util.Image;
import es.upv.alphacodeteam.neoscanner.view.util.QuadrilateralSelectionImageView;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.threshold;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "Scan";
    private static final double INCREASE_CONTRAST_VALUE = 1.7;
    private static final int INCREASE_BRIGHTNESS_VALUE = 10;
    private static final int MAX_HEIGHT = 800;
    private static final double IMAGE_SHARPENING_VALUE_1 = 1.5;
    private static final double IMAGE_SHARPENING_VALUE_2 = -0.8;

    QuadrilateralSelectionImageView quadrilateralSelectionImageView;
    ImageView scannedImageView;
    Button scanButton;
    Button okButton;
    Button cancelButton;
    RelativeLayout mainLayout;
    RelativeLayout afterScanLayout;

    private int mOriginReceived;
    private Bitmap mBitmapReceived;
    private Uri mUriReceived;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Log.e(TAG, "OpenCV not loaded");
        } else {
            // Log.e(TAG, "OpenCV loaded");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_scan);

        // Recuperamos los datos recibidos de MainActivity
            mUriReceived = (Uri) getIntent().getExtras().get("uri");

        initialize();
    }

    Bitmap bmp;
    String imagePath = "/sdcard/documents/";
    ProgressDialog progressDialog;

    void initialize() {
        try {
            quadrilateralSelectionImageView = (QuadrilateralSelectionImageView) findViewById(R.id.quadImage);
            scannedImageView = (ImageView) findViewById(R.id.image);
            scanButton = (Button) findViewById(R.id.scanButton);
            okButton = (Button) findViewById(R.id.okButton);
            cancelButton = (Button) findViewById(R.id.cancelButton);
            mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
            afterScanLayout = (RelativeLayout) findViewById(R.id.afterScanLayout);

            // Ubicación de la foto tomada desde galeria
            InputStream originPath = this.getContentResolver().openInputStream(mUriReceived);
            // Generate rand number
            int min = 0, max = 10000;
            String randNum = ThreadLocalRandom.current().nextInt(min, max + 1) + "";
            Log.d("Random Num--> ", randNum);
            // Path destino
            File targetFile = ResLocalRepo.createLocalFile(randNum, this);
            Path targetPath = Paths.get(targetFile.getPath());
            // Copiamos el fichero resultante en nuestra carpeta local
            Files.copy(originPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            // Comprimimos la imagen local //TODO: Esto habrá que cambiarlo y obtener la foto sin compresión
            Bitmap bitmapCompressed = compressBitmap(75, 75, 40, randNum, this);
            bmp = bitmapCompressed;

            if (bmp == null) {
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
            quadrilateralSelectionImageView.setImageBitmap(getResizedBitmap(bmp, MAX_HEIGHT));
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!OpenCVLoader.initDebug()) {
                        Intent returnIntent = new Intent();
                        setResult(Activity.RESULT_CANCELED, returnIntent);
                        finish();
                        return;
                    }
                    List<PointF> points = findPoints();
                    if (points != null) {
                        boolean dontShow = false;
                        for (int i = 0; i < (points.size() - 1); i++) {
                            if (Math.sqrt(Math.pow((points.get(i + 1).length() - points.get(i).length()), 2)) < 100) {
                                dontShow = true;
                            }
                        }
                        if (!dontShow) {
                            quadrilateralSelectionImageView.setPoints(points);
                        }

                        scanButton.setText("Escanear imagen");
                    }else{
                        scanButton.setText("Escanear imagen");
                    }

                    scanButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            progressDialog = new ProgressDialog(ScanActivity.this);
                            progressDialog.setMessage("Escaneando...");
                            progressDialog.setCancelable(false);
                            progressDialog.show();
                            new ScanImageTask().execute();
                        }
                    });
                }
            }, 2000);

        } catch (Exception e) {
            // Crashlytics.logException(e);
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }
    }

    private class ScanImageTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            return scanImage();
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                mainLayout.setVisibility(View.INVISIBLE);
                afterScanLayout.setVisibility(View.VISIBLE);
                scannedImageView.setImageBitmap(bitmap);
                progressDialog.dismiss();
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri pdfUri = exportImageToPDF(bitmap);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri data = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID +".provider",new File(pdfUri.toString()));
                        intent.setDataAndType(data, "application/pdf");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                    }
                });
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        recreate();
                    }
                });
            } else {
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
            }
        }

    }

    //@AddTrace(name = "scan_trace")
    Bitmap scanImage() {
        try {
            List<PointF> points = quadrilateralSelectionImageView.getPoints();
            if (bmp != null) {
                Mat orig = new Mat();
                org.opencv.android.Utils.bitmapToMat(bmp, orig);
                Mat transformed = perspectiveTransform(orig, points);
                Imgproc.cvtColor(transformed, transformed, Imgproc.COLOR_RGB2GRAY, 4);
                Mat orginal = transformed.clone();
                org.opencv.core.Size s = new Size(0, 0);
                Imgproc.GaussianBlur(transformed, transformed, s, 10);
                Core.addWeighted(orginal, IMAGE_SHARPENING_VALUE_1, transformed, IMAGE_SHARPENING_VALUE_2, 0, transformed);
                transformed.convertTo(transformed, -1, INCREASE_CONTRAST_VALUE, INCREASE_BRIGHTNESS_VALUE);
                bmp.recycle();
                final Bitmap result = Bitmap.createBitmap(transformed.width(), transformed.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(transformed, result);
                orig.release();
                transformed.release();
                return result;
            } else return null;
        } catch (Exception e) {
            //Crashlytics.logException(e);
            return null;
        }
    }

    void saveImage(Bitmap bmp) {   boolean saved;
        OutputStream fos;

        try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "name");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/NeoScanner");
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(imageUri);
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + "NeoScanner";

            File file = new File(imagesDir);

            if (!file.exists()) {
                file.mkdir();
            }

            File image = new File(imagesDir, "test.png");
            fos = new FileOutputStream(image);
        }
            saved = bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //@AddTrace(name = "load_bitmap")
    Bitmap getBitmap(String mCurrentPhotoPath) {
        try {
            Bitmap bitmap;
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_HEIGHT, MAX_HEIGHT);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            options.inMutable = true;
            bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options);
            return bitmap;
        } catch (OutOfMemoryError | NullPointerException e) {
            //Crashlytics.logException(e);
            return null;
        }
    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Export
     *
     * @param bitmap
     * @return
     */
    public Uri exportImageToPDF(Bitmap bitmap) {
        Uri pdfUri = null;
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();

        PdfDocument.Page page = pdf.startPage(pi);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#FFFFFF"));

        canvas.drawPaint(paint);

        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        paint.setColor(Color.BLUE);
        canvas.drawBitmap(bitmap, 0, 0, null);

        pdf.finishPage(page);

        // Generate rand number
        int min = 0, max = 10000;
        String randNum = ThreadLocalRandom.current().nextInt(min, max + 1) + "";
        OutputStream fos;
        Intent intent = new Intent(Intent.ACTION_VIEW);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "name");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/NeoScanner");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(imageUri);
                pdfUri = imageUri;
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS).toString() + File.separator + "NeoScanner";

                File file = new File(imagesDir);

                if (!file.exists()) {
                    file.mkdir();
                }

                File image = new File(imagesDir, "Scan-"+randNum+".pdf");
                fos = new FileOutputStream(image);
                pdfUri = Uri.fromFile(image);
            }
            pdf.writeTo(fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return pdfUri;
    }


    // ******************************************************************************************************************************************************************************* //
    // ******************************************************************************** IMAGE PROCESSING ***************************************************************************** //

    /**
     * Global test
     */
    private List<PointF> testGlobal() {
        Mat src = new Mat();
        Utils.bitmapToMat(bmp, src);
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Log.d(TAG, "testGlobal: h - " + src.height() + "; w - " + src.width());
        int heightOffset = src.height() / 600;
        int widthOffset = src.width() / 502;
        List<PointF> result = null;
        Mat hierarchy = new Mat();
        Mat mIntermediateMat = new Mat();
        //Imgproc.GaussianBlur(src,mIntermediateMat,new Size(9,9),2,2);
        //Imgproc.Canny(src, mIntermediateMat, 80, 100);

        cvtColor( src, mIntermediateMat, COLOR_BGR2GRAY ); //Convert to gray
        threshold( mIntermediateMat, mIntermediateMat, 125, 255, THRESH_BINARY ); //Threshold the gray
        Imgproc.findContours(mIntermediateMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        /* Mat drawing = Mat.zeros( mIntermediateMat.size(), CvType.CV_8UC3 );
         for( int i = 0; i< contours.size(); i++ )
         {
        Scalar color =new Scalar(Math.random()*255, Math.random()*255, Math.random()*255);
         Imgproc.drawContours( drawing, contours, i, color, 2, 8, hierarchy, 0, new Point() );
         }*/
        hierarchy.release();
        // Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4)
        /* Mat drawing = Mat.zeros( mIntermediateMat.size(), CvType.CV_8UC3 );
         for( int i = 0; i< contours.size(); i++ )
         {
        Scalar color =new Scalar(Math.random()*255, Math.random()*255, Math.random()*255);
         Imgproc.drawContours( drawing, contours, i, color, 2, 8, hierarchy, 0, new Point() );
         }*/
        Rect rect;
        double largest_area = 0;
        //float offsetw1 = 120;
        //float offsetw2 = 380;
        //float offseth1 = 230;
        //float offseth2 = 645;
        float offsetw1 = 0;
        float offsetw2 = 0;
        float offseth1 = 0;
        float offseth2 = 0;
        for ( int contourIdx=0; contourIdx < contours.size(); contourIdx++ )
        {
            // Minimum size allowed for consideration
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(contourIdx).toArray() );
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            double area = contourArea(contours.get(contourIdx));  //  Find the area of contour

            if( area > largest_area )
            {
                largest_area = area;// Get bounding rect of contour
                rect = Imgproc.boundingRect(points);
                // rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0, 255), 3);
                result = new ArrayList<>();
                result.add(new PointF(Double.valueOf(rect.x + offsetw1).floatValue(), Double.valueOf(rect.y + offseth1).floatValue()));
                result.add(new PointF(Double.valueOf(rect.x + offsetw1).floatValue(), Double.valueOf(rect.y + rect.height + offseth2).floatValue()));
                result.add(new PointF(Double.valueOf(rect.x + rect.width + offsetw2).floatValue(), Double.valueOf(rect.y + rect.height + offseth2).floatValue()));
                result.add(new PointF(Double.valueOf(rect.x + rect.width + offsetw2).floatValue(), Double.valueOf(rect.y + offseth1).floatValue()));
            }

        }
        return result;
        /*
        Mat result;
        double largest_area=0;
        int largest_contour_index=0;
        Rect bounding_rect = new Rect();
        Mat thr = new Mat();
        cvtColor( src, thr, COLOR_BGR2GRAY ); //Convert to gray
        threshold( thr, thr, 125, 255, THRESH_BINARY ); //Threshold the gray
        List<MatOfPoint> contours = new ArrayList<>();
        findContours( thr, contours, new Mat(), RETR_CCOMP, CHAIN_APPROX_SIMPLE ); // Find the contours in the image
        for( int i = 0; i< contours.size(); i++ ) // iterate through each contour.
        {
            double area = contourArea(contours.get(i));  //  Find the area of contour
            if( area > largest_area )
            {
                largest_area = area;
                largest_contour_index = i;               //Store the index of largest contour
                bounding_rect = boundingRect(contours.get(i)); // Find the bounding rectangle for biggest contour
            }
        }
        drawContours(src, contours, -1, new Scalar(255,0,0), 3);
        rectangle(src,new Point(bounding_rect.x, bounding_rect.y),new Point(bounding_rect.x+bounding_rect.height,bounding_rect.y+bounding_rect.width), new Scalar(0,255,0),2);
        for (MatOfPoint c: contours) {
            Log.d(TAG, "testGlobal: " + c.toArray().toString());
        }
        /*
        c = max(contours, key = cv2.contourArea)
        float x,y,w,h = boundingRect(c);
        rectangle(src,(x,y),(x+w,y+h),(0,255,0),2)
        return src;
         */
    }


    /**
     * Modifica el tamaño de la imagen mediante el tamaño total.
     */
    public static Bitmap getResizedBitmap(Bitmap bitmap, int maxHeight) {
        double ratio = bitmap.getHeight() / (double) maxHeight;
        int width = (int) (bitmap.getWidth() / ratio);
        return Bitmap.createScaledBitmap(bitmap, width, maxHeight, false);
    }


    /**
     * Intenta encontrar las esquinas de la imagen.
     */
    public List<PointF> findPoints() {
        try {
            List<PointF> result = null;
            Mat image = new Mat();
            Mat orig = new Mat();
            org.opencv.android.Utils.bitmapToMat(getResizedBitmap(bmp, MAX_HEIGHT), image);
            org.opencv.android.Utils.bitmapToMat(bmp, orig);

            Mat edges = edgeDetection(image);
            MatOfPoint2f largest = findLargestContour(edges);

            if (largest != null) {
                Log.d(TAG, "deuria");
                Point[] points = sortPoints(largest.toArray());
                result = new ArrayList<>();
                result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
                result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));
                largest.release();
            } else {
                Log.d(TAG, "no deuria");
               // result = testGlobal();
            }

            edges.release();
            image.release();
            orig.release();
            return result;
        } catch (Exception e) {
            //Crashlytics.logException(e);
            return null;
        }
    }
    /**
     * Detecta los bordes.
     */
    private Mat edgeDetection(Mat src) {
        Mat edges = new Mat();
        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);
        Imgproc.Canny(edges, edges, 75, 200);
        return edges;
    }

    /**
     * Encuentra en contorno de 4 PUNTOS mas largo.
     */
    private MatOfPoint2f findLargestContour(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Get the 5 largest contours
        try {

            Collections.sort(contours, new Comparator<MatOfPoint>() {
                public int compare(MatOfPoint o1, MatOfPoint o2) {
                    double area1 = Imgproc.contourArea(o1);
                    double area2 = Imgproc.contourArea(o2);
                    return (int) (area2 - area1);
                }
            });
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (contours.size() > 5) contours.subList(4, contours.size() - 1).clear();

        MatOfPoint2f largest = null;
        for (MatOfPoint contour : contours) {
            MatOfPoint2f approx = new MatOfPoint2f();
            MatOfPoint2f c = new MatOfPoint2f();
            contour.convertTo(c, CvType.CV_32FC2);
            Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true);

            if (approx.total() == 4 && Imgproc.contourArea(contour) > 150) {
                // the contour has 4 points, it's valid
                largest = approx;
                break;
            }
        }
        return largest;
    }

    /**
     * Transforma la perspectiva
     */
    public Mat perspectiveTransform(Mat src, List<PointF> points) {
        Point point1 = new Point(points.get(0).x, points.get(0).y);
        Point point2 = new Point(points.get(1).x, points.get(1).y);
        Point point3 = new Point(points.get(2).x, points.get(2).y);
        Point point4 = new Point(points.get(3).x, points.get(3).y);
        Point[] pts = {point1, point2, point3, point4};
        return fourPointTransform(src, sortPoints(pts));
    }

    /**
     * Aplica el Threshold para dar la sensación de escaneado...
     */
    public Bitmap applyThreshold(Mat src) {
        cvtColor(src, src, COLOR_BGR2GRAY);

        // Some other approaches
        // Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
        // Imgproc.threshold(src, src, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        Imgproc.GaussianBlur(src, src, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2);

        Bitmap bm = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(src, bm);

        return bm;
    }

    /**
     * Ordena los puntos
     * <p>
     * Sigue el siguiente flujo:
     * 0------->1
     * ^        |
     * |        v
     * 3<-------2
     *
     * @param src Los puntos a ordenar
     * @return Una lista de puntos ordenados
     */
    private Point[] sortPoints(Point[] src) {
        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));
        Point[] result = {null, null, null, null};

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };
        Comparator<Point> differenceComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        result[0] = Collections.min(srcPoints, sumComparator);        // Upper left has the minimal sum
        result[2] = Collections.max(srcPoints, sumComparator);        // Lower right has the maximal sum
        result[1] = Collections.min(srcPoints, differenceComparator); // Upper right has the minimal difference
        result[3] = Collections.max(srcPoints, differenceComparator); // Lower left has the maximal difference

        return result;
    }

    /**
     * Transforma la imagen.
     */
    private Mat fourPointTransform(Mat src, Point[] pts) {
        double ratio = src.size().height / (double) MAX_HEIGHT;

        Point ul = pts[0];
        Point ur = pts[1];
        Point lr = pts[2];
        Point ll = pts[3];

        double widthA = Math.sqrt(Math.pow(lr.x - ll.x, 2) + Math.pow(lr.y - ll.y, 2));
        double widthB = Math.sqrt(Math.pow(ur.x - ul.x, 2) + Math.pow(ur.y - ul.y, 2));
        double maxWidth = Math.max(widthA, widthB) * ratio;

        double heightA = Math.sqrt(Math.pow(ur.x - lr.x, 2) + Math.pow(ur.y - lr.y, 2));
        double heightB = Math.sqrt(Math.pow(ul.x - ll.x, 2) + Math.pow(ul.y - ll.y, 2));
        double maxHeight = Math.max(heightA, heightB) * ratio;

        Mat resultMat = new Mat(Double.valueOf(maxHeight).intValue(), Double.valueOf(maxWidth).intValue(), CvType.CV_8UC4);

        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);
        srcMat.put(0, 0, ul.x * ratio, ul.y * ratio, ur.x * ratio, ur.y * ratio, lr.x * ratio, lr.y * ratio, ll.x * ratio, ll.y * ratio);
        dstMat.put(0, 0, 0.0, 0.0, maxWidth, 0.0, maxWidth, maxHeight, 0.0, maxHeight);

        Mat M = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Imgproc.warpPerspective(src, resultMat, M, resultMat.size());

        srcMat.release();
        dstMat.release();
        M.release();

        return resultMat;
    }

    /**
     * Compresión del fichero indicado
     *
     * @param width
     * @param height
     * @param quality
     * @param id
     * @param activity
     * @return bitmap
     * @throws Exception
     */
    public Bitmap compressBitmap(int width, int height, int quality, String id, Activity activity) throws Exception {
        File file = ResLocalRepo.getLocalFile(id, activity);
        Bitmap bitmap = ResLocalRepo.getBitmapFromFile(file);
        FileOutputStream out = new FileOutputStream(file);
        // Dimensiones y filtrado bilineal
        Bitmap.createScaledBitmap(bitmap, width, height, true);
        // Calidad de imagen
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        out.close();
        return bitmap;
    }

}