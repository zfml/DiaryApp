package com.example.diaryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.diaryapp.data.database.ImageToDeleteDao
import com.example.diaryapp.data.database.ImagesToUploadDao
import com.example.diaryapp.navigation.Screen
import com.example.diaryapp.navigation.SetupNavGraph
import com.example.diaryapp.ui.theme.DiaryAppTheme
import com.example.diaryapp.util.Constants.APP_ID
import com.example.diaryapp.util.retryDeletingImageFromFirebase
import com.example.diaryapp.util.retryUploadingImageToFirebase
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imagesToUploadDao: ImagesToUploadDao
    @Inject
    lateinit var imageToDeleteDao: ImageToDeleteDao
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
        imagesToUploadDao: ImagesToUploadDao,
        imageToDeleteDao: ImageToDeleteDao
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

