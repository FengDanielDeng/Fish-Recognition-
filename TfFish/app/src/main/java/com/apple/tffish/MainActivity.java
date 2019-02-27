package com.apple.tffish;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.zxing.BinaryBitmap;
//import com.google.zxing.DecodeHintType;
//import com.google.zxing.MultiFormatReader;
//import com.google.zxing.RGBLuminanceSource;
//import com.google.zxing.Result;
//import com.google.zxing.ResultPoint;
//import com.google.zxing.common.HybridBinarizer;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;


import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import camerakit.Classifier;
import camerakit.MyImageView;
import camerakit.TensorFlowImageClassifier;


public class MainActivity extends AppCompatActivity {
    private static final String  TAG ="MainActivity";
    private static final int CROP_PHOTO = 2;
    private static final int REQUEST_CODE_PICK_IMAGE = 3;
    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 6;
    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE2 = 7;
    private TensorFlowInferenceInterface inferenceInterface;
    private File output;
    private Uri imageUri;
    Bitmap mBitmap;
//    Mat msrcMat;

    private MyImageView   imgView;

    //二维码长度
    private static final double QRCODE_LEN = 50.0;


    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "Input";
    private static final String OUTPUT_NAME = "final_result";


    private static final String MODEL_FILE = "file:///android_asset/test_cliped.pb";
    private static final String LABEL_FILE ="file:///android_asset/output_labels.txt";
    private Classifier classifier;
    private TextView tvClass;
    private TextView tvLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.tvLength = (TextView) findViewById(R.id.tvLength);
        this.tvClass = (TextView) findViewById(R.id.tvClass);

        this.imgView = (MyImageView  ) findViewById(R.id.imgView);

