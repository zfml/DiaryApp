package com.example.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mongo.repository.MongoDB.getAllDiaries
import com.example.mongo.repository.MongoDB.getFilteredDiaries
import com.example.util.connectivity.ConnectivityObserver
import com.example.util.connectivity.NetworkConnectivityObserver
import com.example.util.model.RequestState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.handleCoroutineException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectivity: NetworkConnectivityObserver,
    private val imageToDeleteDao: com.example.mongo.database.ImageToDeleteDao
): ViewModel(){

    private lateinit var allDiariesJob: Job
    private lateinit var filterDiariesJob: Job

    private var network by mutableStateOf(ConnectivityObserver.Status.Unavailable)
    var diaries: MutableState<com.example.mongo.repository.Diaries> = mutableStateOf(RequestState.Idle)
    var dateIsSelected by mutableStateOf(false)
        private set

    init {
        getDiaries()
        viewModelScope.launch {
            connectivity.observe().collect{
                network = it
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDiaries(zonedDateTime: ZonedDateTime? =null) {
        dateIsSelected = zonedDateTime != null
        diaries.value = RequestState.Loading
        if(dateIsSelected && zonedDateTime != null) {
            observeFilteredDiaries(zonedDateTime)
        } else {
            observeAllDiaries()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeFilteredDiaries(zonedDateTime: ZonedDateTime) {
        filterDiariesJob = viewModelScope.launch(Dispatchers.IO) {
            if(::allDiariesJob.isInitialized) {
                allDiariesJob.cancelAndJoin()
            }
            getFilteredDiaries(zonedDateTime = zonedDateTime).collect{ result ->
                diaries.value = result
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeAllDiaries() {
       allDiariesJob = viewModelScope.launch(Dispatchers.IO ){

           if(::filterDiariesJob.isInitialized) {
               filterDiariesJob.cancelAndJoin()
           }

            getAllDiaries().collect{ result ->
                diaries.value = result
            }
        }

    }
    fun deleteAllDiaries(
        onSuccess:() -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if(network == ConnectivityObserver.Status.Available) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val imagesDirectory = "images/${userId}"
            val storage = FirebaseStorage.getInstance().reference
            storage.child(imagesDirectory)
                .listAll()
                .addOnSuccessListener {
                    it.items.forEach{ ref ->
                        val imagePath = "images/${userId}/${ref.name}"
                        storage.child(imagePath).delete()
                            .addOnFailureListener {
                                viewModelScope.launch(Dispatchers.IO){
                                    imageToDeleteDao.addImageToDelete(
                                        com.example.mongo.database.entity.ImageToDelete(
                                            remoteImagePath = imagePath
                                        )
                                    )
                                }

                            }
                    }
                    viewModelScope.launch(Dispatchers.IO){
                        val result = com.example.mongo.repository.MongoDB.deleteAllDiaries()
                        if(result is RequestState.Success) {
                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        } else if(result is RequestState.Error) {
                            withContext(Dispatchers.Main) {
                                onError(result.error)
                            }
                        }
                    }
                }
                .addOnFailureListener { onError(it) }
        } else {
             onError(Exception("No Internet Connection!"))
        }
    }

}