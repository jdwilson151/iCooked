package com.badhabbit.icooked.repository

import android.content.Context
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.badhabbit.icooked.datalayer.CartItem
import com.badhabbit.icooked.datalayer.Recipe
import com.badhabbit.icooked.datalayer.extension
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object RecipeBox {
    private val gson = Gson()
    private val readwriteMutex = Mutex()
    private var recipes = HashMap<String,Recipe>()
    val units = listOf(
        "",
        "pt.",
        "qt.",
        "gal.",
        "oz.",
        "lbs.",
        "cans",
        "bags",
        "boxes"
    )

    suspend fun updateBox(context: Context) {
        readwriteMutex.withLock{
            Thread {
                val recipesList = context.fileList() ?: arrayOf()
                recipesList.filter { it.startsWith(extension) }.forEach{
                    recipes[it.removePrefix(extension)] = getRecipe(context,it)
                }
            }.start()
        }
    }

    private suspend fun fetchRecipes():HashMap<String,Recipe> {
        var returnMap:HashMap<String,Recipe>
        readwriteMutex.withLock {
            returnMap = recipes
        }
        return returnMap
    }

    private suspend fun updateRecipe(context: Context, recipe: Recipe) {
        readwriteMutex.withLock {
            try {
                recipes.set(recipe.name, recipe)
                CoroutineScope(Dispatchers.IO).launch {
                    FileHandler.writeFile(context,recipe,recipe.filename)
                }
            }catch(e: Exception) {
                Log.d("debug","Error: ${e.message}")
            }
        }
    }

    private suspend fun deleteRecipe(context: Context, recipe: Recipe) {
        readwriteMutex.withLock {
            try {
                recipes.remove(recipe.name)
                CoroutineScope(Dispatchers.IO).launch {
                    FileHandler.deleteFile(context,recipe.filename)
                }
            }catch(e: Exception) {
                Log.d("debug","Error: ${e.message}")
            }
        }
    }

    fun getRecipeList(context: Context, recipeList: SnapshotStateList<Recipe>) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                fetchRecipes().values.sortedBy{it.name}.forEach{recipeList.add(it)}
            }
        } catch (e: Exception) {
            Log.d("Debugging", "getRecipes: ${e.message}")
        }
    }

    fun getRecipe(context: Context, filename: String): Recipe {
        Mutex().withLock {
            val sType = object : TypeToken<Recipe>() {}.type
            lateinit var returnRecipe: Recipe
            withContext(Dispatchers.IO) {
                try {
                    val bufferText = FileHandler.readFile(context, filename)
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
        FileHandler.writeFile(context, recipe, recipe.filename)
    }

}