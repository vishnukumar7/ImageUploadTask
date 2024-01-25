package com.app.imageupload.ui

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.app.imageupload.R
import com.app.imageupload.databinding.ActivityMainBinding
import com.app.imageupload.util.ImagePickOptionDialog
import com.bumptech.glide.Glide
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener,
    ImagePickOptionDialog.ImagePickListener {
    val TAG = "MainActivity"
    private lateinit var dataBinding: ActivityMainBinding
    private var imagePickOptionDialog : ImagePickOptionDialog? =null
    private var REQUEST_PICK_CODE=0
    private var imageUri : Uri? =null


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
               if(imageUri==null)
                   Toast.makeText(this, getString(R.string.select_your_image), Toast.LENGTH_SHORT).show()
                else{
                   previewImage()
               }
            }

            R.id.uploadImage -> {
                if(imageUri==null)
                    Toast.makeText(this, getString(R.string.select_your_image), Toast.LENGTH_SHORT).show()
                else
                    mainViewModel.uploadImage(this, imageUri!!)
            }
        }
    }

    private fun previewImage(){
        val imageDialog = Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        imageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        imageDialog.setCancelable(true)
        val view=layoutInflater.inflate(R.layout.image_dialog_fragment,null)
        imageDialog.setContentView(view)
        Glide.with(this).load(File(mainViewModel.getPathFromUri(this,imageUri))).into(view.findViewById(R.id.imageView))
        val  backBtn = view.findViewById(R.id.backBtn) as ImageView
        backBtn.setOnClickListener {
            imageDialog.dismiss()
        }
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
        dataBinding.fileNameTxt.text=""
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

            imageUri =  if(REQUEST_PICK_CODE == ImagePickOptionDialog.GALLERY_CODE){
                val dataIntent = it.data
                dataIntent?.data
            }else{
                imageUri
            }
            imageUri?.let {
                dataBinding.preview.visibility=View.VISIBLE
                dataBinding.uploadImage.visibility=View.VISIBLE
                val filePath = mainViewModel.getPathFromUri(this,it)
                filePath?.let {
                    val fileName = filePath.substring(filePath.lastIndexOf("/")+1)
                    dataBinding.fileNameTxt.text= fileName
                }
                Log.e(TAG, ": image url path : $filePath")
            }
        } else {
            imageUri=null
            dataBinding.preview.visibility=View.GONE
            dataBinding.uploadImage.visibility=View.GONE
            dataBinding.fileNameTxt.text=""
        }

    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        var checkPermissionGrand = true
        it.values.forEach {
            checkPermissionGrand = checkPermissionGrand && it
        }
        if(REQUEST_PICK_CODE== ImagePickOptionDialog.CAMERA_CODE){
            if (checkPermissionGrand) {
                pickFromCamera()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_is_denied), Toast.LENGTH_SHORT).show()
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
