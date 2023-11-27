package com.example.hw2

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Size
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hw2.ui.theme.HW2Theme
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.ImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Query
import kotlin.coroutines.*
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                //CoilGifList("zgm4VU9DxCVLPREbMrOThm98AjYljOKf", "gym")
                SearchableCoilGifList(apiKey = "zgm4VU9DxCVLPREbMrOThm98AjYljOKf")
            }
        }
    }
}

@Composable
fun NetworkConnectionListener(
    onConnectionChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onConnectionChanged(true)
        }

        override fun onLost(network: Network) {
            onConnectionChanged(false)
        }
    }
    DisposableEffect(Unit) {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        onDispose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableCoilGifList(apiKey: String) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var executeSearch by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    executeSearch = !executeSearch
                },
                modifier = Modifier.padding(start = 8.dp)

            ) {
                Text("Поиск")
            }
        }

        if (executeSearch) {
            CoilGifList(apiKey, searchQuery)
        }
    }
}


fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        else -> false
    }
}

interface GiphyApiService {
    @GET("v1/gifs/search")
    suspend fun searchGifs(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): GiphyResponse
}

data class GiphyResponse(val data: List<GifData>)
data class GifData(val images: GifImages)
data class GifImages(val original: GifOriginal)
//data class GifOriginal(val url: String)
data class GifOriginal(val url: String, val width: String, val height: String)
data class GifMetadata(val url: String, val width: String, val height: String)

//suspend fun getGiphyGifs(apiKey: String, query: String, limit: Int, offset: Int): List<String> {
//    val retrofit = Retrofit.Builder()
//        .baseUrl("https://api.giphy.com/")
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//
//    val service = retrofit.create(GiphyApiService::class.java)
//    return try {
//        val response = service.searchGifs(apiKey, query, limit, offset)
//        response.data.map { it.images.original.url }
//    } catch (e: Exception) {
//        emptyList()
//    }
//}
suspend fun getGiphyGifs(apiKey: String, query: String, limit: Int, offset: Int): List<GifMetadata> {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.giphy.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(GiphyApiService::class.java)
    return try {
        val response = service.searchGifs(apiKey, query, limit, offset)
        response.data.map { GifMetadata(it.images.original.url, it.images.original.width, it.images.original.height) }
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun CoilGifList(apiKey: String, search: String) {
    val context = LocalContext.current
    val wasLoaded = rememberSaveable { mutableStateOf(false) }
    val imageCount = rememberSaveable { mutableIntStateOf(20) }
    var gifUrls = rememberSaveable { mutableStateOf<List<GifMetadata>>(listOf()) }
    val listState = rememberLazyGridState()
    var isConnected by rememberSaveable { mutableStateOf(isInternetAvailable(context = context)) }
    val loadedImagesCount = rememberSaveable { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var connected by rememberSaveable { mutableStateOf(true) }
    var lostConnection by rememberSaveable { mutableStateOf(false) }
    NetworkConnectionListener { connected = it }

    fun reloadImages() {
        coroutineScope.launch {
            if (isInternetAvailable(context)) {
                isConnected = true
                val newGifs = getGiphyGifs(
                    apiKey,
                    search,
                    imageCount.intValue - loadedImagesCount.intValue,
                    loadedImagesCount.intValue
                )
                gifUrls.value = gifUrls.value + newGifs
                loadedImagesCount.intValue = imageCount.intValue
            }
        }
    }

//    LaunchedEffect(connectionLost){
//        if (!isInternetAvailable(context)){
//            isConnected = false
//        }
//    }
    LaunchedEffect(connected) {
        if (!connected) {
            lostConnection = true
        }
    }

    if (lostConnection) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = "Соедниенение потеряно")
            Button(onClick = {
                if (isInternetAvailable(context)) {
                    reloadImages()
                    lostConnection = false
                }

            }) {
                Text("Попробовать снова")
            }
        }
    }

    if (!isConnected) {
        Button(onClick = { reloadImages() }) {
            Text("Попробовать снова")
        }
    } else {
        if (!wasLoaded.value) {
            imageCount.intValue = 20
            wasLoaded.value = true
        }

        LaunchedEffect(imageCount.intValue) {
            if (isInternetAvailable(context = context)) {
                val newGifs = getGiphyGifs(
                    apiKey,
                    search,
                    imageCount.intValue - loadedImagesCount.intValue,
                    loadedImagesCount.intValue
                )
                gifUrls.value = gifUrls.value + newGifs
                loadedImagesCount.intValue = imageCount.intValue
            }
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .collect { visibleItems ->
                    val lastVisibleItem = visibleItems.lastOrNull()
                    if (lastVisibleItem != null && lastVisibleItem.index >= imageCount.intValue - 1) {
                        if (isInternetAvailable(context = context)) {
                            imageCount.intValue += 20
                        }
                    }
                }
        }
        LazyVerticalGrid(
            state = listState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(gifUrls.value) { gifUrl ->
                //CoilGifImage(gifUrl.url, gifUrl.width, gifUrl.height)
                CoilGifImage(gifUrl.url)
            }
        }
        if (!connected) {
            Button(onClick = { reloadImages() }) {
                Text("Попробовать снова")
            }
        }
    }
}

