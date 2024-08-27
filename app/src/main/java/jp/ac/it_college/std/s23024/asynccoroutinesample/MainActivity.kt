package jp.ac.it_college.std.s23024.asynccoroutinesample

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import fuel.Fuel
import fuel.get
import fuel.serialization.toJson
import jp.ac.it_college.std.s23024.asynccoroutinesample.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val DEBUG_TAG = "AsyncSample"
        private const val WEATHER_INFO_URL =
            "https://api.openweathermap.org/data/2.5/weather?lang=ja"
        private const val APP_ID = BuildConfig.apikey
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 表示(ビュー)の初期化
        binding.apply {
            lvCityList.apply {
                // RecyclerView の初期化。詳細は過去のチャプターのコードを参照してください
                adapter = CityListAdapter(City.list, ::receiveWeatherInfo)

                val manager = LinearLayoutManager(this@MainActivity)
                layoutManager = manager
                addItemDecoration(
                    DividerItemDecoration(this@MainActivity, manager.orientation)
                )
            }
        }
    }
    @UiThread
    private fun receiveWeatherInfo(item: City) {
        // コルーチンを使って通信するやつの前段階
        lifecycleScope.launch {
            val url = "$WEATHER_INFO_URL&q=${item.q}&appid=$APP_ID"
            val result = weatherInfoBackgroundRunner(url) ?: return@launch
            //データが取れなかったここでコールチン終わり→→→→→→→→→→→→→→→→↑

            // 画面にデータを反映させる
            binding.tvWeatherTelop.text = getString(R.string.tv_telop, result.cityName)
            binding.tvWeatherDesc.text = getString(
                R.string.tv_description,
                result.weather[0].description,
                result.coordinates.longitude,
                result.coordinates.latitude
            )
        }

    }

    @WorkerThread
    private suspend fun weatherInfoBackgroundRunner(url: String) : CurrentWeather? {
        // Web API へのアクセスをする
        val returnVal = withContext(Dispatchers.IO) {
            // Jsonパーサ (Kotlinx.serialization)
            val json = Json {
                ignoreUnknownKeys = true
            }
            // Fuel おｗ使って通信する
            val result = Fuel.get(url).toJson(json, CurrentWeather.serializer())
            // Result 型の中に指定した形のデータクラスがあるので、getメソッドで取り出す(Nullable)
            result.get()
        }
        return returnVal
    }
}