package com.lk.infinitx.player.exoplayer

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.lk.infinitx.player.exoplayer.callbacks.MusicPlaybackPreparer
import com.lk.infinitx.player.exoplayer.callbacks.MusicPlayerEventListener
import com.lk.infinitx.player.exoplayer.callbacks.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG = "SERVICE_TAG"

@AndroidEntryPoint
class MusicService():MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main+serviceJob)
    private lateinit var mediaSession:MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    var isForegroundService = false
    private var currentPlayingSong:MediaMetadataCompat? = null

    private lateinit var musicNotificationManager: MusicNotificationManager

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this,0,it,0)
        }

        mediaSession = MediaSessionCompat(this,SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ){}

        var musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource = firebaseMusicSource){
            currentPlayingSong = it
            if (it != null) {
                preparePlayer(firebaseMusicSource.songs,it,true)
            }
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        exoPlayer.addListener(MusicPlayerEventListener(this))
        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class MusicQueueNavigator:TimelineQueueNavigator(mediaSession){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }

    }
    private fun preparePlayer(
        songs:List<MediaMetadataCompat>,
        songToPlay:MediaMetadataCompat,
        playNow:Boolean
    ){
        val currentSongIndex = if(currentPlayingSong == null) 0 else songs.indexOf(songToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(currentSongIndex,0L)
        exoPlayer.playWhenReady = playNow

    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }
}