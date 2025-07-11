package com.example.weatherly

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.weatherly.WeatherApi.getAQI
import com.example.weatherly.ui.theme.WeatherlyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Lifecycle", "onCreate called")
        installSplashScreen() // Adds Splash Screen, Configure in res/values/splash.xml
        enableEdgeToEdge() // Adds color to status bar and navigation bar
        super.onCreate(savedInstanceState)
        setContent {
            val dataStore = DataStore(LocalContext.current)
            val darkModeState by dataStore.darkModeFlow.collectAsState(initial = isSystemInDarkTheme())
            val cityState by dataStore.cityFlow.collectAsState(initial = "Athens")
            WeatherlyTheme(useDarkTheme = darkModeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(dataStore, darkModeState, cityState)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("Lifecycle", "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d("Lifecycle", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d("Lifecycle", "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("Lifecycle", "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Lifecycle", "onDestroy called")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun App(dataStore: DataStore, darkModeState: Boolean, cityState: String) {
    Log.d("WeatherApp", "App Composable called")

    var refreshEffect by remember { mutableStateOf(true) }
    var textFieldInput by remember { mutableStateOf("") }

    var homeData by remember { mutableStateOf(WeatherApi.dummyData()) }
    var prevData by remember { mutableStateOf(homeData) }

    var aqiData by remember { mutableStateOf(WeatherApi.dummyDataAQI()) }
    var prevAQIData by remember { mutableStateOf(aqiData) }
    var aqiIndex by remember { mutableIntStateOf(0) }
    var aqiName by remember { mutableStateOf("") }

    var forecastData by remember { mutableStateOf(WeatherApi.forecastDummyData()) }
    var prevForecastData by remember { mutableStateOf(WeatherApi.forecastDummyData()) }

    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var sunriseTime by remember { mutableStateOf(ZonedDateTime.now()) }
    var sunsetTime by remember { mutableStateOf(ZonedDateTime.now()) }
    var updatedOnTime by remember { mutableStateOf(ZonedDateTime.now()) }

    var sunriseZone by remember { mutableStateOf(ZonedDateTime.now()) }
    var sunsetZone by remember { mutableStateOf(ZonedDateTime.now()) }

    var currIcon by remember { mutableIntStateOf(R.drawable._01d) }
    var atSettings by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    fun refresh() = refreshScope.launch {
        refreshing = true
        delay(1000)
        refreshEffect = !refreshEffect
        refreshing = false
    }
    val state = rememberPullRefreshState(refreshing, ::refresh)

    // 🔁 Fetch Data
    LaunchedEffect(cityState, refreshEffect) {
        if (cityState.isBlank()) {
            Log.e("App", "❌ Skipping fetch: empty city name")
            return@LaunchedEffect
        }

        Log.d("App", "🌍 Fetching weather data for city: $cityState")

        try {
            val newHomeData = WeatherApi.readMainData(cityState)
            Log.d("App", "📦 Received homeData: ${newHomeData.name}")

            if (newHomeData == WeatherApi.dummyData()) {
                Log.e("App", "❌ Fetched dummy data. Possibly due to bad city name or network.")
                return@LaunchedEffect
            }

            val newAQIData = WeatherApi.readAQIData()
            val newForecastData = WeatherApi.readForecastData(cityState)

            // 📝 Update states
            prevData = homeData
            homeData = newHomeData

            prevAQIData = aqiData
            aqiData = newAQIData

            prevForecastData = forecastData
            forecastData = newForecastData

            // Save City
            if (homeData.name.isNotEmpty()) {
                Log.d("App", "💾 Saving city: ${homeData.name}")
                dataStore.writeCity(homeData.name)
            }

            updatedOnTime = Instant.ofEpochSecond(homeData.dt.toLong()).atZone(ZoneId.systemDefault())
            sunriseTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(homeData.sys.sunrise.toLong()), ZoneId.of("UTC"))
            sunsetTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(homeData.sys.sunset.toLong()), ZoneId.of("UTC"))
            sunriseZone = sunriseTime.withZoneSameInstant(ZoneOffset.ofHours(homeData.timezone / 3600))
            sunsetZone = sunsetTime.withZoneSameInstant(ZoneOffset.ofHours(homeData.timezone / 3600))

            // 🌤 Set weather icon
            val iconCode = homeData.weather.firstOrNull()?.icon ?: "01d"
            currIcon = when (iconCode) {
                "01d", "01n" -> R.drawable._01d
                "02d", "02n" -> R.drawable._02d
                "03d", "03n" -> R.drawable._03d
                "04d", "04n" -> R.drawable._04d
                "09d", "09n" -> R.drawable._09d
                "10d", "10n" -> R.drawable._10d
                "11d", "11n" -> R.drawable._11d
                "13d", "13n" -> R.drawable._13d
                "50d", "50n" -> R.drawable._50d
                else -> R.drawable._01d
            }

            // AQI Stats
            aqiName = when (aqiData.list.firstOrNull()?.main?.aqi ?: 0) {
                1 -> "Good"
                2 -> "Fair"
                3 -> "Moderate"
                4 -> "Poor"
                5 -> "Very Poor"
                else -> "-"
            }
            aqiIndex = aqiData.list.firstOrNull()?.components?.let {
                val pm10 = it.pm10
                val pm25 = it.pm2_5
                ((pm25 + pm10) / 2).toInt()  // 🔧 Simple average AQI
            } ?: 0

        } catch (e: Exception) {
            Log.e("AppCrash", "❌ Exception during data fetch: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    // 🧭 UI Layout
    Scaffold(
        topBar = { TopBar(homeData, atSettings) },
        bottomBar = {
            BottomBar(navController) {
                atSettings = navController.currentDestination?.route == Screen.Settings.route
            }
        }
    ) { innerPadding ->
        NavHost(navController, Screen.Main.route, Modifier.padding(innerPadding)) {
            composable(Screen.Main.route) {
                AnimatedContent(
                    targetState = homeData.name.isNotBlank(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "MainScreenAnimation"
                ) { dataLoaded ->
                    if (dataLoaded) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .pullRefresh(state)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.CenterHorizontally))

                            InputText(input = textFieldInput, onValueChange = { textFieldInput = it }) {
                                if (textFieldInput.trim().isNotEmpty()) {
                                    runBlocking {
                                        Log.d("App", "✅ Saving city: ${textFieldInput.trim()}")
                                        dataStore.writeCity(textFieldInput.trim())
                                    }
                                    textFieldInput = ""
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                            PrimaryStats(homeData, currIcon, forecastData, formatter, sunriseZone, sunsetZone)
                            Spacer(modifier = Modifier.height(120.dp))
                            SecondaryStats(homeData, aqiName, aqiIndex, formatter, updatedOnTime)
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            composable(Screen.Forecast.route) {
                AnimatedContent(
                    targetState = homeData != WeatherApi.dummyData(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ForecastAnim"
                ) { ready ->
                    if (ready) {
                        Forecast(forecastData, state, refreshing)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            composable(Screen.Settings.route) {
                Settings(darkModeState, dataStore)
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Forecast(data: WeatherApi.ForecastJsonData, state: PullRefreshState, refreshing: Boolean) {
    val icons by remember {
        mutableStateOf(mutableListOf(R.drawable._01d, R.drawable._01d, R.drawable._01d, R.drawable._01d, R.drawable._01d))
    }

    val dayString by remember {
        mutableStateOf(mutableListOf("Friday", "Friday", "Friday", "Friday", "Friday"))
    }

    LaunchedEffect(Unit) {
        for ((i, j) in (0..< icons.size).withIndex()) {
            // Setup days
            val day = ZonedDateTime.ofInstant(Instant.ofEpochSecond(data.list[i * 8].dt.toLong()), ZoneOffset.UTC).dayOfWeek
            dayString[j] = day.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
            // Update weather Icons
            val iconCode = data.list[i * 8].weather.firstOrNull()?.icon ?: "01d"
            when (iconCode) {
                "01d", "01n" -> icons[j] = R.drawable._01d
                "02d", "02n" -> icons[j] = R.drawable._02d
                "03d", "03n" -> icons[j] = R.drawable._03d
                "04d", "04n" -> icons[j] = R.drawable._04d
                "09d", "09n" -> icons[j] = R.drawable._09d
                "10d", "10n" -> icons[j] = R.drawable._10d
                "11d", "11n" -> icons[j] = R.drawable._11d
                "13d", "13n" -> icons[j] = R.drawable._13d
                "50d", "50n" -> icons[j] = R.drawable._50d
                else -> icons[j] = R.drawable._01d
            }

        }
    }

    Column(modifier = Modifier
        .background(MaterialTheme.colorScheme.surface)
        .fillMaxSize()
        .pullRefresh(state)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally) {

        PullRefreshIndicator(refreshing = refreshing, state = state,
            Modifier
                .align(Alignment.CenterHorizontally)
                .zIndex(1f),
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary)
        for (j in 0..< icons.size) {
            ForecastItem(data, icons[j], j * 8, dayString[j])
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ForecastItem(data: WeatherApi.ForecastJsonData, icon: Int, it: Int, day: String) {
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .height(150.dp)) {
        Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)) {
                Image(painter = painterResource(id = icon),
                    contentDescription = "Weather", modifier = Modifier.size(60.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(day, fontSize = MaterialTheme.typography.headlineSmall.fontSize, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.weight(1f))
                Text(data.list[it].main.temp.roundToInt().toString(),
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("°C", style = TextStyle(
                    fontSize = 15.sp), modifier = Modifier
                    .align(Alignment.Top)
                    .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.weight(.1f))
            }
            HorizontalDivider(
                modifier = Modifier.padding(5.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)) {
                Image(painterResource(id = R.drawable.thermometer), contentDescription = "real feel",
                    modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = data.list[it].main.feelsLike.roundToInt().toString() + "°", color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(20.dp))
                Image(painterResource(id = R.drawable.humidity), contentDescription = "humidity",
                    modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = data.list[it].main.humidity.toString() + "%", color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(20.dp))
                Image(painterResource(id = R.drawable.compass), contentDescription = "wind dir",
                    modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = data.list[it].wind.deg.toString() + "°", color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(20.dp))
                Image(painterResource(id = R.drawable.speed), contentDescription = "wind speed",
                    modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = data.list[it].wind.speed.toInt().toString() + "m/s", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun Settings(checked: Boolean, dataStore: DataStore) {
    Column(modifier = Modifier
        .background(MaterialTheme.colorScheme.surface)
        .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Spacer(modifier = Modifier.height(30.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark Mode", modifier = Modifier
                .weight(0.5F)
                .padding(start = 40.dp),
                style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = { runBlocking { dataStore.writeTheme(it) } },
                modifier = Modifier
                    .weight(0.5F)
                    .padding(start = 40.dp),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer,
                    checkedThumbColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Spacer(modifier = Modifier.height(30.dp))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Weatherly",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = "Data provided by OpenWeatherMap",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(data: WeatherApi.HomeJsonData, atSettings: Boolean) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = if (atSettings) "Settings" else data.name,
                style = MaterialTheme.typography.titleLarge)
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun BottomBar(navController: NavController, checkScreen: (Unit) -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = listOf(Screen.Main, Screen.Forecast, Screen.Settings)
    NavigationBar(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { screen ->
            NavigationBarItem(selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = { navController.navigate(screen.route)
                    checkScreen(Unit)}, icon = { Icon(
                imageVector = screen.icon,
                contentDescription = "Home")})
        }
    }
}

@Composable
fun InputText(input: String, onValueChange: (String) -> Unit, onDone: (Unit) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(shape = MaterialTheme.shapes.extraLarge,
        value = input,
        label = { Text(text = "Search a city..")},
        onValueChange = { onValueChange(it) },
        keyboardActions = KeyboardActions(onDone = {
            if (input.isNotEmpty()) {
                onDone(Unit)
            }
            focusManager.clearFocus()
        }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor =  MaterialTheme.colorScheme.tertiary,
            unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.tertiary,
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            focusedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer),
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onTertiaryContainer) })
}

@Composable
fun PrimaryStats(data: WeatherApi.HomeJsonData,
                 currIcon: Int,
                 data2: WeatherApi.ForecastJsonData,
                 formatter: DateTimeFormatter,
                 sunriseZone: ZonedDateTime,
                 sunsetZone: ZonedDateTime) {
    val day = ZonedDateTime.ofInstant(Instant.ofEpochSecond(data2.list[0].dt.toLong()), ZoneOffset.UTC).dayOfWeek
    val dayString = day.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(id = currIcon),
            contentDescription = "Weather",
            modifier = Modifier.size(70.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = "Today", fontSize = MaterialTheme.typography.displaySmall.fontSize)
            Text(text = dayString, fontSize = MaterialTheme.typography.titleMedium.fontSize)
        }
    }
    Spacer(modifier = Modifier.height(40.dp))
    Row {
        Spacer(modifier = Modifier.width(30.dp))
        Text(data.main.temp.roundToInt().toString(),
            style = TextStyle(
                fontSize = 70.sp,
                brush = Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary))),
            color = MaterialTheme.colorScheme.primary)
        Text("°C", style = TextStyle(
            fontSize = 30.sp,
            brush = Brush.linearGradient(
                colors = listOf(MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary))),
            color = MaterialTheme.colorScheme.primary)
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row {
        Text(text = data.weather[0].main, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = data.main.tempMin.roundToInt().toString() + "°",
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(5.dp))
        Text("|", color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = data.main.tempMax.roundToInt().toString() + "°",
            color = MaterialTheme.colorScheme.onSurface)
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row {
        Text(text = "Sunrise " + formatter.format(sunriseZone), color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "•", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "Sunset " + formatter.format(sunsetZone), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SecondaryStats(data: WeatherApi.HomeJsonData,
                   aqiName: String,
                   aqiIndex: Int,
                   formatter: DateTimeFormatter,
                   updatedOnTime: ZonedDateTime) {
    Row {
        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(0.3F)
                .padding(5.dp)) {
            Column(modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(id = R.drawable.thermometer), contentDescription = "thermometer",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Feels like " + data.main.feelsLike.roundToInt().toString() + "°", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(0.3F)
                .padding(5.dp)) {
            Column(modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(id = R.drawable.compass), contentDescription = "thermometer",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Wind direction " + data.wind.deg + "°", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    Row {
        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(0.3F)
                .padding(5.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.humidity), contentDescription = "humidity",
                        modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                    Text(text = "Humidity",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(text = data.main.humidity.toString() + "%",
                        modifier = Modifier
                            .padding(10.dp)
                            .weight(0.3F),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(5.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.speed), contentDescription = "speed",
                        modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                    Text(text = "Wind speed",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(text = data.wind.speed.toInt().toString() + "m/s",
                        modifier = Modifier
                            .padding(10.dp)
                            .weight(0.3F),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(5.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.pressure), contentDescription = "pressure",
                        modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                    Text(text = "Pressure",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(text = data.main.pressure.toString() + "hPa",
                        modifier = Modifier
                            .padding(10.dp)
                            .weight(0.3F),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(5.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.visibility), contentDescription = "visibility",
                        modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                    Text(text = "Visibility",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(text = (data.visibility / 1000).toString() + "km",
                        modifier = Modifier
                            .padding(10.dp)
                            .weight(0.3F),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(5.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.updated), contentDescription = "updated on",
                        modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                    Text(text = "Updated on",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(text =  formatter.format(updatedOnTime),
                        modifier = Modifier
                            .padding(10.dp)
                            .weight(0.3F),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }

            }
        }
    }
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(id = R.drawable.aqi), contentDescription = "aqi",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(30.dp))
            Text(text = "AQI $aqiIndex", color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp))
            Text(text = aqiName, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(10.dp)
                    .weight(0.3f),
                textAlign = TextAlign.End)
        }
    }
}

@Preview(showBackground = true, name = "Dark theme", uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true, name = "Light theme", uiMode = UI_MODE_NIGHT_NO)
@Composable
fun AppPreview() {
    val dataStore = DataStore(LocalContext.current)
    WeatherlyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            App(dataStore, true, "Athens")
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, name = "Dark theme", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ForecastPreview() {
    val state = rememberPullRefreshState(false, onRefresh = { })
    WeatherlyTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            Forecast(data = WeatherApi.forecastDummyData(), state, false)
        }
    }
}