        initTensor();
    }

    public void initTensor(){

        classifier =TensorFlowImageClassifier.create(
                getAssets(),
                MODEL_FILE,
                LABEL_FILE,
                INPUT_SIZE,
                IMAGE_MEAN,
                IMAGE_STD,
                INPUT_NAME,
                OUTPUT_NAME);
    }


    /**
     * 选择相机
     *
     * @param view
     */
    public void onSelectPhoto(View view) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE2);

        } else {
            choosePhoto();
        }
    }

    /**
     * 打开相机
     *
     * @param view
     */
    public void onOpenCamera(View view) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE2);
        } else {
            takePhoto();
        }
    }

    /**
     * 从相册选取图片
     */
    void choosePhoto() {
        /**
         * 打开选择图片的界面
         */
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//相片类型
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);

    }


    /**
     * 拍照
     */
    void takePhoto() {
        /**
         * 最后一个参数是文件夹的名称，可以随便起
         */
        File file = new File(Environment.getExternalStorageDirectory(), "拍照");
        if (!file.exists()) {
            file.mkdir();
        }
        /**
         * 这里将时间作为不同照片的名称
         */
        output = new File(file, System.currentTimeMillis() + ".jpg");

        /**
         * 如果该文件夹已经存在，则删除它，否则创建一个
         */
        try {
            if (output.exists()) {
                output.delete();
            }
            output.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * 隐式打开拍照的Activity，并且传入CROP_PHOTO常量作为拍照结束后回调的标志
         */
        imageUri = Uri.fromFile(output);
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CROP_PHOTO);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            /**
             * 拍照的请求标志
             */
            case CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        /**
                         * 该uri就是照片文件夹对应的uri
                         */
                        mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
//                        msrcMat = new Mat();
//                        Utils.bitmapToMat(mBitmap, msrcMat);
//                        Log.d(TAG, msrcMat.cols() + "");
                        imgView.setImageBitmap(mBitmap);

                    } catch (Exception e) {
                        Toast.makeText(this, "程序崩溃", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.i("tag", "失败");
                }

                break;
            /**
             * 从相册中选取图片的请求标志
             */

            case REQUEST_CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        /**
                         * 该uri是上一个Activity返回的
                         */
                        Uri uri = data.getData();
                        mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
//                        msrcMat = new Mat();
//                        Utils.bitmapToMat(mBitmap, msrcMat);
                        imgView.setImageBitmap(mBitmap);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("tag", e.getMessage());
                        Toast.makeText(this, "程序崩溃", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.i("liang", "失败");
                }

                break;
//            case RESULT_OK:
//             { //RESULT_OK = -1
//                Bundle bundle = data.getExtras();
//                String scanResult = bundle.getString("result");
//                Toast.makeText(MainActivity.this, scanResult, Toast.LENGTH_LONG).show();
//            }
            default:
                break;
        }

    }


    /**
     * 调整图片大小
     *
     * @param bitmap
     *            源
     * @param dst_w
     *            输出宽度
     * @param dst_h
     *            输出高度
     * @return
     */
    public static Bitmap imageScale(Bitmap bitmap, int dst_w, int dst_h) {
        int src_w = bitmap.getWidth();
        int src_h = bitmap.getHeight();
        float scale_w = ((float) dst_w) / src_w;
        float scale_h = ((float) dst_h) / src_h;
        Matrix matrix = new Matrix();
        matrix.postScale(scale_w, scale_h);
        Bitmap dstbmp = Bitmap.createBitmap(bitmap, 0, 0, src_w, src_h, matrix,
                true);
        return dstbmp;
    }
    /**
     * 检测
     *
     * @param view
     */
    public void onDetect(View view) {

        Bitmap bmp=imageScale(mBitmap,INPUT_SIZE,INPUT_SIZE);

        //  Bitmap bmp = Bitmap.createBitmap(colors, 0, INPUT_SIZE, INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
        final List<Classifier.Recognition> results = classifier.recognizeImage(bmp);
        decodeQRCode();
        if(results.size()>0){
            Classifier.Recognition result = results.get(0);
            this.tvClass.setText(result.getTitle()+","+ String.format("%.3f", result.getConfidence()));
        }

//        for (final Classifier.Recognition result : results) {
//            Log.i(TAG, result.getId()+" , "+result.getConfidence()+","+result.getTitle());
//        }
//        Utils.matToBitmap(rgba, mBitmap);//opencv 转bmp
//        imgView.setImageBitmap(mBitmap);
//        //ok
//        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//
//            }
//        };
//        //取消
//        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                Utils.matToBitmap(msrcMat, mBitmap);//opencv 转bmp
//                imgView.setImageBitmap(mBitmap);
//            }
//        };
//        new AlertDialog.Builder(this)
//                .setMessage(Html.fromHtml("是否正确？"))
//                .setPositiveButton("确定", okListener)
//                .setNegativeButton("重置", cancelListener)
//                .create()
//                .show();

    }



    public void onDetectLength(View view) {
        Log.i(TAG,mBitmap.getWidth()+","+mBitmap.getHeight());
        Log.i(TAG, imgView.curLine.pt1.x+","+ imgView.curLine.pt1.y+"-----"+ imgView.curLine.pt1.rx+","+ imgView.curLine.pt1.ry);
        Log.i(TAG, imgView.curLine.pt2.x+","+ imgView.curLine.pt2.y+"-----"+ imgView.curLine.pt2.rx+","+ imgView.curLine.pt2.ry);
       float x1=imgView.curLine.pt1.rx;
        float y1=imgView.curLine.pt1.ry;
        float x2=imgView.curLine.pt2.rx;
        float y2=imgView.curLine.pt2.ry;
        double dist=Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));

        ArrayList<MyImageView.ViewPoint>  petend=imgView.getPtEnd();
        if(petend!=null){
            float pt1_x = petend.get(0).rx;
            float pt1_y = petend.get(0).ry;
            float pt2_x = petend.get(1).rx;
            float pt2_y = petend.get(1).ry;


            double dist1=Math.sqrt((pt2_x-pt1_x)*(pt2_x-pt1_x)+(pt2_y-pt1_y)*(pt2_y-pt1_y));
            double realeDist=dist/dist1*QRCODE_LEN;
           String lenStr= String.format("%.2f", realeDist);
           tvLength.setText(lenStr);

        }

    }

    /**
     * 解析二维码图片
     *
     * @param bitmap   要解析的二维码图片
     */
    public final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);
    public void decodeQRCode() {
        new AsyncTask<Void, Void,  ArrayList<MyImageView.ViewPoint> >() {
            @Override
            protected  ArrayList<MyImageView.ViewPoint>  doInBackground(Void... params) {
                try {
                    imgView.setDrawingCacheEnabled(true);
                    Bitmap bitmap = Bitmap.createBitmap(imgView.getDrawingCache());
                    imgView.setDrawingCacheEnabled(false);

                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int[] pixels = new int[width * height];
                    Log.d(TAG,width+","+height);
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                    RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                    Result result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), HINTS);
                    ResultPoint[]  pts=result.getResultPoints();


                    ArrayList<MyImageView.ViewPoint> ptEnds=new ArrayList<>();
//                    Log.d(TAG,pts.length+"");
                    for( ResultPoint pt:pts){
                        MyImageView.ViewPoint ptv=new MyImageView.ViewPoint(imgView,pt.getX(),pt.getY());
                        ptEnds.add(ptv);
                        Log.d(TAG,pt.getX()+","+pt.getY());
                    }


                    return ptEnds;
                } catch (Exception e) {
                    Log.d(TAG,"检测失败");
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ArrayList<MyImageView.ViewPoint>  ptEnds) {
//                Log.d("wxl", "result=" + result);
//                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();

                imgView.setPtEnd(ptEnds);//推进去点
            }
        }.execute();

    }
//    PreferenceManager.OnActivityResultListener




}
