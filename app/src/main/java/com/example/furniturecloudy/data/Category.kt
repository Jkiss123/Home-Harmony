package com.example.furniturecloudy.data

sealed class Category (val category: String){
    object Chair:Category("Chair")
    object CupBoard:Category("CupBoard")
    object Furniture:Category("Furniture")
    object Table:Category("Table")
    object Accessory:Category("Accessory")
}