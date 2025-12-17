package com.smartshop.product.data.local.entity

// app/src/main/java/com/smartshop/product/data/local/entity/LocalProduct.kt

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["category"]),
        Index(value = ["isSynced"]),
        Index(value = ["createdAt"]),
        Index(value = ["isDeleted"])
    ]
)
data class LocalProduct(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description", defaultValue = "")
    val description: String,

    @ColumnInfo(name = "price")
    val price: Double,

    @ColumnInfo(name = "quantity")
    val quantity: Int,

    @ColumnInfo(name = "category", defaultValue = "")
    val category: String,

    @ColumnInfo(name = "imageUrl", defaultValue = "")
    val imageUrl: String,

    @ColumnInfo(name = "userId")
    val userId: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long, // Timestamp en millisecondes

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long, // Timestamp en millisecondes

    @ColumnInfo(name = "isSynced", defaultValue = "false")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "isDeleted", defaultValue = "false")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "lastSyncAttempt", defaultValue = "0")
    val lastSyncAttempt: Long = 0,

    @ColumnInfo(name = "syncError", defaultValue = "")
    val syncError: String = ""
)