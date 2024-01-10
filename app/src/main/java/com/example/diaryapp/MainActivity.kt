package com.example.diaryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.util.Screen
import com.example.diaryapp.navigation.SetupNavGraph

import com.example.mongo.database.entity.ImageToDelete
import com.example.mongo.database.entity.ImageToUpload
import com.example.ui.theme.DiaryAppTheme
import com.example.util.Constants.APP_ID
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import dagger.hilt.android.AndroidEntryPoint
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imagesToUploadDao: com.example.mongo.database.ImagesToUploadDao
    @Inject
    lateinit var imageToDeleteDao: com.example.mongo.database.ImageToDeleteDao
    var keepSplashOpened = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        installSplashScreen().setKeepOnScreenCondition{
            keepSplashOpened
    }
        WindowCompat.setDecorFitsSystemWindows(window,false)


        setContent {
            DiaryAppTheme {

                val navController = rememberNavController()
                SetupNavGraph(
                    navController = navController,
                    startDestination = getStartDestination(),
                    onDataLoaded = {
                        keepSplashOpened = false
                    }
                )

            }
        }
        cleanupCheck(scope = lifecycleScope,imagesToUploadDao,imageToDeleteDao)
    }

    private fun cleanupCheck(
        scope: CoroutineScope,
        imagesToUploadDao: com.example.mongo.database.ImagesToUploadDao,
        imageToDeleteDao: com.example.mongo.database.ImageToDeleteDao
    ) {
        scope.launch(Dispatchers.IO){
            val result = imagesToUploadDao.getAllImages()
            result.forEach { imageToUpload ->
                retryUploadingImageToFirebase(
                    imageToUpload = imageToUpload,
                    onSuccess = {
                        scope.launch(Dispatchers.IO){
                            imagesToUploadDao.cleanupImage(imageId = imageToUpload.id)
                        }
                    }
                )
            }

            val result2 = imageToDeleteDao.getAllImages()
            result2.forEach {imageToDelete ->
                 retryDeletingImageFromFirebase(
                     imageToDelete = imageToDelete,
                     onSuccess = {
                         scope.launch(Dispatchers.IO) {
                             imageToDeleteDao.cleanupImage(imageId = imageToDelete.id)
                         }
                     }

                 )
            }

        }



    }


    private fun getStartDestination(): String {
        val user = App.create(APP_ID).currentUser
        return if(user != null && user.loggedIn) Screen.Home.route
        else Screen.Authentication.route
    }
 }

fun retryUploadingImageToFirebase(
    imageToUpload: ImageToUpload,
    onSuccess: () -> Unit
) {
    val storage = FirebaseStorage.getInstance().reference
    storage.child(imageToUpload.remoteImagePath).putFile(
        imageToUpload.imageUri.toUri(),
        storageMetadata {  },
        imageToUpload.sessionUri.toUri()
    ).addOnSuccessListener { onSuccess() }
}

fun retryDeletingImageFromFirebase(
    imageToDelete: ImageToDelete,
    onSuccess: () -> Unit
) {
    val storage = FirebaseStorage.getInstance().reference
    storage.child(imageToDelete.remoteImagePath).delete()
        .addOnSuccessListener {
            onSuccess()
        }
}


