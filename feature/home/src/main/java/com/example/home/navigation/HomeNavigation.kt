package com.example.home.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.ui.components.DisplayAlertDialog
import com.example.util.model.RequestState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToWriteArgs: (String) -> Unit,
    navigateToAuthScreen: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = com.example.util.Screen.Home.route) {
        val viewModel: com.example.home.HomeViewModel = hiltViewModel()
        val diaries by viewModel.diaries
        val context = androidx.compose.ui.platform.LocalContext.current
        val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed )
        val scope = rememberCoroutineScope()
        var dialogOpened by remember { mutableStateOf(false) }
        var deleteDialogOpened by remember { mutableStateOf(false) }
        LaunchedEffect(key1 = diaries) {
            if(diaries !is RequestState.Loading) {
                onDataLoaded()
            }
        }

        com.example.home.HomeScreen(
            diaries = diaries,
            drawerState = drawerState,
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
            onDateSelected = { viewModel.getDiaries(it) },
            onDateReset = { viewModel.getDiaries() },
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
                scope.launch(kotlinx.coroutines.Dispatchers.IO){
                    val user = App.create(com.example.util.Constants.APP_ID).currentUser
                    if(user != null) {
                        user.logOut()
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
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
                        android.widget.Toast.makeText(
                            context,
                            "All Diaries Deleted.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    onError = {
                        android.widget.Toast.makeText(
                            context,
                            it.message,
                            android.widget.Toast.LENGTH_SHORT
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