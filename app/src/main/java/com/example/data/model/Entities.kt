package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val outingDate: Long? = null,
    val isFinished: Boolean = false
)

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val name: String,
    val avatarColor: String // Hex string representing color
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val title: String,
    val amount: Double,
    val category: String, // Food, Rent, Utilities, Entertainment, Shopping, Other
    val payerId: Int, // Member who paid
    val splitType: String, // "EQUAL", "PERCENTAGE", "CUSTOM"
    val timestamp: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val dueDate: Long = 0L // Next reminder timestamp if recurring
)

@Entity(tableName = "expense_splits")
data class ExpenseSplit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val memberId: Int,
    val amount: Double,
    val percentage: Double = 0.0
)

@Entity(tableName = "settlements")
data class Settlement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val payerId: Int, // The member who paid back (debtor)
    val payeeId: Int, // The member who received (creditor)
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