//@Composable
//fun CoilGifList(apiKey: String, search: String) {  //Без учета размера изображения
//    val context = LocalContext.current
//    val wasLoaded = rememberSaveable { mutableStateOf(false) }
//    val imageCount = rememberSaveable { mutableIntStateOf(20) }
//    var gifUrls = rememberSaveable { mutableStateOf<List<String>>(listOf()) }
//    val listState = rememberLazyGridState()
//    var isConnected by rememberSaveable { mutableStateOf(isInternetAvailable(context = context)) }
//    val loadedImagesCount = rememberSaveable { mutableIntStateOf(0) }
//    val coroutineScope = rememberCoroutineScope()
//    var connected by rememberSaveable { mutableStateOf(true) }
//    var lostConnection by rememberSaveable { mutableStateOf(false) }
//    NetworkConnectionListener { connected = it }
//
//    fun reloadImages() {
//        coroutineScope.launch {
//            if (isInternetAvailable(context)) {
//                isConnected = true
//                val newGifs = getGiphyGifs(
//                    apiKey,
//                    search,
//                    imageCount.intValue - loadedImagesCount.intValue,
//                    loadedImagesCount.intValue
//                )
//                gifUrls.value = gifUrls.value + newGifs
//                loadedImagesCount.intValue = imageCount.intValue
//            }
//        }
//    }
//
////    LaunchedEffect(connectionLost){
////        if (!isInternetAvailable(context)){
////            isConnected = false
////        }
////    }
//    LaunchedEffect(connected) {
//        if (!connected) {
//            lostConnection = true
//        }
//    }
//
//    if (lostConnection) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//        ) {
//            Text(text = "Соедниенение потеряно")
//            Button(onClick = {
//                if (isInternetAvailable(context)) {
//                    reloadImages()
//                    lostConnection = false
//                }
//
//            }) {
//                Text("Попробовать снова")
//            }
//        }
//    }
//
//    if (!isConnected) {
//        Button(onClick = { reloadImages() }) {
//            Text("Попробовать снова")
//        }
//    } else {
//        if (!wasLoaded.value) {
//            imageCount.intValue = 20
//            wasLoaded.value = true
//        }
//
//        LaunchedEffect(imageCount.intValue) {
//            if (isInternetAvailable(context = context)) {
//                val newGifs = getGiphyGifs(
//                    apiKey,
//                    search,
//                    imageCount.intValue - loadedImagesCount.intValue,
//                    loadedImagesCount.intValue
//                )
//                gifUrls.value = gifUrls.value + newGifs
//                loadedImagesCount.intValue = imageCount.intValue
//            }
//        }
//
//        LaunchedEffect(listState) {
//            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
//                .collect { visibleItems ->
//                    val lastVisibleItem = visibleItems.lastOrNull()
//                    if (lastVisibleItem != null && lastVisibleItem.index >= imageCount.intValue - 1) {
//                        if (isInternetAvailable(context = context)) {
//                            imageCount.intValue += 20
//                        }
//                    }
//                }
//        }
//        LazyVerticalGrid(
//            state = listState,
//            columns = GridCells.Fixed(2),
//            contentPadding = PaddingValues(8.dp)
//        ) {
//            items(gifUrls.value) { gifUrl ->
//                CoilGifImage(gifUrl)
//            }
//        }
//        if (!connected) {
//            Button(onClick = { reloadImages() }) {
//                Text("Попробовать снова")
//            }
//        }
//    }
//}

////////////////////////////////////////////////////////////////////////////////////////////////

