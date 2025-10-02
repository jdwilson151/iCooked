package com.badhabbit.icooked.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FileHandler {
    private val gson = Gson()
    suspend fun readFile(context: Context, filename: String):String {
        Mutex().withLock {
            lateinit var bufferText: String
            //withContext(Dispatchers.IO) {
                try {
                    context.openFileInput(filename)
                        .bufferedReader().useLines { lines ->
                            bufferText = lines.fold("") { some, text ->
                                "$some\n$text"
                            }
                        }
                    Log.d("Debugging", "Buffer: $bufferText")
                } catch (e: Exception) {
                    Log.d("Debugging", "readFile: ${e.message}")
                    bufferText = ""
                }
            //}
            return bufferText
        }
    }

    suspend fun writeFile(context: Context, file: Any, filename: String) {
        Mutex().withLock {
            try {
                val data: String = gson.toJson(file)
                Log.d("Debugging", "Writing: $data")
                context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                    it.write(data.toByteArray())
                }
            }catch(e:Exception) {
                Log.d("debug","Writing Error: ${e.message} Filename: $filename")
            }
        }
    }

    suspend fun deleteFile(context: Context,filename: String) {
        Mutex().withLock {
            context.deleteFile(filename)
        }
    }

    suspend fun shareString(context: Context, string: String) {
        Mutex().withLock {
            val shareRecipe = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT,string)
                type = "text/plain"
            }
            ContextCompat.startActivity(context, Intent.createChooser(shareRecipe, null), null)
        }
    }
}