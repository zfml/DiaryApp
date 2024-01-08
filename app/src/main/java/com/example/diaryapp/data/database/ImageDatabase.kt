package com.example.diaryapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.diaryapp.data.database.entity.ImageToDelete
import com.example.diaryapp.data.database.entity.ImageToUpload

@Database(
    entities = [ImageToUpload::class,ImageToDelete::class],
    version = 1,
    exportSchema = false
)
abstract class ImageDatabase : RoomDatabase(){
    abstract fun imageToUploadDao(): ImagesToUploadDao
    abstract fun imageToDeleteDao(): ImageToDeleteDao
}