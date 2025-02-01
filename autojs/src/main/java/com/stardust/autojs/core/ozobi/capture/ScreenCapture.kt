package com.stardust.autojs.core.ozobi.capture

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.stardust.autojs.core.image.ImageWrapper
import com.stardust.autojs.core.image.capture.ScreenCaptureManager
import com.stardust.autojs.core.image.capture.ScreenCaptureRequester
import com.stardust.util.ViewUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

// Created by ozobi - 2025/01/13

class ScreenCapture( private val mContext: Context) {
    private val mScreenCaptureRequester: ScreenCaptureRequester = ScreenCaptureManager()
    init {
        curOrientation = mContext.resources.configuration.orientation
    }
    companion object{
        var curImg: ImageWrapper? = null
        var curImgBitmap:Bitmap? = null
        var isCurImgBitmapValid = false
        var isDoneVerity = false
        var avaliable = false
        var curOrientation = 1
        fun cleanCurImgBitmap(){
            curImgBitmap?.recycle()
            curImgBitmap = null
        }
        fun cleanCurImg(){
            curImg?.recycle()
            curImg = null
        }
    }
    fun requestScreenCapture(orientation: Int): Boolean = runBlocking {
        return@runBlocking runCatching {
            try{
                stopScreenCapturer()
                mScreenCaptureRequester.requestScreenCapture(
                    mContext, orientation
                )
                curOrientation = orientation
                captureScreen(false)
                avaliable = true
            }catch(e:Exception){
                Log.d("ozobiLog",e.toString())
            }
        }.isSuccess
    }
    fun stopScreenCapturer(){
        mScreenCaptureRequester.recycle()
    }
    suspend fun isValidBitmap(bitmap: Bitmap?):Boolean{
        val width = bitmap?.width
        val height = bitmap?.height
        if(bitmap == null || width == null || height == null){
            return false
        }
        val min: Int = if(width < height){
            width
        }else{
            height
        }
        val statusBar = ViewUtil.getStatusBarHeight(mContext).absoluteValue + 3
        var pos = 5
        for(index in IntRange(statusBar,min)){
            if(pos > min){
                Log.d("ozobiLog","大概率无效截图: min: $min")
                withContext(Dispatchers.Main){
                    Toast.makeText(mContext,"鉴定:无效截图",Toast.LENGTH_LONG).show()
                }
                return false
            }
            val pixel = bitmap.getPixel(pos,pos)
            if(pixel != -16777216 && pixel != -1644826){
                Log.d("ozobiLog","有效截图: $index pixel: "+bitmap.getPixel(pos,pos))
                return true
            }
            pos += 10
        }
        return false
    }
    @Synchronized
    fun captureScreen(isUpdate:Boolean): ImageWrapper {
        val screenCapture = mScreenCaptureRequester.screenCapture
        checkNotNull(screenCapture) { SecurityException("No screen capture permission") }
        return runBlocking {
            if(isUpdate){
                isDoneVerity = false
                cleanCurImg()
                cleanCurImgBitmap()
                curImg = screenCapture.captureImageWrapper()
                isCurImgBitmapValid = isValidBitmap(curImg!!.bitmap)
                isDoneVerity = true
                if(isCurImgBitmapValid){
                    curImgBitmap = curImg!!.bitmap
                    Log.d("ozobiLog","ScreenCapture: 当前截图已更新")
                }
                return@runBlocking curImg!!
            }else{
                return@runBlocking screenCapture.captureImageWrapper()
            }
        }
    }
}