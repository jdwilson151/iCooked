package com.badhabbit.icooked.repository

import android.content.Context
import android.util.Log
import com.badhabbit.icooked.datalayer.CartItem
import com.badhabbit.icooked.datalayer.Recipe
import com.badhabbit.icooked.datalayer.extension
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object Repository {
    private val gson = Gson()
    private val readwriteMutex = Mutex()
    //Comment thingy goes here
    private val garbage = 0

    private suspend fun readFile(context: Context, filename: String):String {
        readwriteMutex.withLock {
            lateinit var bufferText: String
            withContext(Dispatchers.IO) {
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
            }
            return bufferText
        }
    }

    private suspend fun writeFile(context: Context, file: Any, filename: String) {
        readwriteMutex.withLock {
            val data: String = gson.toJson(file)
            Log.d("Debugging", "Writing: $data")
            context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(data.toByteArray())
            }
        }
    }

    suspend fun getCart(context: Context): MutableList<CartItem> {
        Mutex().withLock {
            lateinit var returnList: MutableList<CartItem>
            withContext(Dispatchers.IO) {
                val sType = object : TypeToken<MutableList<CartItem>>() {}.type
                val bufferText = readFile(context, Cart.filename)
                try {
                    returnList = gson.fromJson(bufferText, sType)
                } catch (e: Exception) {
                    Log.d("Debugging", "getCart: ${e.message}")
                    returnList = mutableListOf()
                }
            }
            return returnList
        }
    }
    suspend fun saveCart(context: Context, list: List<CartItem>) {
        writeFile(context, list, Cart.filename)
    }

    suspend fun getRecipes(context: Context): List<String> {
        Mutex().withLock {
            lateinit var returnList: List<String>
            withContext(Dispatchers.IO) {
                try {
                    val recipesList = context.fileList() ?: arrayOf()
                    returnList = recipesList
                        .filter { it.startsWith(extension) }
                } catch (e: Exception) {
                    Log.d("Debugging", "getRecipes: ${e.message}")
                    returnList = listOf()
                }
            }
            return returnList
        }
    }

    suspend fun getRecipe(context: Context, filename: String): Recipe {
        Mutex().withLock {
            val sType = object : TypeToken<Recipe>() {}.type
            lateinit var returnRecipe: Recipe
            withContext(Dispatchers.IO) {
                try {
                    val bufferText = readFile(context, filename)
                    returnRecipe = gson.fromJson(bufferText, sType)
                } catch (e: Exception) {
                    Log.d("Debugging", "getRecipe: ${e.message}")
                    returnRecipe = Recipe("ERROR")
                }
            }
            return returnRecipe
        }
    }

    suspend fun newRecipe(context: Context,recipe: Recipe) {
        Mutex().withLock {
            withContext(Dispatchers.IO) {
                try {
                    val fileList = context.fileList() ?: arrayOf()
                    var filterList = fileList.filter { it.equals(recipe.filename) }
                    while (filterList.isNotEmpty()) {
                        recipe.filename = "$extension${recipe.name}${(1..100).random()}"
                        filterList = fileList.filter { it.equals(recipe.filename) }
                    }
                    saveRecipe(context, recipe)
                } catch (e: Exception) {
                    Log.d("Debugging", "newRecipe: ${e.message}")
                }
            }
        }
    }
    suspend fun saveRecipe(context: Context, recipe: Recipe) {
        writeFile(context, recipe, recipe.filename)
    }

    suspend fun deleteRecipe(context: Context, recipe: Recipe) {
        Mutex().withLock {
            withContext(Dispatchers.IO) {
                try {
                    context.deleteFile(recipe.filename)
                } catch (e: Exception) {
                    Log.d("Debugging", "deleteRecipe: ${e.message}")
                }
            }
        }
    }
}