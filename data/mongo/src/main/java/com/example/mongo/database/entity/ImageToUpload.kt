package com.example.mongo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ImageToUpload(
      @PrimaryKey(autoGenerate = true)
     val id: Int = 0,
     val remoteImagePath: String,
     val imageUri: String,
     val sessionUri: String
)
