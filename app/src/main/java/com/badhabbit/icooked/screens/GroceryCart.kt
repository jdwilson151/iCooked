package com.badhabbit.icooked.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.badhabbit.icooked.datalayer.CartItem
import com.badhabbit.icooked.repository.Cart
import com.badhabbit.icooked.repository.RecipeBox
import kotlinx.coroutines.launch


@Composable
fun GroceryCart(
    contentPadding: PaddingValues,
    onTitleChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val itemList = remember { mutableStateListOf<CartItem>()}
    val scope = rememberCoroutineScope()

    val onSave:(CartItem) -> Unit = { newItem: CartItem ->
        scope.launch {
            Cart.updateItem(context, newItem)
            Cart.getList(itemList)
        }
    }
    val onCheckbox: (Boolean, CartItem) -> Unit = { checked: Boolean, item: CartItem ->
        item.done = checked
        scope.launch {
            Cart.updateItem(context, item)
            Cart.getList(itemList)
        }
    }
    val onDelete: (item: CartItem) -> Unit = {
        scope.launch {
            Cart.deleteItem(context, it)
            Cart.getList(itemList)
        }
    }

    val listState = rememberLazyListState()
    var delta: Float by remember { mutableFloatStateOf(0f) }
    var draggingItem: LazyListItemInfo? by remember { mutableStateOf(null)}
    var draggingItemIndex: Int? by remember { mutableStateOf(null)}
    val onMoveUndone = { fromIndex: Int, toIndex: Int ->
        itemList.apply { add(toIndex, removeAt(fromIndex)) }
    }

    LaunchedEffect(context) {
        Cart.updateCart(context)
        Cart.getList(itemList)
    }
    onTitleChanged("- Grocery Cart")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .pointerInput(key1 = listState) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { item ->
                                    offset.y.toInt() in item.offset..(item.offset + item.size)
                                }
                                ?.also {
                                    (it.contentType as? DraggableItem)?.let { draggableItem ->
                                        draggingItem = it
                                        draggingItemIndex = draggableItem.index
                                    }
                                }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            delta += dragAmount.y

                            val currentIndex =
                                draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                            val currentItem =
                                draggingItem ?: return@detectDragGesturesAfterLongPress

                            val startOffset = currentItem.offset + delta
                            val endOffset = currentItem.offset + currentItem.size + delta
                            val middleOffset = startOffset + (endOffset - startOffset) / 2

                            val targetItem =
                                listState.layoutInfo.visibleItemsInfo.find { item ->
                                    middleOffset.toInt() in item.offset..item.offset + item.size &&
                                            currentItem.index != item.index &&
                                            item.contentType is DraggableItem
                                }

                            if (targetItem != null) {
                                val targetIndex = (targetItem.contentType as DraggableItem).index
                                onMoveUndone(currentIndex, targetIndex)
                                draggingItemIndex = targetIndex
                                draggingItem = targetItem
                                delta += currentItem.offset - targetItem.offset
                            }
                        },
                        onDragEnd = {
                            draggingItemIndex = null
                            delta = 0f
                            scope.launch {
                                itemList.filter { !it.done }.forEach { item: CartItem ->
                                    val index =
                                        itemList.indexOf(itemList.first { it.id == item.id })
                                    val newitem = item.copy(index = index)
                                    Cart.updateItem(context, newitem)
                                }
                            }
                        },
                        onDragCancel = {
                            draggingItemIndex = null
                            delta = 0f
                        }
                    )
                }
        ) {
            item {
                if (itemList.any { !it.done }) {
                    Text(
                        text = "PENDING",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    if(itemList.none { it.done }) {
                        Text(
                            text = "No items to display",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            itemsIndexed(
                items = itemList.filter { !it.done },
                key = {_,it -> it.hashCode()},
                contentType = { index, _ -> DraggableItem(index = index)}
            ) { index: Int, item: CartItem ->
                /*scope.launch {
                    item.index = index
                }*/
                ItemCard(
                    item = item,
                    show = remember { mutableStateOf(false)},
                    editMode = remember { mutableStateOf(false)},
                    onSave = onSave,
                    onCheckbox = {checked:Boolean ->
                        itemList[index] = item.copy(done = checked)
                        onCheckbox(checked,item)
                    },
                    onDelete = {onDelete(item)},
                    modifier = if(draggingItemIndex == index) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer {
                                translationY = delta
                            }
                    }else{
                        Modifier
                    }
                )
            }
            item {
                if (itemList.any { it.done }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPLETED",
                            modifier = Modifier
                                .padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Card(
                            modifier = Modifier
                                .size(width = 60.dp, height = 40.dp)
                                .padding(horizontal = 0.dp, vertical = 4.dp)
                                .clickable(onClick = {
                                    while (itemList.any { it.done }) {
                                        val item = itemList.filter { it.done }[0]
                                        scope.launch {
                                            Cart.deleteItem(context, item)
                                        }
                                        itemList.remove(item)
                                    }
                                }),
                            colors = CardDefaults.cardColors(
                                MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .sizeIn(minHeight = 20.dp),
                                contentDescription = "Delete",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
            itemsIndexed(
                itemList.filter { it.done },
                {_,it -> it.hashCode()}
            ) { index: Int, item: CartItem ->
                ItemCard(
                    item = item,
                    show = remember { mutableStateOf(false)},
                    editMode = remember { mutableStateOf(false)},
                    onSave = onSave,
                    onCheckbox = {checked:Boolean ->
                        itemList[index] = item.copy(done = checked)
                        onCheckbox(checked, item)
                    },
                    onDelete = {onDelete(item)}
                )
            }
        }
    }
}

@Composable
fun ItemCard(
    item: CartItem,
    show: MutableState<Boolean>,
    editMode: MutableState<Boolean>,
    onSave: (CartItem) -> Unit,
    onCheckbox: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconSize = 20.dp
    val containerColor =
        if(item.done)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.tertiaryContainer
    val textColor =
        if(item.done)
            MaterialTheme.colorScheme.onSecondary
        else
            MaterialTheme.colorScheme.onTertiaryContainer
    val buttonColor =
        if(item.done)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.secondaryContainer
    val tempItem = remember { mutableStateOf(item)}
    var ddExpanded by remember { mutableStateOf(false)}

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 2.dp)
            .clickable(onClick = {
                show.value = !show.value
                if (editMode.value) onSave(tempItem.value)
                editMode.value = false
            }),
        colors = CardDefaults.cardColors(containerColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(0.dp)
                    .weight(6f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = {
                                onCheckbox(it)
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .sizeIn(maxWidth = iconSize, maxHeight = iconSize),
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0x00000000),
                                uncheckedColor = textColor
                            )
                        )
                        if(!editMode.value) {
                            Text(
                                text = try {
                                        tempItem.value.itemName
                                    } catch (e: Exception) {
                                        Log.d("Debugging", "Miscast ${item.itemName}: ${e.message}")
                                        tempItem.value = item
                                        tempItem.value.itemName
                                    },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .sizeIn(maxWidth = 175.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            OutlinedTextField(
                                value = tempItem.value.itemName,
                                onValueChange = {
                                    tempItem.value = tempItem.value.copy(itemName = it)
                                },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .sizeIn(maxWidth = 275.dp),
                                maxLines = 1,
                                label = { Text("Item name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = textColor,
                                    focusedBorderColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedTextColor = textColor,
                                    unfocusedLabelColor = textColor,
                                    focusedLabelColor = textColor
                                )
                            )
                        }
                        if(item.qty.isNotEmpty() && !editMode.value) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                modifier = Modifier
                                    .padding(end = 4.dp),
                                text = "${item.qty} ${item.unit}",
                                color = textColor,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
                if (show.value) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if(editMode.value){
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    modifier = Modifier.weight(3f),
                                    value = tempItem.value.qty,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    maxLines = 1,
                                    label = { Text("qty") },
                                    onValueChange = {
                                        tempItem.value = tempItem.value.copy(qty = it)
                                        if(tempItem.value.qty.isEmpty())
                                            tempItem.value = tempItem.value.copy(unit = "")
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = textColor,
                                        focusedBorderColor = textColor,
                                        unfocusedTextColor = textColor,
                                        focusedTextColor = textColor,
                                        unfocusedLabelColor = textColor,
                                        focusedLabelColor = textColor
                                    )
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Column(
                                    Modifier
                                        .weight(3f),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "units",
                                        color = textColor
                                    )
                                    Card(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                onClick = { ddExpanded = true }
                                            ),
                                        colors = CardDefaults.cardColors(buttonColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = tempItem.value.unit,
                                                modifier = Modifier
                                            )
                                        }
                                    }
                                }
                                var position by remember { mutableStateOf(0f)}
                                DropdownMenu(
                                    modifier = Modifier
                                        .width(75.dp)
                                        .onGloballyPositioned {
                                            position = it.positionInParent().x + it.size.width
                                        },
                                    expanded = ddExpanded,
                                    onDismissRequest = {ddExpanded = !ddExpanded},
                                    offset = DpOffset(position.dp,0.dp)
                                ) {
                                    RecipeBox.units.forEach {it ->
                                        DropdownMenuItem(
                                            modifier = Modifier
                                                .border(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = it,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            },
                                            onClick = {
                                                tempItem.value = tempItem.value.copy(unit = it)
                                                ddExpanded = false
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(10f)
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor),
                                shape = RectangleShape
                            ) {
                                if(!editMode.value) {
                                    Text(
                                        text = item.desc,
                                        modifier = Modifier.padding(8.dp),
                                        color = textColor,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }else {
                                    OutlinedTextField(
                                        value = tempItem.value.desc,
                                        onValueChange = {
                                            tempItem.value = tempItem.value.copy(desc = it)
                                        },
                                        modifier = Modifier.padding(0.dp),
                                        label = { Text("Notes") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = textColor,
                                            focusedBorderColor = textColor,
                                            unfocusedTextColor = textColor,
                                            focusedTextColor = textColor,
                                            unfocusedLabelColor = textColor,
                                            focusedLabelColor = textColor,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(6.dp))
                IconButton(
                    onClick = {
                        editMode.value = !editMode.value
                        show.value = editMode.value
                        if(!editMode.value) {
                            onSave(tempItem.value)
                        }
                    },
                    modifier = Modifier
                        .sizeIn(maxHeight = iconSize)
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        modifier = Modifier
                            .size(iconSize),
                        contentDescription = "Edit",
                        //tint = Color.Black
                    )
                }
                if (show.value) {
                    Spacer(Modifier.height(12.dp))
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier
                            .height(iconSize)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            modifier = Modifier
                                .size(iconSize)
                                .padding(0.dp),
                            contentDescription = "Delete",
                            //tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/*private fun updateList(
    context: Context,
    itemListDone: SnapshotStateList<CartItem>,
    itemListUndone: SnapshotStateList<CartItem>
) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            val fetchedList = Cart.getList(context)
            itemListUndone.clear()
            itemListUndone.addAll(fetchedList.filter { !it.done })
            itemListDone.clear()
            itemListDone.addAll(fetchedList.filter { it.done })
        }catch(e: Exception) {
            Log.d("Debugging","updateList: ${e.message}")
        }
    }
}*/