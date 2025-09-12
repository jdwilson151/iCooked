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
    val filename = "list_GroceryCart"
    private var list = mutableListOf<CartItem>()

    suspend fun updateCart(context: Context) {
        Mutex().withLock {
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

    private suspend fun saveCart(context: Context, list: List<CartItem>) {
        FileHandler.writeFile(context, list, Cart.filename)
    }

    suspend fun getList(cartList:SnapshotStateList<CartItem>) {
        Mutex().withLock {
            try {
                cartList.clear()
                cartList.addAll(list)
            }catch(e: Exception) {
                Log.d("Debugging","getList: ${e.message}")
            }
        }
    }

    suspend fun newItem(context: Context, name:String) {
        Mutex().withLock {
            try {
                var rand = (1..10000).random()
                var checkList = list.filter { it.id == rand }
                while (checkList.isNotEmpty()) {
                    rand = (1..10000).random()
                    checkList = list.filter { it.id == rand }
                }
                list.add(CartItem(rand, name))
                CoroutineScope(Dispatchers.IO).launch {
                    saveCart(context, list)
                }
            } catch (e: Exception) {
                Log.d("Debugging", "newItem: ${e.message}")
            }
        }
    }

    suspend fun updateItem(context: Context, item: CartItem) {
        Mutex().withLock {
            try {
                val index = list.indexOf(list.first { it.id == item.id })
                list[index] = item
                CoroutineScope(Dispatchers.IO).launch {
                    saveCart(context, list)
                }
            } catch (e: Exception) {
                Log.d("Debugging", "updateItem: ${e.message}")
            }
        }
    }

    suspend fun deleteItem(context: Context, item: CartItem) {
        Mutex().withLock {
            try {
                val index = list.indexOf(list.first { it.id == item.id })
                list.removeAt(index)
                withContext(Dispatchers.IO) {
                    saveCart(context, list)
                }
            } catch (e: Exception) {
                Log.d("Debugging", "deleteItem: ${e.message}")
            }
        }
    }
}