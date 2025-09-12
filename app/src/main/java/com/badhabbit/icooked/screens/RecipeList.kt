package com.badhabbit.icooked.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.badhabbit.icooked.datalayer.Recipe
import com.badhabbit.icooked.repository.RecipeBox
import kotlinx.coroutines.launch

@Composable
fun RecipeList(
    contentPadding: PaddingValues,
    onTitleChanged: (String) -> Unit,
    onRecipeClicked: (String) -> Unit
) {
    val context = LocalContext.current
    val recipeList = remember { mutableStateListOf<Recipe>()}
    val scope = rememberCoroutineScope()
    val onDeleteRecipe: (Recipe) -> Unit = { recipe: Recipe ->
        scope.launch {
            RecipeBox.deleteRecipe(context,recipe)
            RecipeBox.getRecipeList(context,recipeList)
        }
    }

    LaunchedEffect(recipeList) {
        scope.launch {
            RecipeBox.updateBox(context)
            RecipeBox.getRecipeList(context, recipeList)
            //fetchList(context, recipeList)
        }
    }
    onTitleChanged("- Recipes")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(0.dp)
                .padding(top = 12.dp)
        ) {
            items(items = recipeList) {

            }
            items(
                items = recipeList
            ) { recipe ->
                TitleCard(recipe, onRecipeClicked, onDeleteRecipe)
            }
        }
    }
}

@Composable
fun TitleCard(
    recipe: Recipe,
    onRecipeClicked: (String) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
            .padding(top = 5.dp)
            .clickable { onRecipeClicked(recipe.filename) },
        colors = CardDefaults.cardColors(
            MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = recipe.name,
                modifier = Modifier,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete Recipe",
                modifier = Modifier
                    .clickable { onDeleteRecipe(recipe) }
            )
        }
    }
}

/*private fun fetchList(
    context: Context,
    recipeList: SnapshotStateList<Recipe>
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val tempList = RecipeBox.getRecipeList(context).sortedBy { it.lowercase() }
            recipeList.clear()
            var unsortedList = mutableListOf<Recipe>()
            tempList.forEach {filename ->
                val file = RecipeBox.getRecipe(context,filename)
                unsortedList.add(file)
            }
            unsortedList
                .sortedBy { it.name.lowercase() }
                .forEach {recipe ->
                    recipeList.add(recipe)
                }
        } catch (e: Exception) {
            Log.d("Debugging", "fetchList: ${e.message}")
        }
    }
}

private fun deleteRecipe(
    context: Context,
    recipe: Recipe
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            RecipeBox.deleteRecipe(context, recipe)
        } catch (e: Exception) {
            Log.d("Debugging", "deleteRecipe: ${e.message}")
        }
    }
}*/