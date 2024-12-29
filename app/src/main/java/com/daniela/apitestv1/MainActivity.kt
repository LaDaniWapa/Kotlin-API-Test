package com.daniela.apitestv1

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.daniela.apitestv1.api.MyApi
import com.daniela.apitestv1.ui.theme.APITestV1Theme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            APITestV1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val getContent = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> selectedImageUri = uri }
    )

    var str = remember { mutableStateOf("") }
    val ctx = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = str.value, fontSize = 25.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { getContent.launch("image/*") },
                content = { Text("Select Image") }
            )
            Button(

                onClick = {
                    if (selectedImageUri != null)
                        str.value = "Loading..."

                        sendImageTestRequest(
                            selectedImageUri!!,
                            ctx,
                            str
                        )
                },
                content = { Text("Test Image") }
            )
        }

        selectedImageUri?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

    }
}

fun sendImageTestRequest(imgUri: Uri, context: Context, str: MutableState<String>) {
    val api = Retrofit.Builder()
        .baseUrl("https://api.ladaniwapa.es") // API's base URL
        .addConverterFactory(GsonConverterFactory.create()) // Gson handles JSON parsing
        .build()
        .create(MyApi::class.java)

    val contentResolver = context.contentResolver

    // Prepare the file from URI
    val inputStream = contentResolver.openInputStream(imgUri)
    if (inputStream == null) {
        Log.e("FileError", "File could not be opened from URI")
        str.value = "File error"
        return
    }

    val tempFile = File(context.cacheDir, "temp_image.jpg")
    inputStream.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

    // Send the request
    GlobalScope.launch {
        try {
            val response = api.uploadImage(filePart)
            if (response.isSuccessful) {
                val result = response.body()
                str.value = "SFW: ${result?.sfwProbability}, NSFW: ${result?.nsfwProbability}"
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("APIError", "Error: $errorBody, Code: ${response.code()}")
                str.value = "Error: $errorBody"
            }
        } catch (e: Exception) {
            Log.e("APIError", "Exception: ${e.message}")
            str.value = "Request failed: ${e.message}"
        }
    }
}


fun sendIndexRequest(str: MutableState<String>) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.ladaniwapa.es")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(MyApi::class.java)

    val call: Call<String?>? = api.getIndex();

    call!!.enqueue(object : Callback<String?> {
        override fun onResponse(call: Call<String?>, response: Response<String?>) {
            Log.d(":::TAG", "Success: " + response.body().toString())
            str.value = response.body().toString()
        }

        override fun onFailure(call: Call<String?>, t: Throwable) {
            Log.d(":::TAG", "Failure: " + t.message)
            str.value = t.message.toString()
        }
    })

}