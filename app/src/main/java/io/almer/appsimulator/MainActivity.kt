package io.almer.appsimulator

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.almer.appsimulator.receiver.TAG
import io.almer.appsimulator.ui.theme.AppSimulatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.Manifest
import java.io.File
import java.io.FileOutputStream

private const val TAG = "AppSimulator"
private const val ACTION_OVERRIDE = "com.realwear.wearhf.intent.action.OVERRIDE_COMMANDS"
private const val ACTION_RESTORE = "com.realwear.wearhf.intent.action.RESTORE_COMMANDS"
private const val ACTION_SPEECH_EVENT = "com.realwear.wearhf.intent.action.SPEECH_EVENT"
private const val EXTRA_COMMAND = "command"
private const val EXTRA_COMMANDS = "com.realwear.wearhf.intent.extra.COMMANDS"
private const val EXTRA_SOURCE = "com.realwear.wearhf.intent.extra.SOURCE_PACKAGE"

class MainActivity : ComponentActivity() {

    private lateinit var audioRecorder: AndroidAudioRecorder
    private lateinit var audioPlayer: AndroidAudioPlayer
    private lateinit var recordFile: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        recordFile = File(applicationContext.cacheDir, "inputOutputTest.mp3")
        audioRecorder = AndroidAudioRecorder(applicationContext)
        audioPlayer = AndroidAudioPlayer()

        setContent {
            AppSimulatorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    Column {
                        Text(
                            text = "Add the phrases* you want to be recognized!\n" + "A pop up will appear when you say one of them!",
                            fontSize = 25.sp
                        )
                        Text(
                            text = """*Add just English phrases separated by - """,
                            fontSize = 25.sp,
                            color = Color.Gray,
                        )
                        Text(
                            text = """*You can add up to 20 phrases, beside these, there are 5 other "system words" available: up, down, left, right, home.""".trimMargin(),
                            fontSize = 25.sp,
                            color = Color.Gray,
                        )
                        Divider(color = Color.Blue, thickness = 2.dp)
                        Column {
                            content()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RestoreGrammarButton() {
        Button(onClick = {
            val intent = Intent().apply {
                action = ACTION_RESTORE
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            }
            sendBroadcast(intent)
        }) {
            Text(text = "Restore")
        }
    }


    @Composable
    fun content() {
        val scaffoldState: ScaffoldState = rememberScaffoldState()
        val coroutineScope: CoroutineScope = rememberCoroutineScope()
        val text = remember { mutableStateOf("") }

        LaunchedEffect(key1 = true) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Received ${intent.getStringExtra(EXTRA_COMMAND)}",
                            actionLabel = "Close",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
            registerReceiver(receiver, IntentFilter(ACTION_SPEECH_EVENT))
        }

        val view = LocalView.current
        Scaffold(scaffoldState = scaffoldState) {
            Column {
                RestoreGrammarButton()
                OutlinedTextField(
                    value = text.value,
                    onValueChange = { newText ->
                        text.value = newText
                    },
                    modifier = Modifier.fillMaxWidth(0.83f),
                )
                Button(onClick = {
                    audioRecorder.startRecording(recordFile)
                }) {
                    Text("Record")
                }
                Button(onClick = {
                    audioRecorder.stopRecording()
                    audioPlayer.playFile(recordFile)
                }) {
                    Text("Play Record")
                }

                Button(onClick = {

                    val inputMethodManager =
                        getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Grammar sent: " + text.value.split("-").toString(),
                            actionLabel = "Close"
                        )
                    }

                    val toSend = arrayListOf<String>()

                    if (text.value.isNotEmpty()) {
                        text.value.split("-").forEach {
                            toSend.add(it)
                        }
                    }

                    toSend.forEach { word ->
                        if (word == "") {
                            toSend.remove(word)
                        }
                    }

                    //Send the Words List to the service, it will listen for these words and send
                    //broadcasts when one of the sent words is recognized
                    val intent = Intent(ACTION_OVERRIDE)
                    intent.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                    intent.putExtra(EXTRA_SOURCE, packageName)
                    intent.putExtra(EXTRA_COMMANDS, toSend)
                    sendBroadcast(intent)
                    Log.d(TAG, "upload grammar sent")


                }) {
                    Text(text = "Send")
                }
            }

        }
    }
}

class AndroidAudioRecorder(
    private val context: Context,
) {

    private var recorder: MediaRecorder? = null

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    fun startRecording(outputFile: File) {
        if (recorder != null) return
        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
            setOutputFile(FileOutputStream(outputFile).fd)
            setAudioEncodingBitRate(238500)
            setAudioSamplingRate(16000)
            prepare()
            start()
            recorder = this
        }
    }

    fun stopRecording() {
        recorder?.stop()
        recorder?.reset()
        recorder = null
    }
}

class AndroidAudioPlayer() {

    private var player: MediaPlayer? = null

    fun playFile(file: File) {
        try {
            player = MediaPlayer().apply {
                setDataSource(file.path)
                prepare()
                start()

            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play file", e)
        }
    }

    fun stopPlayer() {
        player?.stop()
        player?.release()
        player = null
    }
}


