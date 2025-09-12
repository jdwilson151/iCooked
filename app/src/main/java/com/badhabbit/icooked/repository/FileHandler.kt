package com.badhabbit.icooked.repository

import android.content.Context
import android.util.Log
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
            val data: String = gson.toJson(file)
            Log.d("Debugging", "Writing: $data")
            context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(data.toByteArray())
            }
        }
    }

    suspend fun deleteFile(context: Context,filename: String) {
        Mutex().withLock {
            context.deleteFile(filename)
        }
    }
}