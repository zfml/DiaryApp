package com.example.diaryapp.navigation

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.diaryapp.model.GalleryImage
import com.example.diaryapp.model.Mood
import com.example.diaryapp.presentation.components.DisplayAlertDialog
import com.example.diaryapp.presentation.screens.auth.AuthenticationScreen
import com.example.diaryapp.presentation.screens.auth.AuthenticationViewModel
import com.example.diaryapp.presentation.screens.home.HomeScreen
import com.example.diaryapp.presentation.screens.home.HomeViewModel
import com.example.diaryapp.presentation.screens.write.WriteScreen
import com.example.diaryapp.presentation.screens.write.WriteViewModel
import com.example.diaryapp.util.Constants.APP_ID
import com.example.diaryapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.example.diaryapp.model.RequestState
import com.example.diaryapp.model.rememberGalleryState
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String,
    onDataLoaded: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        authenticateRoute(
            onDataLoaded = onDataLoaded,
            navigateToHome = {
                navController.popBackStack()
                navController.navigate(Screen.Home.route)
            }
        )
        homeRoute(
            onDataLoaded = onDataLoaded ,
            navigateToWrite = {
                navController.navigate(Screen.Write.route)
            },
            navigateToAuthScreen = {
                navController.popBackStack()
                navController.navigate(Screen.Authentication.route)
            },
            navigateToWriteArgs = {
                navController.navigate(Screen.Write.passDiaryId(it))
            }
        )
        writeRoute(
            onBackedClicked = {
                navController.popBackStack()
            }
        )

    }
}

fun NavGraphBuilder.authenticateRoute(
    navigateToHome: () -> Unit,
    onDataLoaded: () -> Unit
) {


    composable(route = Screen.Authentication.route) {
        val viewModel: AuthenticationViewModel = viewModel()
        val loadingState by viewModel.loadingState
        val authenticated by viewModel.authenticated
        val oneTapState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()

        LaunchedEffect(key1 = Unit) {
            onDataLoaded()
        }


        AuthenticationScreen(
              oneTapSignInState = oneTapState,
              messageBarState = messageBarState,
              loadingState = loadingState,
              onButtonClicked = {
                  oneTapState.open()
                  viewModel.setLoading(true)
              },
            onSuccessfulFirebaseSignIn = { tokenId ->
               viewModel.signInWithMongoAtlas(
                   tokenId = tokenId,
                   onSuccess = {
                          messageBarState.addSuccess("Successfully Authenticated!")
                           viewModel.setLoading(false)

                   },
                   onError = {
                        messageBarState.addError(it)
                        viewModel.setLoading(false)
                   }
               )
            },
            onDialogDismissed = {
                messageBarState.addError(Exception(it))
                viewModel.setLoading(false)
            },
            onFailedFirebaseSignIn = {
                messageBarState.addError(it)
                viewModel.setLoading(false)
            }
            ,
            authenticated = authenticated,
            navigateToHome = navigateToHome
          )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToWriteArgs: (String) -> Unit,
    navigateToAuthScreen: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Home.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val diaries by viewModel.diaries
        val context = LocalContext.current
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed )
        val scope = rememberCoroutineScope()
        var dialogOpened by remember { mutableStateOf(false) }
        var deleteDialogOpened by remember { mutableStateOf(false) }
        LaunchedEffect(key1 = diaries) {
            if(diaries !is RequestState.Loading) {
                onDataLoaded()
            }
        }

        HomeScreen(
            diaries = diaries,
            drawerState = drawerState ,
            onMenuClicked = {
                 scope.launch {
                     drawerState.open()
                 }
            },
            onSignOutClicked = {
                dialogOpened = true
            },
            onDeleteAllClicked = {
                 deleteDialogOpened = true
            },
            dateIsSelected = viewModel.dateIsSelected,
            onDateSelected = {viewModel.getDiaries(it)},
            onDateReset = {viewModel.getDiaries()},
            navigateToWriteArgs = navigateToWriteArgs,
            navigateToWrite = navigateToWrite
        )

//        LaunchedEffect(key1= Unit) {
//            MongoDB.configurationRealm()
//        }
        
        DisplayAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign Out from your Google Account?",
            dialogOpened = dialogOpened,
            onClosedDialog = {
                 dialogOpened = false
            },
            onYesClicked = {
                scope.launch(Dispatchers.IO){
                    val user = App.create(APP_ID).currentUser
                    if(user != null) {
                        user.logOut()
                        withContext(Dispatchers.Main) {
                            navigateToAuthScreen()
                        }
                    }
                }
            }
        )
        DisplayAlertDialog(
            title = "Delete All Diaries",
            message = "Are you sure you want to permanently delete all your diaries?",
            dialogOpened = deleteDialogOpened,
            onClosedDialog = {
                deleteDialogOpened = false
            },
            onYesClicked = {
                viewModel.deleteAllDiaries(
                    onSuccess = {
                         Toast.makeText(
                             context,
                             "All Diaries Deleted.",
                             Toast.LENGTH_SHORT
                         ).show()
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    onError = {
                        Toast.makeText(
                            context,
                            it.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        )

    }
}

@OptIn(ExperimentalFoundationApi::class)
fun NavGraphBuilder.writeRoute(
    onBackedClicked: () -> Unit
) {
    composable(
        route = Screen.Write.route,
        arguments = listOf(
            navArgument(name = WRITE_SCREEN_ARGUMENT_KEY) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {

        val context = LocalContext.current
        val viewModel: WriteViewModel = hiltViewModel()
        val uiState = viewModel.uiState

        val pagerState = rememberPagerState (pageCount = {Mood.values().size})
        val galleryState = viewModel.galleryState
        val pageNumber by remember {
            derivedStateOf {
                pagerState.currentPage
            }
        }

        LaunchedEffect(key1 = uiState.mood) {
            pagerState.scrollToPage(Mood.valueOf(uiState.mood.name).ordinal)
        }


         WriteScreen(
             uiState = uiState,
             galleryState = galleryState,
             pagerState = pagerState,
             moodName = Mood.values()[pageNumber].name,
             onDeletedDiary = {
                      viewModel.deleteDiary(
                          onSuccess = {
                               Toast.makeText(
                                   context,
                                   "Deleted",
                                   Toast.LENGTH_SHORT
                               ).show()
                              onBackedClicked()
                          },
                          onError = {
                               Toast.makeText(
                                   context,
                                   it,
                                   Toast.LENGTH_SHORT
                               ).show()
                          }
                      )
             },
             onBackClicked = onBackedClicked,
             onTitleChanged = {viewModel.setTitle(it)},
             onDescriptionChanged = {viewModel.setDescription(it)},
             onDateTimeUpdated = {
                 viewModel.updateDateTime(it)
             },
             onSavedClicked = {
                      viewModel.upsertDiary(
                          diary = it.apply { mood = Mood. values()[pageNumber].name },
                          onSuccess = onBackedClicked,
                          onError = {
                              Toast.makeText(
                                  context,
                                  it,
                                  Toast.LENGTH_SHORT
                              ).show()
                          }

                      )
             },
             onImageSelect = {
                 val type = context.contentResolver.getType(it)?.split("/")?.last() ?: "jpg"
                 viewModel.addImage(
                     image = it,
                     imageType = type
                 )
             },
             onImageDeleteClicked = {
                 galleryState.removeImage(it)
             }



        )
    }
}

