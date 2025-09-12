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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.badhabbit.icooked.datalayer.DraggableItem
import com.badhabbit.icooked.datalayer.Ingredient
import com.badhabbit.icooked.datalayer.Recipe
import com.badhabbit.icooked.repository.Cart
import com.badhabbit.icooked.repository.RecipeBox
import kotlinx.coroutines.launch

@Composable
fun RecipeView(
    contentPadding: PaddingValues,
    filename: String,
    onTitleChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val borderColor = MaterialTheme.colorScheme.onSurface
    val recipe = remember { mutableStateOf(Recipe("Loading...")) }
    val ingredients = remember { mutableStateListOf<Ingredient>() }
    val instructions = remember { mutableStateListOf<String>()}
    val scope = rememberCoroutineScope()

    val recipeState = rememberLazyListState()
    var delta: Float by remember { mutableFloatStateOf(0f) }
    var draggingItem: LazyListItemInfo? by remember { mutableStateOf(null) }
    var draggingItemIndex: Int? by remember { mutableStateOf(null) }
    val onMoveIngredient = { fromIndex: Int, toIndex: Int ->
        ingredients.apply { add(toIndex, removeAt(fromIndex)) }
    }
    val onMoveInstruction = { fromIndex: Int, toIndex: Int ->
        instructions.apply { add(toIndex, removeAt(fromIndex)) }
    }

    LaunchedEffect(context) {
        scope.launch {
            try {
                recipe.value = RecipeBox.getRecipe(context, filename)
                ingredients.clear()
                ingredients.addAll(recipe.value.ingredients)
                instructions.clear()
                instructions.addAll(recipe.value.instructions)
            }catch(e:Exception) {
                Log.d("Debugging"," fetchRecipe: ${e.message}")
            }
        }
    }
    onTitleChanged(recipe.value.name)

    Box(
        modifier = Modifier
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            state = recipeState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp)
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(key1 = recipeState) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            recipeState.layoutInfo.visibleItemsInfo
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
                                recipeState.layoutInfo.visibleItemsInfo.find { item ->
                                    middleOffset.toInt() in item.offset..item.offset + item.size &&
                                            currentItem.index != item.index &&
                                            item.contentType is DraggableItem
                                }

                            if (targetItem != null) {
                                val targetIndex = (targetItem.contentType as DraggableItem).index
                                onMoveIngredient(currentIndex, targetIndex)
                                draggingItemIndex = targetIndex
                                draggingItem = targetItem
                                delta += currentItem.offset - targetItem.offset
                            }
                        },
                        onDragEnd = {
                            draggingItemIndex = null
                            delta = 0f
                        },
                        onDragCancel = {
                            draggingItemIndex = null
                            delta = 0f
                        }
                    )
                }
        ) {
            item {
                var editmode = remember { mutableStateOf(false) }
                if(!editmode.value) {
                    Text(
                        text = recipe.value.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 8.dp)
                            .padding(start = 24.dp)
                            .clickable { editmode.value = true }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = recipe.value.name,
                            onValueChange = {
                                recipe.value = recipe.value.copy(name = it)
                            },
                            maxLines = 1,
                            label = { Text("Name") }
                        )
                        IconButton(
                            onClick = {
                                scope.launch{
                                    RecipeBox.saveRecipe(context,recipe.value)
                                }
                                editmode.value = false
                            }
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                "Finish Edit",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                    colors = CardDefaults.cardColors(
                        MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RectangleShape
                ) {
                    var editmode = remember { mutableStateOf(false) }
                    if(!editmode.value) {
                        Text(
                            text = "About~\n    ${recipe.value.description}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp)
                                .clickable { editmode.value = true },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(6f),
                                value = recipe.value.description,
                                onValueChange = {
                                    recipe.value = recipe.value.copy(description = it)
                                },
                                label = { Text("About") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                )
                            )
                            IconButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch{
                                        RecipeBox.saveRecipe(context,recipe.value)
                                    }
                                    editmode.value = false
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    "Finish Edit",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
            item{
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                        .drawBehind {
                            val y = size.height + 4
                            drawLine(
                                borderColor,
                                Offset(0f, y),
                                Offset(size.width, y),
                                3f
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ingredients:",
                        modifier = Modifier
                            .padding(top = 0.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Filled.AddCircle,
                        "Add Ingredient",
                        modifier = Modifier
                            .clickable {
                                scope.launch {
                                    recipe.value = recipe.value.copy(
                                        ingredients = (recipe.value.ingredients + Ingredient("New")).toMutableList()
                                    )
                                    scope.launch{
                                        RecipeBox.saveRecipe(context,recipe.value)
                                    }
                                    ingredients.clear()
                                    ingredients.addAll(recipe.value.ingredients)
                                }
                            },
                        tint = MaterialTheme.colorScheme.onSurface

                    )
                }
            }
            itemsIndexed(
                items = ingredients,
                key = {_,it -> it.hashCode() },
                contentType = { index, _ -> DraggableItem(index = index) }
            ) { index, ingredient ->
                IngredientCard(
                    ingredient,
                    remember { mutableStateOf(false) },
                    onCart = { name ->
                        scope.launch {
                            Cart.newItem(context,name)
                        }
                    },
                    onSave = { newIngredient ->
                        recipe.value.ingredients[index] =
                            recipe.value.ingredients[index].copy(
                                name = newIngredient.name,
                                qty = newIngredient.qty,
                                unit = newIngredient.unit
                            )
                        scope.launch{
                            RecipeBox.saveRecipe(context,recipe.value)
                        }
                        ingredients.clear()
                        ingredients.addAll(recipe.value.ingredients)
                    },
                    onDelete = {
                        scope.launch {
                            val tempList =
                                recipe.value.ingredients.filterIndexed { it, _ -> it != index }
                            recipe.value =
                                recipe.value.copy(ingredients = tempList.toMutableList())
                            scope.launch{
                                RecipeBox.saveRecipe(context,recipe.value)
                            }
                            ingredients.clear()
                            ingredients.addAll(recipe.value.ingredients)
                        }
                    },
                    modifier = if(draggingItemIndex == index) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer {
                                translationY = delta
                            }
                    } else {
                        Modifier
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                        .drawBehind {
                            val y = size.height + 4
                            drawLine(
                                borderColor,
                                Offset(0f, y),
                                Offset(size.width, y),
                                3f
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Directions:",
                        modifier = Modifier
                            .padding(top = 0.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Filled.AddCircle,
                        "Add Direction",
                        modifier = Modifier
                            .clickable {
                                recipe.value = recipe.value.copy(instructions = (recipe.value.instructions + "New").toMutableList())
                                scope.launch{
                                    RecipeBox.saveRecipe(context,recipe.value)
                                }
                                instructions.clear()
                                instructions.addAll(recipe.value.instructions)
                            },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            itemsIndexed(
                items = instructions,
                key = {_,it -> it.hashCode() },
                contentType = { index, _ -> DraggableItem(index = index) }
            ) { index, instruction ->
                DirectionCard(
                    index,
                    instruction,
                    remember { mutableStateOf(false) },
                    onSave = { newInstruction ->
                        scope.launch {
                            recipe.value.instructions[index] = newInstruction
                            scope.launch{
                                RecipeBox.saveRecipe(context,recipe.value)
                            }
                            instructions.clear()
                            instructions.addAll(recipe.value.instructions)
                        }
                    },
                    onDelete = {
                        scope.launch {
                            val tempList =
                                recipe.value.instructions.filterIndexed { it, _ -> it != index }
                            recipe.value =
                                recipe.value.copy(instructions = tempList.toMutableList())
                            scope.launch{
                                RecipeBox.saveRecipe(context,recipe.value)
                            }
                            instructions.clear()
                            instructions.addAll(recipe.value.instructions)
                        }
                    },
                    modifier = if(draggingItemIndex == index) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer {
                                translationY = delta
                            }
                    } else {
                        Modifier
                    }
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                    colors = CardDefaults.cardColors(
                        MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RectangleShape
                ) {
                    var editmode = remember { mutableStateOf(false) }
                    if(!editmode.value) {
                        Text(
                            text = "Notes~\n    ${recipe.value.notes}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp)
                                .clickable { editmode.value = true },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(6f),
                                value = recipe.value.notes,
                                onValueChange = {
                                    recipe.value = recipe.value.copy(notes = it)
                                },
                                label = { Text("Notes") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                            )
                            IconButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch{
                                        RecipeBox.saveRecipe(context,recipe.value)
                                    }
                                    editmode.value = false
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    "Finish Edit",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectionCard(
    index: Int,
    direction: String,
    editMode: MutableState<Boolean>,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tempDirection = remember { mutableStateOf(direction) }
    val boxColor = MaterialTheme.colorScheme.tertiaryContainer
    val textColor = MaterialTheme.colorScheme.onTertiaryContainer
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .clickable {
                editMode.value = true
            },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        colors = CardDefaults.cardColors(boxColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment =
                if(!editMode.value)
                    Alignment.Top
                else
                    Alignment.CenterVertically
        ) {
            if(!editMode.value) {
                Text(
                    text = "${index + 1}) $direction",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp)
                        .padding(vertical = 4.dp),
                    color = textColor
                )
                Icon(
                    Icons.Filled.Delete,
                    "Remove Direction",
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .padding(vertical = 4.dp)
                        .clickable { onDelete() },
                    tint = textColor
                )
            } else {
                OutlinedTextField(
                    value = tempDirection.value,
                    onValueChange = {
                        tempDirection.value = it
                    },
                    modifier = Modifier
                        .weight(20f)
                        .padding(vertical = 2.dp)
                        .padding(start = 2.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor,
                    )
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Edit,
                    "Edit",
                    modifier = Modifier
                        .weight(2f)
                        .clickable {
                            onSave(tempDirection.value)
                            editMode.value = false
                        },
                    tint = textColor
                )
            }
        }
    }
}

@Composable
fun IngredientCard(
    ingredient: Ingredient,
    editMode: MutableState<Boolean>,
    onCart: (String) -> Unit,
    onSave: (Ingredient) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boxColor = MaterialTheme.colorScheme.tertiaryContainer
    val textColor = MaterialTheme.colorScheme.onTertiaryContainer
    val tempIngredient = remember { mutableStateOf(ingredient) }
    var ddExpanded by remember { mutableStateOf(false)}

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        colors = CardDefaults.cardColors(boxColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(!editMode.value) {
                Icon(
                    Icons.Filled.Add,
                    "Add to Cart",
                    modifier = Modifier.clickable { onCart(ingredient.name) },
                    tint = textColor
                )
                Text(
                    text = "${ingredient.name}",
                    modifier = Modifier
                        .clickable { editMode.value = true }
                )
                Spacer(modifier = Modifier.weight(4f))
                Text("${ingredient.qty}")
                Text("${ingredient.unit}")
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove Ingredient",
                    modifier = Modifier
                        .clickable { onDelete() },
                    tint = textColor
                )
            }else{
                OutlinedTextField(
                    value = tempIngredient.value.name,
                    onValueChange = {
                        tempIngredient.value = tempIngredient.value.copy(name = it)
                    },
                    modifier = Modifier
                        .weight(5f)
                        .padding(vertical = 4.dp)
                        .padding(start = 2.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor,
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = tempIngredient.value.qty,
                    onValueChange = {
                        tempIngredient.value = tempIngredient.value.copy(qty = it)
                    },
                    modifier = Modifier
                        .weight(2f)
                        .padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor,
                    )
                )
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "units")
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .border(
                                2.dp,
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            .clickable(
                                onClick = { ddExpanded = true }
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tempIngredient.value.unit,
                                modifier = Modifier
                                    .padding(vertical = 4.dp),
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
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
                                .border(2.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            },
                            onClick = {
                                tempIngredient.value = tempIngredient.value.copy(unit = it)
                                ddExpanded = false
                            }
                        )
                    }
                }
                Icon(
                    Icons.Filled.Edit,
                    "",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onSave(tempIngredient.value)
                            editMode.value = false
                        },
                    tint = textColor
                )
            }
        }
    }
}

