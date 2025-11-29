
package com.aslihanmetehan.story

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONArray
import java.nio.charset.Charset
import android.content.Context

data class Scene(val id: Int, val title: String, val text: String, val choiceA: Pair<String, Int>, val choiceB: Pair<String, Int>)

class StoryViewModel(context: Context): ViewModel() {
    private val assets = context.assets
    var scenes by mutableStateOf(listOf<Scene>())
        private set
    var currentSceneId by mutableStateOf(0)
    var history by mutableStateOf(listOf<Int>())
    var bgmPlaying by mutableStateOf(false)

    init {
        loadScenes()
    }

    private fun loadScenes() {
        try {
            val jsonStr = assets.open("scenes.json").readBytes().toString(Charset.defaultCharset())
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Scene>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getInt("id")
                val title = obj.getString("title")
                val text = obj.getString("text")
                val aText = obj.getJSONObject("a").getString("text")
                val aTo = obj.getJSONObject("a").getInt("to")
                val bText = obj.getJSONObject("b").getString("text")
                val bTo = obj.getJSONObject("b").getInt("to")
                list.add(Scene(id, title, text, Pair(aText, aTo), Pair(bText, bTo)))
            }
            scenes = list
            if (list.isNotEmpty()) currentSceneId = list[0].id
        } catch (e: Exception) {
            scenes = listOf(Scene(0,"Hata","Sahne yüklenemedi", Pair("","0"), Pair("","0")))
        }
    }

    fun choose(toId: Int) {
        history = history + currentSceneId
        currentSceneId = toId
    }

    fun back() {
        if (history.isNotEmpty()) {
            currentSceneId = history.last()
            history = history.dropLast(1)
        }
    }

    fun currentScene(): Scene {
        return scenes.firstOrNull { it.id == currentSceneId } ?: Scene(0,"Bitti","Oyun sona erdi", Pair("","0"), Pair("","0"))
    }
}

class MainActivity : ComponentActivity() {
    private var soundPool: SoundPool? = null
    private var soundClick = 0
    private var soundBgm = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // SoundPool setup
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
        soundClick = soundPool!!.load(assets.openFd("click.wav"), 1)
        soundBgm = soundPool!!.load(assets.openFd("bgm_short.wav"), 1)

        setContent {
            val vm: StoryViewModel = viewModel(factory = object: androidx.lifecycle.ViewModelProvider.Factory{
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return StoryViewModel(this@MainActivity) as T
                }
            })

            StoryApp(vm, playClick = { playSound(true) }, toggleBgm = { playBgm() })
        }
    }

    private fun playSound(click: Boolean) {
        soundPool?.play(soundClick,1f,1f,1,0,1f)
    }

    private fun playBgm() {
        // short BGM sample: play once
        soundPool?.play(soundBgm, 0.6f, 0.6f, 1, 0, 1f)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }
}

@Composable
fun StoryApp(vm: StoryViewModel, playClick: ()->Unit, toggleBgm: ()->Unit) {
    val scene = vm.currentScene()
    Scaffold(topBar = { TopAppBar(title = { Text("Aslıhan ve Metehan") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(scene.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), elevation = 4.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text(scene.text, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { playClick(); vm.choose(scene.choiceA.second) }, modifier = Modifier.fillMaxWidth()) {
                        Text(scene.choiceA.first)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { playClick(); vm.choose(scene.choiceB.second) }, modifier = Modifier.fillMaxWidth()) {
                        Text(scene.choiceB.first)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { vm.back() }) { Text("Geri") }
                        OutlinedButton(onClick = { toggleBgm() }) { Text("BGM") }
                    }
                }
            }
        }
    }
}
