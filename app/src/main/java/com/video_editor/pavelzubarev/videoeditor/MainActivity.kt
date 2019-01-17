package com.video_editor.pavelzubarev.videoeditor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import org.jcodec.api.android.AndroidSequenceEncoder
import android.widget.Toast
import java.lang.reflect.Array.getLength
import android.content.res.AssetFileDescriptor
import android.media.*
import android.net.Uri
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer




class MainActivity : AppCompatActivity() {
    private val images = mutableListOf<Bitmap>()
    private var sound = String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onVideoCreateClick(test: android.view.View) {
        val permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 3
            );
        }
        else
            createVideo()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, result: IntArray) {
        when (requestCode) {
            3 -> {
                createVideo()
            }
        }
    }

    fun createVideo() {
        if (sound == "" || images.isEmpty()) {
            Toast.makeText(applicationContext, "No inputs", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val videoName = "video.mp4"
            val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), File.separator)
            path.mkdirs()
            val videoFile = File(path, videoName)
            videoFile.createNewFile()
            val encoder = AndroidSequenceEncoder.createSequenceEncoder(videoFile, 1)
            images.forEach {
                encoder.encodeImage(Bitmap.createScaledBitmap(it, 640, 480, true))
            }
            encoder.finish()

            val output = File(path, "result.mp4")
            output.createNewFile()
            val soundExtractor = MediaExtractor()
            var fb = contentResolver.openFileDescriptor(Uri.parse(sound), "r")
            soundExtractor.setDataSource(fb.fileDescriptor)
            val videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(videoFile.path)

            val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            videoExtractor.selectTrack(0)
            val videoFormat = videoExtractor.getTrackFormat(0)
            val videoTrack = muxer.addTrack(videoFormat)

            soundExtractor.selectTrack(0)
            val audioFormat = soundExtractor.getTrackFormat(0)
            val audioTrack = muxer.addTrack(audioFormat)

            var sawEOS = false
            var frameCount = 0
            val offset = 100
            val sampleSize = 256 * 1024
            val videoBuf = ByteBuffer.allocate(sampleSize)
            val audioBuf = ByteBuffer.allocate(sampleSize)
            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            soundExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            muxer.start()

            while (!sawEOS) {
                videoBufferInfo.offset = offset
                audioBufferInfo.offset = offset

                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset)
                audioBufferInfo.size = soundExtractor.readSampleData(audioBuf, offset)

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {

                    sawEOS = true
                    videoBufferInfo.size = 0
                    audioBufferInfo.size = 0
                } else {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime()
                    videoBufferInfo.flags = videoExtractor.getSampleFlags()
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo)
                    videoExtractor.advance()

                    audioBufferInfo.presentationTimeUs = soundExtractor.getSampleTime()
                    audioBufferInfo.flags = soundExtractor.getSampleFlags()
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo)
                    soundExtractor.advance()

                    frameCount++
                }
            }
            muxer.stop()
            muxer.release()
        }
        catch (e :IOException) {
            Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(applicationContext, "Succes", Toast.LENGTH_SHORT).show()
    }

    fun onImageSelectClick(test: android.view.View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(intent, 1)
    }

    fun onSoundSelectClick(test: android.view.View) {
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(intent, 2)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            1 -> {
                if (resultCode == Activity.RESULT_OK) {
                    images.clear()
                    if (data.clipData != null)
                        for (i in 0 until data.clipData!!.itemCount)
                            images.add(MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.clipData!!.getItemAt(i).uri))
                    else
                        if (data.data != null)
                            images.add(MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.data))
                }
                imagesLabel.text = "Upload images: %d".format(images.size)
            }
            2 -> {
                if (resultCode == Activity.RESULT_OK) {
                    sound = data.data.toString()
                }
                soundLabel.text = "Sound load:" + (if (sound  == null) "none" else sound.toString())
            }
            3 -> {
                createVideo()
            }
        }
    }
}
