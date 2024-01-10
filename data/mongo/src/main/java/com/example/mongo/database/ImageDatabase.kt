package com.example.mongo.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mongo.database.entity.ImageToDelete
import com.example.mongo.database.entity.ImageToUpload

@Database(
    entities = [ImageToUpload::class, ImageToDelete::class],
    version = 1,
    exportSchema = false
)
abstract class ImageDatabase : RoomDatabase(){
    abstract fun imageToUploadDao(): ImagesToUploadDao
    abstract fun imageToDeleteDao(): ImageToDeleteDao
}