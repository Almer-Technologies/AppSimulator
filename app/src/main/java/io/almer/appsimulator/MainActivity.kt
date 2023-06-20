package io.almer.appsimulator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.almer.appsimulator.ui.theme.AppSimulatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "AppSimulator"
private const val BROADCAST_UPLOAD_GRAMMAR = "io.almer.voiceAssistantUploadGrammar"
private const val BROADCAST_START_SERVICE = "io.almer.voiceAssistantStart"
private const val BROADCAST_STOP_SERVICE = "io.almer.voiceAssistantStop"
private const val WORD_DETECTED_KEY = "io.almer.wordDetected"
private const val EXTRA_WORD = "DETECTED_WORD"
private const val WORDS_LIST = "WORDS_LIST"
private const val APP_NAME = "APP_NAME"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppSimulatorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    Column {
                        Text(
                            text = "Add the phrases* you want to be recognized!\n" +
                                    "A pop up will appear when you say one of them!",
                            fontSize = 13.sp
                        )
                        Text(
                            text = """*Add just English phrases separated by - """,
                            fontSize = 8.sp,
                            color = Color.Gray,
                        )
                        Text(
                            text = """*You can add up to 20 phrases, beside these, there are 5 other "system words" available: up, down, left, right, home.""".trimMargin(),
                            fontSize = 8.sp,
                            color = Color.Gray,
                        )
                        Text(
                            text = "Press the triangle to get rid of keyboard ->",
                            fontSize = 8.sp,
                            color = Color.Gray,
                        )
                        Divider(color = Color.Blue, thickness = 2.dp)
                        Column {
                            SendGrammar()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun startService(receiver: BroadcastReceiver){
        //Tell the service that we want to use it,
        //it will turn on the mic and load the default grammar
        Button(onClick = {
            //turn on the mic in the voice service
            val intent = Intent().apply {
                action = BROADCAST_START_SERVICE
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            }
            //register the receiver to get the broadcasts when an word is detected
            registerReceiver(receiver, IntentFilter(WORD_DETECTED_KEY))
            sendBroadcast(intent)
        }) {
            Text(text = "Start")
        }
    }
    @Composable
    fun stopService(){
        //Tell the service that we don't want to use it,
        //it will turn off the mic and unload the default grammar
        Button(onClick = {
            //send the intent that stops the microphone in voice service
            val intent = Intent().apply {
                action = BROADCAST_STOP_SERVICE
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            }
            sendBroadcast(intent)
        }) {
            Text(text = "Stop")
        }
    }


    @Composable
    fun SendGrammar() {
        val scaffoldState: ScaffoldState = rememberScaffoldState()
        val coroutineScope: CoroutineScope = rememberCoroutineScope()
        val text = remember { mutableStateOf("") }

        //receiver raise a notification when it gets
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(
                    io.almer.appsimulator.receiver.TAG,
                    "received ${intent.getStringExtra(EXTRA_WORD)}"
                )
                coroutineScope.launch {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = "Received ${intent.getStringExtra(EXTRA_WORD)}",
                        actionLabel = "Close",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }

        Scaffold(scaffoldState = scaffoldState) {
            Column {
                Row {
                    startService(receiver)
                    stopService()
                }
                OutlinedTextField(
                    value = text.value,
                    onValueChange = { newText ->
                        text.value = newText
                    },
                    modifier = Modifier.fillMaxWidth(0.83f)
                )

                Button(onClick = {
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Grammar sent: " + text.value.split("-")
                                .toString(), actionLabel = "Close"
                        )
                    }

                    val toSend = arrayListOf<String>()

                    if (text.value.length > 0) {
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
                    val intent = Intent()
                    intent.action = BROADCAST_UPLOAD_GRAMMAR
                    intent.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                    intent.putExtra(WORDS_LIST, toSend)
                    intent.putExtra(APP_NAME, "AppSimulator")
                    sendBroadcast(intent)
                    Log.d(TAG, "upload grammar sent")


                }) {
                    Text(text = "Send")
                }
            }

        }
    }
}