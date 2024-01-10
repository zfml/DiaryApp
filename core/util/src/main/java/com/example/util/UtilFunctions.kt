package com.example.util

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import io.realm.kotlin.types.RealmInstant
import java.time.Instant


fun fetchImagesFromFirebase(
    images: List<String>,
    onImageDownload: (Uri) -> Unit,
    onImageDownloadFailed: (Exception) -> Unit = {},
    onReadyToDisplay: () -> Unit = {}
) {
    if(images.isNotEmpty()) {
        images.forEachIndexed { index , image ->
            if(image.trim().isNotEmpty()) {
                FirebaseStorage.getInstance().reference.child(image.trim()).downloadUrl
                    .addOnSuccessListener {
                        onImageDownload(it)
                        if(image.lastIndexOf(images.last()) == index) {
                            onReadyToDisplay()
                        }
                    }
                    .addOnFailureListener {
                        onImageDownloadFailed(it)
                    }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun RealmInstant.toInstant(): Instant {
    val sec: Long = this.epochSeconds
    val nano: Int = this.nanosecondsOfSecond
    return if(sec >= 0) {
        Instant.ofEpochSecond(sec, nano.toLong())
    }else {
        Instant.ofEpochSecond(sec -1 , 1_000_000 + nano.toLong())
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun Instant.toRealmInstant(): RealmInstant {
    val sec: Long = this.epochSecond
    val nano: Int = this.nano
    return if(sec >= 0) {
        RealmInstant.from(sec,nano)
    } else {
        RealmInstant.from(sec + 1 , -1_000_000 + nano)
    }
}