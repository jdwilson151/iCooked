package com.badhabbit.icooked.datalayer

const val extension:String = "rcp_"

data class DraggableItem(val index: Int)

data class Recipe(
    var name: String = "",
    var filename: String = "$extension$name",
    var description: String = "",
    var notes: String = "",
    var ingredients: MutableList<Ingredient> = arrayListOf(Ingredient("")),
    var instructions: MutableList<String> = arrayListOf("Step 1?")
) {
    override fun toString():String {
        var returnString = "iCooked Recipe for\n${this.name}\nAbout~ ${this.description}\n\nIngredients:\n"
        this.ingredients.forEach {returnString += " $it\n"}
        returnString += "\nDirections:\n"
        var i = 0
        this.instructions.forEach {
            i++
            returnString += "$i) $it\n"
        }
        returnString += "\nNotes: ${this.notes}"
        return returnString
    }
}

data class Ingredient(
    var name: String,
    var qty: String = "",
    var unit: String = "",
) {
    override fun toString():String {
        return "${this.name} ${this.qty} ${this.unit}"
    }
}

data class CartItem(
    val id: Int,
    var itemName: String,
    var qty: String = "",
    var unit: String = "",
    var index: Int = 0,
    var desc: String = "",
    var done: Boolean = false
) {
    override fun toString(): String {
        return "itemName: $itemName, qty: $qty, unit: $unit"
    }
}