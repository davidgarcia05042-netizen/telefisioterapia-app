package com.example.therapp.ui.components

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.OptIn
import com.example.therapp.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Con TextureView (ver res/layout/texture_player_view.xml): el video se
 * desvanece con la pantalla al salir, en lugar de quedar pegado un instante.
 * También respeta las esquinas redondeadas, que un SurfaceView ignoraba.
 */
private fun texturePlayerView(context: Context): PlayerView =
    LayoutInflater.from(context).inflate(R.layout.texture_player_view, null) as PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = {
                        texturePlayerView(context).apply {
                            player = exoPlayer
                            useController = true
                            setFullscreenButtonClickListener {
                                isFullScreen = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        AndroidView(
            factory = {
                texturePlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    setFullscreenButtonClickListener {
                        isFullScreen = true
                    }
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}