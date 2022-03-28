package com.shawn.opencvdemo

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.shawn.opencvdemo.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initOpenCV()
        binding.button.post {

            binding.button.setOnClickListener {
                val src = Mat()
                val dst = Mat()

                val srcBitmap = (resources.getDrawable(R.mipmap.src, null) as BitmapDrawable).bitmap
                val tplBitmap = (resources.getDrawable(R.mipmap.tpl, null) as BitmapDrawable).bitmap
                Utils.bitmapToMat(srcBitmap, src)

                matchTemplate(src, tplBitmap, dst)
                Utils.matToBitmap(dst, srcBitmap)
                binding.imageView.setImageBitmap(srcBitmap)
//                // 图片灰度处理
//                val bitmap = (binding.imageView.background as BitmapDrawable).bitmap
//                Utils.bitmapToMat(bitmap, src)
//                Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY)
//                Utils.matToBitmap(dst, bitmap)
//                binding.imageView.setImageBitmap(bitmap)
                src.release()
                dst.release()
            }

//            createBitmapFromView(this.window, this.window.decorView) { bitmap, b ->
//                if (b) {
//                    val drawable = BitmapDrawable(bitmap)
//                    binding.imageView.background = drawable
//                } else {
//                    "截图失败".toast()
//                }
//            }
        }
    }


    private fun initOpenCV() {
        val success = OpenCVLoader.initDebug()
        if (success) {
            "加载OpenCV成功".toast()
        } else {
            "加载OpenCV失败".toast()
        }
    }

    private fun matchTemplate(src: Mat, tplB: Bitmap, dst: Mat) {
        val tpl = Mat()
        var tplBitmap = tplB

        // 如果不是8888或者565编码，转换成8888编码
        if (tplBitmap.config != Bitmap.Config.ARGB_8888 && tplBitmap.config != Bitmap.Config.RGB_565) {
            var dataByte = ByteArrayOutputStream()
            tplBitmap.compress(Bitmap.CompressFormat.PNG, 100, dataByte)
            val optionsopt = BitmapFactory.Options()
            optionsopt.inPreferredConfig = Bitmap.Config.RGB_565
            tplBitmap = BitmapFactory.decodeByteArray(dataByte.toByteArray(),
                0,
                dataByte.size(),
                optionsopt)
        }

        Log.e("TAG", "matchTemplate: ${tplBitmap.config}")

        Utils.bitmapToMat(tplBitmap, tpl)
        val height = src.rows() - tpl.rows() + 1
        val width = src.cols() - tpl.cols() + 1
        val result = Mat(height, width, CvType.CV_32FC1)

        // 模版匹配
        val method = Imgproc.TM_SQDIFF
        Imgproc.matchTemplate(src, tpl, result, method)

        val minMaxLocResult = Core.minMaxLoc(result)

        val maxloc = minMaxLocResult.maxLoc
        val minloc = minMaxLocResult.minLoc

        val matchloc: Point =
            if (method == Imgproc.TM_SQDIFF || method == Imgproc.TM_SQDIFF_NORMED) {
                minloc
            } else {
                maxloc
            }

        // 绘制
        src.copyTo(dst)
        Imgproc.rectangle(dst, matchloc, Point(matchloc.x + tpl.cols(), matchloc.y + tpl
            .rows()), Scalar(0.0, 0.0, 255.0), 2, 8, 0)

        "发现位置:top: ${matchloc.y} ; right : ${matchloc.x} ; bottom : ${matchloc.y + tpl.rows()} ; left : ${matchloc.y + tpl.rows()}".toast()

        tpl.release()
        result.release()
    }

    private fun String.toast() {
        Toast.makeText(this@MainActivity, this, Toast.LENGTH_SHORT).show()
    }

    /**
     * 将View转换成Bitmap
     */
    fun createBitmapFromView(window: Window, view: View, callBack: (Bitmap?, Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888, true)
            convertLayoutToBitmap(
                window, view, bitmap
            ) { copyResult -> //如果成功
                if (copyResult == PixelCopy.SUCCESS) {
                    callBack(bitmap, true)
                } else {
                    callBack(null, false)
                }
            }
        } else {
            var bitmap: Bitmap? = null
            //开启view缓存bitmap
            view.isDrawingCacheEnabled = true
            //设置view缓存Bitmap质量
            view.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
            //获取缓存的bitmap
            val cache: Bitmap = view.drawingCache
            if (cache != null && !cache.isRecycled) {
                bitmap = Bitmap.createBitmap(cache)
            }
            //销毁view缓存bitmap
            view.destroyDrawingCache()
            //关闭view缓存bitmap
            view.isDrawingCacheEnabled = false
            callBack(bitmap, bitmap != null)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertLayoutToBitmap(
        window: Window, view: View, dest: Bitmap,
        listener: PixelCopy.OnPixelCopyFinishedListener,
    ) {
        //获取layout的位置
        val location = IntArray(2)
        view.getLocationInWindow(location)
        //请求转换
        PixelCopy.request(
            window,
            Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
            dest, listener, Handler(Looper.getMainLooper())
        )
    }
}