//@Composable
//fun CoilGifList(apiKey: String) {
//    val context = LocalContext.current
//
//    val wasLoaded = rememberSaveable {mutableStateOf(false)}
//
//    val imageCount = rememberSaveable { mutableIntStateOf(0) }
//    var gifUrls = rememberSaveable { mutableStateOf<List<String>>(listOf()) }
//    val listState = rememberLazyGridState()
//    var isConnected by rememberSaveable { mutableStateOf(isInternetAvailable(context = context))}
//    val loadedImagesCount = rememberSaveable { mutableIntStateOf(0) }
//
////    LaunchedEffect(isConnected){
////        isConnected = isInternetAvailable(context)
////        if (isConnected && !wasLoaded.value){
////            val newGifs = getGiphyGifs(
////                apiKey,
////                "dogs",
////                20,
////                0
////            )
////            gifUrls.value = gifUrls.value + newGifs
////            loadedImagesCount.intValue = imageCount.intValue
////            wasLoaded.value = true
////        }
////    }
//
//    LaunchedEffect(imageCount.intValue) {
//        if (isInternetAvailable(context = context)) {
//            val newGifs = getGiphyGifs(
//                apiKey,
//                "dogs",
//                imageCount.intValue - loadedImagesCount.intValue,
//                loadedImagesCount.intValue
//            )
//            gifUrls.value = gifUrls.value + newGifs
//            loadedImagesCount.intValue = imageCount.intValue
//        }
//    }
//
//    LaunchedEffect(listState) {
//        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
//            .collect { visibleItems ->
//                val lastVisibleItem = visibleItems.lastOrNull()
//                if (lastVisibleItem != null && lastVisibleItem.index >= imageCount.intValue - 1) {
//                    if (isInternetAvailable(context = context)) {
//                        imageCount.intValue += 20
//                    }
//                }
//            }
//    }
//    LazyVerticalGrid(
//        state = listState,
//        columns = GridCells.Fixed(2),
//        contentPadding = PaddingValues(8.dp)
//    ) {
//        items(gifUrls.value) { gifUrl ->
//            CoilGifImage(gifUrl)
//        }
//    }
//}


//@Composable
//fun CoilGifList() {
//    val gifUrls = mutableListOf(
//        "https://cdn.britannica.com/25/7125-050-67ACEC3C/Abyssinian-sorrel.jpg",
//        "https://cdn.britannica.com/39/7139-050-A88818BB/Himalayan-chocolate-point.jpg",
//        "https://cataas.com/cat"
//    )
//    repeat(20) { gifUrls.add("https://cataas.com/cat") }
//    LazyVerticalGrid(
//        GridCells.Fixed(2),
//    ) {
//        items(gifUrls) { gifUrl ->
//            CoilGifImage(gifUrl)
//        }
//    }
//}

//@Composable
//fun CoilGifList() {
//    val imageCount = remember { mutableIntStateOf(20) }
//
//    val listState = rememberLazyGridState()
//
//    LaunchedEffect(listState) {
//        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
//            .collect { visibleItems ->
//                val lastVisibleItem = visibleItems.lastOrNull()
//                if (lastVisibleItem != null && lastVisibleItem.index >= imageCount.intValue - 1) {
//                    imageCount.intValue += 20
//                }
//            }
//    }
//    LazyVerticalGrid(
//        state = listState,
//        columns = GridCells.Fixed(2),
//        contentPadding = PaddingValues(8.dp)
//    ) {
//        items(imageCount.intValue) {
//            CoilGifImage("https://cataas.com/cat")
//        }
//    }
//}

@Composable
fun CoilGifImage(gifUrl: String, width: String, height: String) { //Попытка сделать так, чтобы учитывались размеры изображения
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(width.toFloat() / height.toFloat()),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val imageLoader = ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(context).data(data = gifUrl).apply(block = {
            })
                .build(), imageLoader = imageLoader
        )

        if (painter.state is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(50.dp)
                    .padding(16.dp)
            )
        }
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),

            )
    }
}

@Composable
fun CoilGifImage(gifUrl: String) {  //без учета размеров изображения
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val imageLoader = ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(context).data(data = gifUrl).apply(block = {
            })
                .build(), imageLoader = imageLoader
        )

        if (painter.state is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(50.dp)
                    .padding(16.dp)
            )
        }
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),

            )
    }
}
