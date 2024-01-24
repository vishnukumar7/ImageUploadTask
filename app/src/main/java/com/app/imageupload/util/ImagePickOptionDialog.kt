package com.app.imageupload.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.app.imageupload.R
import com.app.imageupload.databinding.ImagePickOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ImagePickOptionDialog(private val listener: ImagePickListener) : BottomSheetDialogFragment() {
   companion object{
       val GALLERY_CODE=1001
       val CAMERA_CODE=1002
   }
    private lateinit var binding: ImagePickOptionsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.image_pick_options,container,false)
        binding.pickFromCamera.setOnClickListener {
            listener.pickOption(CAMERA_CODE)
            dismiss()
        }
        binding.pickFromGallery.setOnClickListener {
            listener.pickOption(GALLERY_CODE)
            dismiss()
        }
        return binding.root
    }

    interface ImagePickListener{
        fun pickOption(requestPickCode: Int)
    }
}