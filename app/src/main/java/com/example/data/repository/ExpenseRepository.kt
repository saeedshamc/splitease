package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val db: AppDatabase) {
    val groups: Flow<List<Group>> = db.groupDao().getAllGroups()

    suspend fun getGroupById(id: Int): Group? = db.groupDao().getGroupById(id)

    suspend fun insertGroup(name: String): Long {
        return db.groupDao().insertGroup(Group(name = name))
    }

    suspend fun insertGroup(group: Group): Long {
        return db.groupDao().insertGroup(group)
    }

    suspend fun updateGroup(group: Group) {
        db.groupDao().updateGroup(group)
    }

    suspend fun deleteGroup(group: Group) {
        db.groupDao().deleteGroup(group)
    }

    fun getMembersForGroup(groupId: Int): Flow<List<Member>> = db.memberDao().getMembersByGroup(groupId)

    suspend fun getMembersForGroupSync(groupId: Int): List<Member> = db.memberDao().getMembersByGroupSync(groupId)

    suspend fun insertMember(groupId: Int, name: String, avatarColor: String, headcount: Int = 1, userId: String? = null): Long {
        return db.memberDao().insertMember(Member(groupId = groupId, name = name, avatarColor = avatarColor, headcount = headcount, userId = userId))
    }

    suspend fun insertMember(member: Member): Long {
        return db.memberDao().insertMember(member)
    }

    suspend fun insertMembers(members: List<Member>) {
        db.memberDao().insertMembers(members)
    }

    suspend fun deleteMember(member: Member) {
        db.memberDao().deleteMember(member)
    }

    // User Operations
    val allUsers: Flow<List<User>> = db.userDao().getAllUsers()
    suspend fun insertUser(user: User): Long = db.userDao().insertUser(user)
    suspend fun getUserById(uid: String): User? = db.userDao().getUserById(uid)

    // Recurring Schedule Operations
    fun getActiveSchedulesForGroup(groupId: Int): Flow<List<RecurringSchedule>> = db.recurringScheduleDao().getActiveSchedulesByGroup(groupId)
    suspend fun insertSchedule(schedule: RecurringSchedule): Long = db.recurringScheduleDao().insertSchedule(schedule)
    suspend fun updateSchedule(schedule: RecurringSchedule) = db.recurringScheduleDao().updateSchedule(schedule)
    suspend fun deleteSchedule(schedule: RecurringSchedule) = db.recurringScheduleDao().deleteSchedule(schedule)
    suspend fun getAllActiveSchedulesSync(): List<RecurringSchedule> = db.recurringScheduleDao().getAllActiveSchedulesSync()

    fun getExpensesForGroup(groupId: Int): Flow<List<Expense>> = db.expenseDao().getExpensesByGroup(groupId)

    suspend fun getExpensesForGroupSync(groupId: Int): List<Expense> = db.expenseDao().getExpensesByGroupSync(groupId)

    fun getSplitsForGroup(groupId: Int): Flow<List<ExpenseSplit>> = db.expenseDao().getSplitsForGroup(groupId)

    suspend fun getSplitsForGroupSync(groupId: Int): List<ExpenseSplit> = db.expenseDao().getSplitsForGroupSync(groupId)

    suspend fun insertExpense(expense: Expense, splits: List<ExpenseSplit>): Long {
        val expenseId = db.expenseDao().insertExpense(expense)
        val updatedSplits = splits.map { it.copy(expenseId = expenseId.toInt()) }
        db.expenseDao().insertSplits(updatedSplits)
        return expenseId
    }

    suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>) {
        db.expenseDao().updateExpense(expense)
        db.expenseDao().deleteSplitsByExpense(expense.id)
        val updatedSplits = splits.map { it.copy(expenseId = expense.id) }
        db.expenseDao().insertSplits(updatedSplits)
    }

    suspend fun deleteExpense(expense: Expense) {
        db.expenseDao().deleteSplitsByExpense(expense.id)
        db.expenseDao().deleteExpense(expense)
    }

    suspend fun getSplitsForExpense(expenseId: Int): List<ExpenseSplit> {
        return db.expenseDao().getSplitsForExpense(expenseId)
    }

    fun getSettlementsForGroup(groupId: Int): Flow<List<Settlement>> = db.settlementDao().getSettlementsByGroup(groupId)

    suspend fun getSettlementsForGroupSync(groupId: Int): List<Settlement> = db.settlementDao().getSettlementsByGroupSync(groupId)

    suspend fun insertSettlement(settlement: Settlement): Long {
        return db.settlementDao().insertSettlement(settlement)
    }

    suspend fun deleteSettlement(settlement: Settlement) {
        db.settlementDao().deleteSettlement(settlement)
    }
}
