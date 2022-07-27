package com.lk.infinitx.player.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.lk.infinitx.player.data.entity.Song
import com.lk.infinitx.player.utils.Constants.SONG_COLLECTION
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class MusicDatabase {

    private val firesotre = FirebaseFirestore.getInstance()
    private val songCollection = firesotre.collection(SONG_COLLECTION)

    suspend fun getAllSongs():List<Song>{
        return try{
            songCollection.get().await().toObjects(Song::class.java)
        }catch (e:Exception){
            Log.e("",e.toString())
            emptyList<Song>()
        }
    }
}