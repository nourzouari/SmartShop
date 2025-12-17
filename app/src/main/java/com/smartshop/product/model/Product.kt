// Product.kt - For Firebase only
package com.smartshop.product.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Product(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val category: String = "",
    val imageUrl: String = "",

    @ServerTimestamp
    val createdAt: Timestamp? = null ,

    val userId: String = ""
)