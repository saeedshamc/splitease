package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Update
    suspend fun updateGroup(group: Group)

    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: Int): Group?

    @Delete
    suspend fun deleteGroup(group: Group)
}

@Dao
interface MemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Member>)

    @Query("SELECT * FROM members WHERE groupId = :groupId")
    fun getMembersByGroup(groupId: Int): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE groupId = :groupId")
    suspend fun getMembersByGroupSync(groupId: Int): List<Member>

    @Delete
    suspend fun deleteMember(member: Member)
}

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getExpensesByGroup(groupId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    suspend fun getExpensesByGroupSync(groupId: Int): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplit>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsByExpense(expenseId: Int)

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: Int): List<ExpenseSplit>

    @Query("SELECT * FROM expense_splits WHERE expenseId IN (SELECT id FROM expenses WHERE groupId = :groupId)")
    fun getSplitsForGroup(groupId: Int): Flow<List<ExpenseSplit>>

    @Query("SELECT * FROM expense_splits WHERE expenseId IN (SELECT id FROM expenses WHERE groupId = :groupId)")
    suspend fun getSplitsForGroupSync(groupId: Int): List<ExpenseSplit>
}

@Dao
interface SettlementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: Settlement): Long

    @Delete
    suspend fun deleteSettlement(settlement: Settlement)

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getSettlementsByGroup(groupId: Int): Flow<List<Settlement>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId")
    suspend fun getSettlementsByGroupSync(groupId: Int): List<Settlement>
}

@Database(
    entities = [Group::class, Member::class, Expense::class, ExpenseSplit::class, Settlement::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
}
