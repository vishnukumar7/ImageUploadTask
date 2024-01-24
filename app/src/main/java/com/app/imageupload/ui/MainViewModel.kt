package com.app.imageupload.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.app.imageupload.R
import com.app.imageupload.network.ApiInterface
import com.app.imageupload.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _progressBar = MutableLiveData<Boolean>()
    val progressBar : LiveData<Boolean> = _progressBar


    private val _errorMessage = MutableLiveData<String>()
    val errorMessage : LiveData<String> = _errorMessage



    fun uploadImage(context: Context,filePath : String){
        if(networkCheck(context)){
            CoroutineScope(Dispatchers.IO).launch {
                _progressBar.postValue(true)
                val file = File(filePath)
                val requestBody = RequestBody.create(MultipartBody.FORM,file)
                val body = MultipartBody.Part.createFormData("image",file.name,requestBody)
                val apiInterface = RetrofitClient.buildService(ApiInterface::class.java)
                val call = apiInterface.uploadImage(body)
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        _progressBar.postValue(true)
                        if(response.isSuccessful && response.body()!=null){
                            //success response
                            _errorMessage.postValue(context.getString(R.string.image_uploaded_successfully))
                        }else{
                            //failure response
                            _errorMessage.postValue(context.getString(R.string.image_upload_failed))
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        _progressBar.postValue(true)
                        // error response or network failure
                        _errorMessage.postValue(t.message)
                    }

                })
            }
        }else{
            _errorMessage.postValue(context.getString(R.string.check_your_internet_connection))
        }

    }

    private fun networkCheck(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
}