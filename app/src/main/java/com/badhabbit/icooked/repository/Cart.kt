package com.badhabbit.icooked.repository

import android.content.Context
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
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
                var tempCart:MutableList<CartItem> = Gson().fromJson(bufferText, sType)

                list.clear()
                list.addAll(tempCart.sortedBy {it.index})
            }catch(e: Exception) {
                Log.d("debug","Error: ${e.message}")
            }
        }
    }

    private suspend fun saveCart(context: Context) {
        readwriteMutex.withLock {
            FileHandler.writeFile(context, list, filename)
        }
    }

    suspend fun getList(cartList:SnapshotStateList<CartItem>) {
        readwriteMutex.withLock {
            try {
                var i = 0
                list.forEach {
                    it.index = i
                    i++
                }
                cartList.clear()
                cartList.addAll(list)
            }catch(e: Exception) {
                Log.d("Debugging","getList: ${e.message}")
            }
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
                withContext(Dispatchers.IO) {
                    saveCart(context)
                }
            } catch (e: Exception) {
                Log.d("Debugging", "deleteItem: ${e.message}")
            }
        }
    }
}