package com.video_editor.pavelzubarev.videoeditor

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

//    fun onImageSelectClick(test: android.view.View){
//        val intent = Intent()
//                .setType("*/*")
//                .setAction(Intent.ACTION_GET_CONTENT)
//
//        startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
//    }

    fun onImageSelectClick(test: android.view.View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(intent, 1)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    println("image" + i + "=" + uri.toString())
                }
            }
        }
    }
}
