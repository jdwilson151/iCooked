package com.badhabbit.icooked

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.badhabbit.icooked.datalayer.Recipe
import com.badhabbit.icooked.repository.Cart
import com.badhabbit.icooked.repository.RecipeBox
import com.badhabbit.icooked.screens.GroceryCart
import com.badhabbit.icooked.screens.RecipeList
import com.badhabbit.icooked.screens.RecipeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val appTitle = remember { mutableStateOf("iCooked")}
    val navController = rememberNavController()
    val currentView = remember { mutableStateOf("")}
    val context = LocalContext.current
    val popup = remember { mutableStateOf(false) }
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    navController.addOnDestinationChangedListener {_,view,_ ->
        currentView.value = view.route.toString()
    }
    //Update data in repositories
    LaunchedEffect(context) {
        CoroutineScope(Dispatchers.Main).launch {
            Cart.updateCart(context)
            RecipeBox.updateBox(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appTitle.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        color = onPrimary
                    )
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    MaterialTheme.colorScheme.primary
                ),
                actions = {
                    if(currentView.value == Screen.RList.route ||
                        currentView.value == "${Screen.RView.route}/{id}"
                    ) {
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.Cart.route)
                            }
                        ) {
                            Icon(
                                Icons.Rounded.ShoppingCart,
                                "Goto Cart",
                                tint = onPrimary
                            )
                        }
                    }
                    if(currentView.value == Screen.Cart.route
                    ) {
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.RList.route)
                            }
                        ) {
                            Icon(
                                Icons.Rounded.List,
                                "Goto Recipes",
                                tint = onPrimary
                            )
                        }
                    }
                    if(currentView.value == "${Screen.RView.route}/{id}"
                    ) {
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.RList.route)
                            }
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                "Goto Recipes",
                                tint = onPrimary
                            )
                        }
                    }
                    //New CartItem
                    if(currentView.value == Screen.Cart.route) {
                        IconButton(
                            onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Cart.newItem(context, "New Item")
                                }
                                navController.navigate(Screen.Cart.route)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddCircle,
                                contentDescription = "Add CartItem",
                                tint = onPrimary
                            )
                        }
                    }
                    //New Recipe
                    if(currentView.value == Screen.RList.route) {
                        IconButton(
                            onClick = {
                                popup.value = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddCircle,
                                contentDescription = "Add Recipe",
                                tint = onPrimary
                            )
                            when {popup.value ->
                                NewRecipePopup(
                                    onConfirmation = {
                                        newRecipe(context,it)
                                        popup.value = false
                                        navController.navigate(Screen.RList.route)
                                    },
                                    onDismiss = { popup.value = false }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) {
        NavigationGraph(it, navController) {title ->
            appTitle.value = "iCooked $title"
        }
    }
}

sealed class Screen(val route: String) {
    data object Cart: Screen("GroceryCart")
    data object RList: Screen("RecipeList")
    data object RView: Screen("RecipeView")
}

@Composable
fun NavigationGraph(
    contentPadding: PaddingValues,
    navController: NavHostController,
    onTitleChanged: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.RList.route
    ) {
        composable(Screen.Cart.route) {
            GroceryCart(contentPadding,onTitleChanged)
        }
        composable(Screen.RList.route) {
            RecipeList(contentPadding,onTitleChanged) {
                navController.navigate("${Screen.RView.route}/$it")
            }
        }
        composable(
            route = "${Screen.RView.route}/{id}",
            arguments = listOf(navArgument("id"){type = NavType.StringType})
        ) {backStackEntry ->
            val filename = backStackEntry.arguments?.getString("id")!!
            RecipeView(
                contentPadding = contentPadding,
                filename = filename,
                onTitleChanged = onTitleChanged
            )
        }
    }
}
@Composable
fun NewRecipePopup(
    onConfirmation: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tempName = remember { mutableStateOf("")}
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        title = {
            Text("New Recipe")
        },
        text = {
            OutlinedTextField(
                value = tempName.value,
                onValueChange = {tempName.value = it}
            )
        },
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = {onConfirmation(tempName.value)}
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(
                onClick = {onDismiss()}
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun newRecipe(
    context: Context,
    name: String
) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            RecipeBox.newRecipe(context, Recipe(name))
        }catch(e: Exception) {
            Log.d("Debugging","newRecipe: ${e.message}")
        }
    }
}