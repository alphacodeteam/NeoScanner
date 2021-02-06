package es.upv.alphacodeteam.neoscanner.view.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.io.FileOutputStream;

import es.upv.alphacodeteam.neoscanner.model.repository.ResLocalRepo;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.RETR_CCOMP;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.threshold;

public class Image {

    public static final int CAMERA_INTENT_CODE = 0, GALLERY_INTENT_CODE = 1, RESULT_OK = -1;
    private static final int MAX_HEIGHT = 500;

    /**
     * Inicio del modo de Galería
     *
     * @param activity
     */
    public static void startModeOfGallery(final Activity activity) {
        // Generamos el Intent
        Intent intent = new Intent();
        // Cualquier tipo de imagen
        intent.setType("image/*");
        // Acción de sistema de archivos
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(intent, GALLERY_INTENT_CODE);
    }

    /**
     * Inicio del modo de Camara
     *
     * @param activity
     */
    public static void startModeOfCamera(final Activity activity, File dir) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = FileProvider.getUriForFile(activity,
                "com.example.android.fileprovider",
                dir);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        activity.startActivityForResult(cameraIntent, CAMERA_INTENT_CODE);
    }

    /**
     * Modifica el tamaño de la imagen mediante el tamaño total.
     */
    private static Bitmap getResizedBitmap(Bitmap bitmap, int maxHeight) {
        double ratio = bitmap.getHeight() / (double) maxHeight;
        int width = (int) (bitmap.getWidth() / ratio);
        return Bitmap.createScaledBitmap(bitmap, width, maxHeight, false);
    }

    /**
     * Intenta encontrar las esquinas de la imagen.
     */
    public static List<PointF> findPoints(Bitmap mBitmap) {
        List<PointF> result = null;

        Mat image = new Mat();
        Mat orig = new Mat();
        org.opencv.android.Utils.bitmapToMat(getResizedBitmap(mBitmap, MAX_HEIGHT), image);
        org.opencv.android.Utils.bitmapToMat(mBitmap, orig);

        Mat edges = edgeDetection(image);
        Bitmap m = null;
        m = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, m);
        //mSelectionImageView.setImageBitmap(getResizedBitmap(m, MAX_HEIGHT));
        MatOfPoint2f largest = findLargestContour(edges);

        if (largest != null) {
            Point[] points = sortPoints(largest.toArray());
            Point[] test = largest.toArray();
            for (Point p: test) {
                Log.d("LARGEST POINTS no compression", "x:" + p.x + " - y:" + p.y );
            }
            for (Point p: points) {
                Log.d("LARGEST POINTS compressed ", "x:" + p.x + " - y:" + p.y );
            }
            result = new ArrayList<>();
            result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));

            largest.release();

        } else {
            Log.d("CALCULATEACTIVITY", "No hay cuadrado, creando uno base!");
            result = new ArrayList<>();
            result.add(new PointF(Double.valueOf(100).floatValue(), Double.valueOf(200).floatValue()));
            result.add(new PointF(Double.valueOf(100).floatValue(), Double.valueOf(100).floatValue()));
            result.add(new PointF(Double.valueOf(200).floatValue(), Double.valueOf(100).floatValue()));
            result.add(new PointF(Double.valueOf(200).floatValue(), Double.valueOf(200).floatValue()));
        }

        edges.release();
        image.release();
        orig.release();

        return result;
    }

    /**
     * Global test
     */
    public static Mat testGlobal(Mat src) {
        Mat result;
        double largest_area=0;
        int largest_contour_index=0;
        Rect bounding_rect;

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
            drawContours( thr, contours, i, new Scalar( 0, 255, 0 ), 4 ); // Draw the largest contour using previously stored index.
        }


        return thr;
    }

    /**
     * Detecta los bordes.
     */
    public static Mat edgeDetection(Mat src) {
        Mat edges = new Mat();
        cvtColor(src, edges, COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);
        Imgproc.Canny(edges, edges, 75, 200);
        return edges;
    }

    /**
     * Encuentra en contorno de 4 PUNTOS mas largo.
     */
    private static MatOfPoint2f findLargestContour(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        findContours(src, contours, new Mat(), Imgproc.RETR_LIST, CHAIN_APPROX_SIMPLE);

        // Get the 5 largest contours
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double area1 = contourArea(o1);
                double area2 = contourArea(o2);
                return (int) (area2 - area1);
            }
        });
        if (contours.size() > 5) contours.subList(4, contours.size() - 1).clear();

        Log.d("CONTOUR", "C: " + contours.size());

        MatOfPoint2f largest = null;

        Log.d("CONTOUR", "A");
        for (MatOfPoint contour : contours) {
            MatOfPoint2f approx = new MatOfPoint2f();
            MatOfPoint2f c = new MatOfPoint2f();
            contour.convertTo(c, CvType.CV_32F);
            Log.d("CONTOUR", "C = " + c.total());
            Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true);
            Log.d("CONTOUR", "aprox: " + approx.total());
            Log.d("CONTOUR", "contour area: " + contourArea(contour));
            if (approx.total() == 4 && contourArea(contour) > 150) {
                // the contour has 4 points, it's valid
                largest = approx;
                Log.d("CONTOUR", "premio");
                break;
            }
        }

        Log.d("CONTOUR", "C");
        return largest;
    }

    /**
     * Transforma la perspectiva
     */
    public static Mat perspectiveTransform(Mat src, List<PointF> points) {
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
    public static Bitmap applyThreshold(Mat src) {
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
    private static Point[] sortPoints(Point[] src) {
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
    private static Mat fourPointTransform(Mat src, Point[] pts) {
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
    public static Bitmap compressBitmap(int width, int height, int quality, String id, Activity activity) throws Exception {
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
