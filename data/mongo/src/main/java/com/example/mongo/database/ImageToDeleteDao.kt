package com.example.mongo.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mongo.database.entity.ImageToDelete

@Dao
interface ImageToDeleteDao {
    @Query("SELECT * FROM imagetodelete ORDER BY id ASC")
    suspend fun getAllImages(): List<ImageToDelete>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addImageToDelete(imageToDelete: ImageToDelete)

    @Query("DELETE FROM imagetodelete WHERE id=:imageId")
    suspend fun cleanupImage(imageId: Int)
}