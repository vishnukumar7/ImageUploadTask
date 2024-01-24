package com.app.imageupload.ui

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.app.imageupload.util.ImagePickOptionDialog
import com.app.imageupload.R
import com.app.imageupload.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener,
    ImagePickOptionDialog.ImagePickListener {
    val TAG = "MainActivity"
    private lateinit var dataBinding: ActivityMainBinding
    private var imagePickOptionDialog : ImagePickOptionDialog? =null
    private var REQUEST_PICK_CODE=0
    var imageUri : Uri? =null

    private lateinit var mainViewModel: MainViewModel

    private val progressDialogLazy = lazy {
        val progressDialog = ProgressDialog(this, R.style.MyAlertDialogStyle)
        progressDialog.setMessage("Loading...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.show()
        return@lazy progressDialog
    }

    private val progressDialog by progressDialogLazy

    private fun showProgress(content: String) {
        if (progressDialog.isShowing) progressDialog.dismiss()
        progressDialog.setMessage("$content...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideProgress() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        dataBinding.getImages.setOnClickListener(this)
        dataBinding.preview.setOnClickListener(this)
        dataBinding.uploadImage.setOnClickListener(this)
        mainViewModel.progressBar.observe(this){
            if(it){
                showProgress("Image Uploading")
            }else{
                hideProgress()
            }
        }

        mainViewModel.errorMessage.observe(this){
            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.getImages -> {
                imagePickOptionDialog = ImagePickOptionDialog(this)
                imagePickOptionDialog?.show(supportFragmentManager,"Image Pick Options")

            }

            R.id.preview -> {
                showImage(imageUri)
            }

            R.id.uploadImage -> {
                getPathFromUri(imageUri)?.let { mainViewModel.uploadImage(this, it) }
            }
        }
    }

    private fun showImage(imageUri : Uri?){

        val imageDialog = Dialog(this)
        imageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        imageDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        imageDialog.setCancelable(true)
        imageDialog.setOnDismissListener {

        }
        val imageView = ImageView(this)
        imageView.setImageURI(imageUri)
        imageDialog.addContentView(imageView,RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))
        imageDialog.show()

    }

    override fun pickOption(requestPickCode: Int) {
        REQUEST_PICK_CODE=requestPickCode
        if(requestPickCode== ImagePickOptionDialog.CAMERA_CODE){
            if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions.launch(arrayOf(Manifest.permission.CAMERA))
            }else{
                pickFromCamera()
            }
        }else if(requestPickCode== ImagePickOptionDialog.GALLERY_CODE){
            var checkPermissionGranted = true
            for (permission in permissionImage) {
                checkPermissionGranted= checkPermissionGranted && checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED
                if(!checkPermissionGranted)
                    break
            }
            if(checkPermissionGranted){
                pickFromGallery()
            }else{
                requestPermissions.launch(permissionImage)
            }
        }
    }


    private fun pickFromGallery(){
        imageUri=null
        dataBinding.preview.visibility=View.GONE
        dataBinding.uploadImage.visibility=View.GONE
        val intent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        registerImageResult.launch(intent)
    }

    private fun pickFromCamera(){
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE,"Temp_FILE")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"From the Camera")
        imageUri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        registerImageResult.launch(intent)
    }

    private fun getPathFromUri(uri: Uri?): String? {
        val proj : Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = managedQuery(uri,proj,null,null,null)
        val cursorIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(cursorIndex)
    }


    private val permissionImage : Array<String> by lazy {
        if (Build.VERSION.SDK_INT > 32) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private val registerImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode==Activity.RESULT_OK){
            if(REQUEST_PICK_CODE == ImagePickOptionDialog.GALLERY_CODE){
                val dataIntent = it.data
                imageUri = dataIntent?.data
                Log.e(TAG, ": image url : $imageUri", )
                Log.e(TAG, ": image url authority : ${imageUri?.authority}", )
                Log.e(TAG, ": image url encodedAuthority : ${imageUri?.encodedAuthority}", )
                Log.e(TAG, ": image url encodedFragment : ${imageUri?.encodedFragment}", )
                Log.e(TAG, ": image url encodedPath : ${imageUri?.encodedPath}", )
                Log.e(TAG, ": image url encodedQuery : ${imageUri?.encodedQuery}", )
                imageUri?.let {
                    dataBinding.preview.visibility=View.VISIBLE
                    dataBinding.uploadImage.visibility=View.VISIBLE
                }
                Log.e(TAG, ": image url path : ${getPathFromUri(imageUri)}", )
            }else{
                val dataIntent = it.data
                imageUri?.let {
                    dataBinding.preview.visibility=View.VISIBLE
                    dataBinding.uploadImage.visibility=View.VISIBLE
                    Log.e(TAG, ": image url full path : ${getPathFromUri(imageUri)}", )
                }
                Log.e(TAG, ": image url path : $imageUri", )
            }
        } else {
            imageUri=null
            dataBinding.preview.visibility=View.GONE
            dataBinding.uploadImage.visibility=View.GONE
        }

    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        var checkPermissionGrand = true
        it.values.forEach {
            checkPermissionGrand = checkPermissionGrand && it
        }
        if(REQUEST_PICK_CODE== ImagePickOptionDialog.CAMERA_CODE){
            if(checkPermissionGrand){
                Toast.makeText(this, getString(R.string.camera_permission_is_denied), Toast.LENGTH_SHORT).show()
            }else{
                pickFromCamera()
            }
        }else if(REQUEST_PICK_CODE== ImagePickOptionDialog.GALLERY_CODE){
            if(checkPermissionGrand){
                pickFromGallery()
            }else{
                Toast.makeText(this,getString(R.string.storage_permission_is_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
