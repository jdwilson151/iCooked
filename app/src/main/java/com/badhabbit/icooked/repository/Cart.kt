package com.badhabbit.icooked.repository

import android.content.Context
import android.util.Log
import com.badhabbit.icooked.datalayer.CartItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object Cart {
    private val readwriteMutex = Mutex()
    val filename = "list_GroceryCart"
    private var list = mutableListOf<CartItem>()

    suspend fun updateCart(context: Context) {
        readwriteMutex.withLock {
            try {
                val sType = object : TypeToken<MutableList<CartItem>>() {}.type
                val bufferText = FileHandler.readFile(context, filename)
                val tempCart:MutableList<CartItem> = Gson().fromJson(bufferText, sType)

                list.clear()
                list.addAll(tempCart.sortedBy {it.index})
                Log.d("Debug","Cart updated")
            }catch(e: Exception) {
                Log.d("debug","Error: ${e.message}")
            }
        }
    }

    private suspend fun saveCart(context: Context) {
        FileHandler.writeFile(context, list, filename)
    }

    suspend fun getList():List<CartItem> {
        readwriteMutex.withLock {
            try {
                var i = 0
                list.forEach {
                    it.index = i
                    Log.d("Debug","${it.index}) ${it.itemName}")
                    i++
                }
                Log.d("Debug","Loaded Cart list")
            }catch(e: Exception) {
                Log.d("Debugging","getList: ${e.message}")
            }
            return list
        }
    }

    suspend fun newItem(context: Context, name:String) {
        readwriteMutex.withLock {
            try {
                var rand = (1..10000).random()
                var checkList = list.filter { it.id == rand }
                while (checkList.isNotEmpty()) {
                    rand = (1..10000).random()
                    checkList = list.filter { it.id == rand }
                }
                list.add(CartItem(rand, name,index = list.size))
                CoroutineScope(Dispatchers.IO).launch {
                    saveCart(context)
                }
            } catch (e: Exception) {
                Log.d("Debugging", "newItem: ${e.message}")
            }
        }
    }

    suspend fun updateItem(context: Context, item: CartItem) {
        readwriteMutex.withLock {
            try {
                val index = list.indexOf(list.first { it.id == item.id })
                list[index] = item
                list.sortBy {it.index}
                CoroutineScope(Dispatchers.IO).launch {
                    saveCart(context)
                }
            } catch (e: Exception) {
                Log.d("Debugging", "updateItem: ${e.message}")
            }
        }
    }

    suspend fun updateList(context: Context, undoneList: List<CartItem>, doneList: List<CartItem>) {
        readwriteMutex.withLock {
            try {
                list.clear()
                list.addAll(undoneList)
                list.addAll(doneList)
                list.forEach {
                    it.index = list.indexOf(it)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    saveCart(context)
                }
            }catch(e: Exception) {
                Log.d("Debug","updateList error: ${e.message}")
            }
        }
    }

    suspend fun deleteItem(context: Context, item: CartItem) {
        readwriteMutex.withLock {
            try {
                val index = list.indexOf(list.first { it.id == item.id })
                list.removeAt(index)
                var i = 0
                list.forEach {
                    it.index = i
                    i++
                }
                Log.d("Debug","Removed ${item.itemName} from $index")
                withContext(Dispatchers.IO) {
                    saveCart(context)
                }
            } catch (e: Exception) {
                Log.d("Debugging", "deleteItem: ${e.message}")
            }
        }
    }
}