package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar

data class SettleTransaction(
    val debtor: Member,
    val creditor: Member,
    val amount: Double
)

class SplitEaseViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("SplitEasePrefs", Context.MODE_PRIVATE)
    
    // Database and Repository Setup
    private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE expenses ADD COLUMN actualPayerName TEXT DEFAULT NULL")
        }
    }

    private val db = androidx.room.Room.databaseBuilder(
        application,
        AppDatabase::class.java, "splitease_db"
    ).addMigrations(MIGRATION_3_4).fallbackToDestructiveMigration().build()
    
    val repository = ExpenseRepository(db)
    
    val authManager = AuthManager(application)
    val currentUser: StateFlow<User?> = authManager.currentUser

    // Language & Theme State
    private val _isFarsi = MutableStateFlow(sharedPrefs.getBoolean("isFarsi", false))
    val isFarsi: StateFlow<Boolean> = _isFarsi.asStateFlow()

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("isDarkMode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _customCurrency = MutableStateFlow(sharedPrefs.getString("customCurrency", null))
    val customCurrency: StateFlow<String?> = _customCurrency.asStateFlow()

    private val _enableDebtAlerts = MutableStateFlow(sharedPrefs.getBoolean("enableDebtAlerts", true))
    val enableDebtAlerts: StateFlow<Boolean> = _enableDebtAlerts.asStateFlow()

    fun toggleDebtAlerts(enabled: Boolean) {
        _enableDebtAlerts.value = enabled
        sharedPrefs.edit().putBoolean("enableDebtAlerts", enabled).apply()
    }

    // App Update & Admin Announcement State
    private val _backendApiUrl = MutableStateFlow(sharedPrefs.getString("backendApiUrl", "https://splitease-api.vercel.app/api") ?: "https://splitease-api.vercel.app/api")
    val backendApiUrl: StateFlow<String> = _backendApiUrl.asStateFlow()

    private val _appUpdatePolicy = MutableStateFlow<AppUpdatePolicy?>(null)
    val appUpdatePolicy: StateFlow<AppUpdatePolicy?> = _appUpdatePolicy.asStateFlow()

    private val _activeAdminMessage = MutableStateFlow<AdminMessage?>(null)
    val activeAdminMessage: StateFlow<AdminMessage?> = _activeAdminMessage.asStateFlow()

    fun saveBackendApiUrl(url: String) {
        _backendApiUrl.value = url
        sharedPrefs.edit().putString("backendApiUrl", url).apply()
        fetchAppConfigFromServer()
    }

    fun dismissAdminMessage() {
        _activeAdminMessage.value = null
    }

    fun dismissOptionalUpdate() {
        if (_appUpdatePolicy.value?.isMandatory == false) {
            _appUpdatePolicy.value = null
        }
    }

    fun simulateUpdatePolicy(versionCode: Int, versionName: String, isMandatory: Boolean, releaseNotes: String, upcomingTeaser: String?) {
        _appUpdatePolicy.value = AppUpdatePolicy(
            latestVersionCode = versionCode,
            latestVersionName = versionName,
            isMandatory = isMandatory,
            releaseNotes = releaseNotes,
            upcomingFeaturesTeaser = upcomingTeaser.takeIf { !it.isNullOrBlank() }
        )
    }

    fun simulateAdminMessage(title: String, message: String, type: String) {
        _activeAdminMessage.value = AdminMessage(
            id = "msg_" + System.currentTimeMillis(),
            title = title,
            message = message,
            type = type,
            isActive = true
        )
    }

    fun fetchAppConfigFromServer() {
        viewModelScope.launch {
            try {
                val url = _backendApiUrl.value.trimEnd('/') + "/config"
                val response = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute()
                }
                if (response.isSuccessful && response.body != null) {
                    val jsonStr = response.body!!.string()
                    val jsonObject = JSONObject(jsonStr)
                    if (jsonObject.has("updatePolicy")) {
                        val upObj = jsonObject.getJSONObject("updatePolicy")
                        _appUpdatePolicy.value = AppUpdatePolicy(
                            latestVersionCode = upObj.optInt("latestVersionCode", 1),
                            latestVersionName = upObj.optString("latestVersionName", "1.0.0"),
                            isMandatory = upObj.optBoolean("isMandatory", false),
                            releaseNotes = upObj.optString("releaseNotes", ""),
                            upcomingFeaturesTeaser = upObj.optString("upcomingFeaturesTeaser", null),
                            downloadUrl = upObj.optString("downloadUrl", "https://example.com")
                        )
                    }
                    if (jsonObject.has("activeMessage")) {
                        val msgObj = jsonObject.getJSONObject("activeMessage")
                        if (msgObj.optBoolean("isActive", false)) {
                            _activeAdminMessage.value = AdminMessage(
                                id = msgObj.optString("id", "msg_1"),
                                title = msgObj.optString("title", ""),
                                message = msgObj.optString("message", ""),
                                type = msgObj.optString("type", "INFO"),
                                isActive = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep simulated/local fallback state when offline or endpoint unreachable
            }
        }
    }

    // Groups & Selected Group
    val groups: StateFlow<List<Group>> = repository.groups.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedGroupId = MutableStateFlow(sharedPrefs.getInt("selectedGroupId", -1))
    val selectedGroupId: StateFlow<Int> = _selectedGroupId.asStateFlow()

    // Current Group Members
    val currentMembers: StateFlow<List<Member>> = selectedGroupId
        .flatMapLatest { id ->
            if (id != -1) repository.getMembersForGroup(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Group Expenses
    val currentExpenses: StateFlow<List<Expense>> = selectedGroupId
        .flatMapLatest { id ->
            if (id != -1) repository.getExpensesForGroup(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Group Splits
    val currentSplits: StateFlow<List<ExpenseSplit>> = selectedGroupId
        .flatMapLatest { id ->
            if (id != -1) repository.getSplitsForGroup(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Group Settlements
    val currentSettlements: StateFlow<List<Settlement>> = selectedGroupId
        .flatMapLatest { id ->
            if (id != -1) repository.getSettlementsForGroup(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Group Recurring Schedules
    val currentSchedules: StateFlow<List<RecurringSchedule>> = selectedGroupId
        .flatMapLatest { id ->
            if (id != -1) repository.getActiveSchedulesForGroup(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMembers: StateFlow<List<Member>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month & Year Filter for Reports
    private val calendar = Calendar.getInstance().apply {
        // Default to July 2026 as per local metadata time, or current device month
        timeInMillis = System.currentTimeMillis()
    }
    private val _reportMonth = MutableStateFlow(calendar.get(Calendar.MONTH) + 1) // 1-indexed
    val reportMonth: StateFlow<Int> = _reportMonth.asStateFlow()

    private val _reportYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val reportYear: StateFlow<Int> = _reportYear.asStateFlow()

    // Reactive calculated smart transactions / simplified debts
    val smartTransactions: StateFlow<List<SettleTransaction>> = combine(
        currentMembers, currentExpenses, currentSplits, currentSettlements
    ) { members, expenses, splits, settlements ->
        calculateSmartSettlements(members, expenses, splits, settlements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive balances summary (net for each member)
    val memberBalances: StateFlow<Map<Int, Double>> = combine(
        currentMembers, currentExpenses, currentSplits, currentSettlements
    ) { members, expenses, splits, settlements ->
        calculateBalances(members, expenses, splits, settlements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        fetchAppConfigFromServer()
        viewModelScope.launch { processRecurringSchedules() }
        // Create initial default group if none exists after small delay or instantly, but only on first run
        viewModelScope.launch {
            groups.collect { groupList ->
                val hasCreatedDemo = sharedPrefs.getBoolean("hasCreatedDemo", false)
                if (groupList.isEmpty()) {
                    if (!hasCreatedDemo) {
                        sharedPrefs.edit().putBoolean("hasCreatedDemo", true).apply()
                        createDefaultDemoGroup()
                    } else {
                        _selectedGroupId.value = -1
                    }
                } else if (_selectedGroupId.value == -1 || !groupList.any { it.id == _selectedGroupId.value }) {
                    selectGroup(groupList.first().id)
                }
            }
        }
    }

    private suspend fun createDefaultDemoGroup() {
        // Create a default family/roommate group so the app is not empty on first launch
        val isFarsiActive = _isFarsi.value
        val groupName = if (isFarsiActive) "هم‌خانه‌ای‌ها 🏠" else "Roommates 🏠"
        val gId = repository.insertGroup(groupName).toInt()
        
        val demoMembers = listOf(
            Member(groupId = gId, name = if (isFarsiActive) "سحر" else "Sarah", avatarColor = "#FF6B6B"),
            Member(groupId = gId, name = if (isFarsiActive) "امید" else "Alex", avatarColor = "#4DABF7"),
            Member(groupId = gId, name = if (isFarsiActive) "کیان" else "Kevin", avatarColor = "#51CF66"),
            Member(groupId = gId, name = if (isFarsiActive) "مریم" else "Mary", avatarColor = "#FCC419")
        )
        repository.insertMembers(demoMembers)
        
        // Fetch actual member IDs
        val savedMembers = repository.getMembersForGroupSync(gId)
        if (savedMembers.size >= 3) {
            val m1 = savedMembers[0]
            val m2 = savedMembers[1]
            val m3 = savedMembers[2]

            // Initial demo expenses
            val exp1Id = repository.insertExpense(
                Expense(
                    groupId = gId,
                    title = if (isFarsiActive) "خرید میوه و تره‌بار" else "Grocery Shopping",
                    amount = 120.0,
                    category = "Food",
                    payerId = m1.id,
                    splitType = "EQUAL",
                    timestamp = System.currentTimeMillis() - 86400000 * 2 // 2 days ago
                ),
                savedMembers.map { ExpenseSplit(expenseId = 0, memberId = it.id, amount = 120.0 / savedMembers.size) }
            )

            val exp2Id = repository.insertExpense(
                Expense(
                    groupId = gId,
                    title = if (isFarsiActive) "قبض اینترنت پرسرعت" else "High-Speed Internet Bill",
                    amount = 60.0,
                    category = "Utilities",
                    payerId = m2.id,
                    splitType = "EQUAL",
                    timestamp = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
                    isRecurring = true,
                    dueDate = System.currentTimeMillis() + 86400000 * 25 // 25 days later
                ),
                savedMembers.map { ExpenseSplit(expenseId = 0, memberId = it.id, amount = 60.0 / savedMembers.size) }
            )
        }
        
        selectGroup(gId)
    }

    // Calculations
    private fun calculateBalances(
        members: List<Member>,
        expenses: List<Expense>,
        splits: List<ExpenseSplit>,
        settlements: List<Settlement>
    ): Map<Int, Double> {
        val balances = members.associate { it.id to 0.0 }.toMutableMap()
        
        // Add expense credits and subtract debits
        expenses.forEach { expense ->
            // Credit payer
            balances[expense.payerId] = (balances[expense.payerId] ?: 0.0) + expense.amount
            
            // Debit splits
            val expSplits = splits.filter { it.expenseId == expense.id }
            expSplits.forEach { split ->
                balances[split.memberId] = (balances[split.memberId] ?: 0.0) - split.amount
            }
        }

        // Add settlements (debtor pays back creditor)
        settlements.forEach { settlement ->
            // Debtor gets credit (balance goes up towards 0)
            balances[settlement.payerId] = (balances[settlement.payerId] ?: 0.0) + settlement.amount
            // Creditor gets debit (balance goes down towards 0 since they received money)
            balances[settlement.payeeId] = (balances[settlement.payeeId] ?: 0.0) - settlement.amount
        }

        return balances
    }

    private fun calculateSmartSettlements(
        members: List<Member>,
        expenses: List<Expense>,
        splits: List<ExpenseSplit>,
        settlements: List<Settlement>
    ): List<SettleTransaction> {
        if (members.isEmpty()) return emptyList()
        val balances = calculateBalances(members, expenses, splits, settlements)

        val debtors = mutableListOf<Pair<Member, Double>>()
        val creditors = mutableListOf<Pair<Member, Double>>()

        members.forEach { member ->
            val bal = balances[member.id] ?: 0.0
            if (bal < -0.01) {
                debtors.add(Pair(member, -bal))
            } else if (bal > 0.01) {
                creditors.add(Pair(member, bal))
            }
        }

        val result = mutableListOf<SettleTransaction>()

        // Greedy match
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            debtors.sortByDescending { it.second }
            creditors.sortByDescending { it.second }

            val (debtor, debt) = debtors.first()
            val (creditor, credit) = creditors.first()

            val amountSettle = minOf(debt, credit)
            result.add(SettleTransaction(debtor, creditor, amountSettle))

            debtors.removeAt(0)
            creditors.removeAt(0)

            val remDebt = debt - amountSettle
            val remCredit = credit - amountSettle

            if (remDebt > 0.01) {
                debtors.add(Pair(debtor, remDebt))
            }
            if (remCredit > 0.01) {
                creditors.add(Pair(creditor, remCredit))
            }
        }

        return result
    }

    // Actions
    fun createGroup(
        name: String, 
        memberNames: List<String>, 
        outingDate: Long? = null, 
        isFinished: Boolean = false,
        groupType: String = "STANDARD",
        memberHeadcounts: List<Int> = emptyList()
    ) {
        viewModelScope.launch {
            val colors = listOf("#FF6B6B", "#4DABF7", "#51CF66", "#FCC419", "#AE3EC9", "#15AABF", "#F76707", "#74B816")
            val groupId = repository.insertGroup(Group(name = name, outingDate = outingDate, isFinished = isFinished, groupType = groupType)).toInt()
            memberNames.forEachIndexed { index, memberName ->
                if (memberName.isNotBlank()) {
                    val hc = memberHeadcounts.getOrNull(index)?.coerceAtLeast(1) ?: 1
                    repository.insertMember(
                        groupId = groupId,
                        name = memberName.trim(),
                        avatarColor = colors[index % colors.size],
                        headcount = hc
                    )
                }
            }
            selectGroup(groupId)
        }
    }

    fun createMultiGroupTrip(
        tripName: String,
        selectedGroupIds: List<Int>,
        outingDate: Long? = null
    ) {
        viewModelScope.launch {
            val colors = listOf("#FF6B6B", "#4DABF7", "#51CF66", "#FCC419", "#AE3EC9", "#15AABF", "#F76707", "#74B816")
            val tripId = repository.insertGroup(Group(name = tripName, outingDate = outingDate, isFinished = false, groupType = "MULTI_GROUP_TRIP")).toInt()
            
            val allMems = allMembers.value
            val allGrps = groups.value
            
            selectedGroupIds.forEachIndexed { index, grpId ->
                val grp = allGrps.find { it.id == grpId }
                if (grp != null) {
                    val grpMembers = allMems.filter { it.groupId == grpId }
                    val totalHc = grpMembers.sumOf { it.headcount.coerceAtLeast(1) }.coerceAtLeast(1)
                    val masterPayer = grpMembers.firstOrNull()?.name
                    val dispName = if (masterPayer != null && grp.groupType == "FAMILY_TRIP") "${grp.name} (👑 $masterPayer)" else grp.name
                    repository.insertMember(
                        groupId = tripId,
                        name = dispName,
                        avatarColor = colors[index % colors.size],
                        headcount = totalHc,
                        userId = "GROUP_${grpId}"
                    )
                }
            }
            selectGroup(tripId)
        }
    }

    fun updateGroupDetails(group: Group, name: String, outingDate: Long?, isFinished: Boolean) {
        viewModelScope.launch {
            repository.updateGroup(group.copy(name = name, outingDate = outingDate, isFinished = isFinished))
        }
    }

    fun toggleGroupFinished(group: Group) {
        viewModelScope.launch {
            val updatedStatus = !group.isFinished
            repository.updateGroup(group.copy(isFinished = updatedStatus))
        }
    }

    fun addMember(name: String, color: String, headcount: Int = 1, userId: String? = null, targetGroupId: Int? = null) {
        val gId = targetGroupId ?: _selectedGroupId.value
        if (gId == -1) return
        viewModelScope.launch {
            repository.insertMember(gId, name, color, headcount, userId)
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        payerId: Int,
        splitType: String,
        customShares: Map<Int, Double>, // Member ID -> share amount/percentage
        isRecurring: Boolean,
        actualPayerName: String? = null
    ) {
        val gId = _selectedGroupId.value
        if (gId == -1) return
        
        viewModelScope.launch {
            val membersList = currentMembers.value
            val splits = mutableListOf<ExpenseSplit>()

            val nextDueDate = if (isRecurring) {
                System.currentTimeMillis() + 86400000L * 30L // Exactly 30 days from now
            } else {
                0L
            }

            val expense = Expense(
                groupId = gId,
                title = title.trim(),
                amount = amount,
                category = category,
                payerId = payerId,
                splitType = splitType,
                timestamp = System.currentTimeMillis(),
                isRecurring = isRecurring,
                dueDate = nextDueDate,
                actualPayerName = actualPayerName
            )

            when (splitType) {
                "EQUAL" -> {
                    val portion = amount / membersList.size
                    membersList.forEach { member ->
                        splits.add(ExpenseSplit(expenseId = 0, memberId = member.id, amount = portion))
                    }
                }
                "PERCENTAGE" -> {
                    membersList.forEach { member ->
                        val percent = customShares[member.id] ?: 0.0
                        val portion = (percent / 100.0) * amount
                        splits.add(ExpenseSplit(expenseId = 0, memberId = member.id, amount = portion, percentage = percent))
                    }
                }
                "CUSTOM" -> {
                    membersList.forEach { member ->
                        val portion = customShares[member.id] ?: 0.0
                        splits.add(ExpenseSplit(expenseId = 0, memberId = member.id, amount = portion))
                    }
                }
                "BY_HEADCOUNT" -> {
                    val totalHeadcount = membersList.sumOf { it.headcount.coerceAtLeast(1) }
                    val costPerPerson = if (totalHeadcount > 0) amount / totalHeadcount else if (membersList.isNotEmpty()) amount / membersList.size else 0.0
                    membersList.forEach { member ->
                        val portion = costPerPerson * member.headcount.coerceAtLeast(1)
                        splits.add(ExpenseSplit(expenseId = 0, memberId = member.id, amount = portion))
                    }
                }
            }

            repository.insertExpense(expense, splits)
            if (_enableDebtAlerts.value) {
                com.example.util.NotificationHelper.showDebtAlert(
                    getApplication(),
                    "🔔 New Expense Recorded / هزینه جدید ثبت شد",
                    "${title.trim()} (${amount}) added to group."
                )
            }
        }
    }

    fun updateExpense(
        expenseId: Int,
        title: String,
        amount: Double,
        category: String,
        payerId: Int,
        splitType: String,
        customShares: Map<Int, Double>,
        isRecurring: Boolean,
        timestamp: Long,
        actualPayerName: String? = null
    ) {
        val gId = _selectedGroupId.value
        if (gId == -1) return

        viewModelScope.launch {
            val membersList = currentMembers.value
            val splits = mutableListOf<ExpenseSplit>()

            val nextDueDate = if (isRecurring) {
                System.currentTimeMillis() + 86400000L * 30L
            } else {
                0L
            }

            val expense = Expense(
                id = expenseId,
                groupId = gId,
                title = title.trim(),
                amount = amount,
                category = category,
                payerId = payerId,
                splitType = splitType,
                timestamp = timestamp,
                isRecurring = isRecurring,
                dueDate = nextDueDate,
                actualPayerName = actualPayerName
            )

            when (splitType) {
                "EQUAL" -> {
                    val portion = amount / membersList.size
                    membersList.forEach { member ->
                        splits.add(ExpenseSplit(expenseId = expenseId, memberId = member.id, amount = portion))
                    }
                }
                "PERCENTAGE" -> {
                    membersList.forEach { member ->
                        val percent = customShares[member.id] ?: 0.0
                        val portion = (percent / 100.0) * amount
                        splits.add(ExpenseSplit(expenseId = expenseId, memberId = member.id, amount = portion, percentage = percent))
                    }
                }
                "CUSTOM" -> {
                    membersList.forEach { member ->
                        val portion = customShares[member.id] ?: 0.0
                        splits.add(ExpenseSplit(expenseId = expenseId, memberId = member.id, amount = portion))
                    }
                }
                "BY_HEADCOUNT" -> {
                    val totalHeadcount = membersList.sumOf { it.headcount.coerceAtLeast(1) }
                    val costPerPerson = if (totalHeadcount > 0) amount / totalHeadcount else if (membersList.isNotEmpty()) amount / membersList.size else 0.0
                    membersList.forEach { member ->
                        val portion = costPerPerson * member.headcount.coerceAtLeast(1)
                        splits.add(ExpenseSplit(expenseId = expenseId, memberId = member.id, amount = portion))
                    }
                }
            }

            repository.updateExpense(expense, splits)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun settleDebt(debtorId: Int, creditorId: Int, amount: Double) {
        val gId = _selectedGroupId.value
        if (gId == -1) return
        viewModelScope.launch {
            val settlement = Settlement(
                groupId = gId,
                payerId = debtorId,
                payeeId = creditorId,
                amount = amount,
                timestamp = System.currentTimeMillis()
            )
            repository.insertSettlement(settlement)
        }
    }

    fun deleteSettlement(settlement: Settlement) {
        viewModelScope.launch {
            repository.deleteSettlement(settlement)
        }
    }

    fun deleteGroup(group: Group) {
        viewModelScope.launch {
            repository.deleteGroup(group)
        }
    }

    suspend fun getSplitsForExpense(expenseId: Int): List<ExpenseSplit> {
        return repository.getSplitsForExpense(expenseId)
    }

    fun selectGroup(groupId: Int) {
        _selectedGroupId.value = groupId
        sharedPrefs.edit().putInt("selectedGroupId", groupId).apply()
    }

    fun setReportMonth(month: Int) {
        _reportMonth.value = month
    }

    fun setReportYear(year: Int) {
        _reportYear.value = year
    }

    fun toggleLanguage() {
        val newValue = !_isFarsi.value
        _isFarsi.value = newValue
        sharedPrefs.edit().putBoolean("isFarsi", newValue).apply()
        
        // Let's also update demo group names if there is only one default group and it hasn't been edited
        // to make sure it respects language toggling nicely!
    }

    fun toggleTheme() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        sharedPrefs.edit().putBoolean("isDarkMode", newValue).apply()
    }

    fun setCustomCurrency(currency: String?) {
        _customCurrency.value = currency
        sharedPrefs.edit().putString("customCurrency", currency).apply()
    }

    private suspend fun processRecurringSchedules() {
        try {
            val now = System.currentTimeMillis()
            val schedules = repository.getAllActiveSchedulesSync()
            schedules.forEach { schedule ->
                if (schedule.nextDueDate <= now && schedule.isActive) {
                    val membersList = repository.getMembersForGroupSync(schedule.groupId)
                    if (membersList.isNotEmpty()) {
                        val interval = when (schedule.frequency.uppercase()) {
                            "WEEKLY", "هفتگی" -> 86400000L * 7L
                            "YEARLY", "ساله", "سالانه" -> 86400000L * 365L
                            else -> 86400000L * 30L
                        }
                        val splits = mutableListOf<ExpenseSplit>()
                        val portion = schedule.amount / membersList.size
                        membersList.forEach { member ->
                            splits.add(ExpenseSplit(expenseId = 0, memberId = member.id, amount = portion))
                        }
                        val expense = Expense(
                            groupId = schedule.groupId,
                            title = schedule.title,
                            amount = schedule.amount,
                            category = schedule.category,
                            payerId = schedule.payerId,
                            splitType = schedule.splitType,
                            timestamp = now,
                            isRecurring = true,
                            dueDate = now + interval
                        )
                        repository.insertExpense(expense, splits)
                        var updatedDue = schedule.nextDueDate + interval
                        while (updatedDue <= now) {
                            updatedDue += interval
                        }
                        repository.updateSchedule(schedule.copy(nextDueDate = updatedDue))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addRecurringSchedule(title: String, amount: Double, category: String, payerId: Int, splitType: String = "EQUAL", frequency: String = "MONTHLY") {
        val gId = _selectedGroupId.value
        if (gId == -1) return
        viewModelScope.launch {
            val interval = when (frequency) {
                "WEEKLY" -> 86400000L * 7L
                "YEARLY" -> 86400000L * 365L
                else -> 86400000L * 30L
            }
            val schedule = RecurringSchedule(
                groupId = gId,
                title = title.trim(),
                amount = amount,
                category = category,
                payerId = payerId,
                splitType = splitType,
                frequency = frequency,
                nextDueDate = System.currentTimeMillis() + interval,
                isActive = true
            )
            repository.insertSchedule(schedule)
            com.example.data.worker.RecurringWorkScheduler.runImmediateCheck(getApplication())
        }
    }

    fun deleteRecurringSchedule(schedule: RecurringSchedule) {
        viewModelScope.launch { repository.deleteSchedule(schedule) }
    }

    fun signUpAuth(email: String, pass: String, name: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = authManager.signUp(email, pass, name)
            if (res.isSuccess) onResult(true, "ثبت نام موفقیت‌آمیز بود / Signed up successfully")
            else onResult(false, res.exceptionOrNull()?.message ?: "Sign up failed")
        }
    }

    fun loginAuth(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = authManager.login(email, pass)
            if (res.isSuccess) onResult(true, "ورود موفقیت‌آمیز بود / Logged in successfully")
            else onResult(false, res.exceptionOrNull()?.message ?: "Login failed")
        }
    }

    fun loginGuestAuth(name: String) = authManager.loginAsGuest(name)
    fun logoutAuth() = authManager.logout()

    fun exportDatabaseToJson(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val groupsList = repository.getAllGroupsSync()
                val allMembersList = repository.getAllMembersSync()
                val allExpensesList = repository.getAllExpensesSync()
                val allSplitsList = repository.getAllSplitsSync()
                val allSettlementsList = repository.getAllSettlementsSync()

                val root = org.json.JSONObject()
                val groupsArr = org.json.JSONArray()
                groupsList.forEach { g ->
                    groupsArr.put(org.json.JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("createdAt", g.createdAt)
                        if (g.outingDate != null) put("outingDate", g.outingDate)
                        put("isFinished", g.isFinished)
                        put("groupType", g.groupType)
                    })
                }
                root.put("groups", groupsArr)

                val membersArr = org.json.JSONArray()
                allMembersList.forEach { m ->
                    membersArr.put(org.json.JSONObject().apply {
                        put("id", m.id)
                        put("groupId", m.groupId)
                        put("name", m.name)
                        put("avatarColor", m.avatarColor)
                        put("headcount", m.headcount)
                        if (m.userId != null) put("userId", m.userId)
                    })
                }
                root.put("members", membersArr)

                val expensesArr = org.json.JSONArray()
                allExpensesList.forEach { e ->
                    expensesArr.put(org.json.JSONObject().apply {
                        put("id", e.id)
                        put("groupId", e.groupId)
                        put("title", e.title)
                        put("amount", e.amount)
                        put("category", e.category)
                        put("payerId", e.payerId)
                        put("splitType", e.splitType)
                        put("timestamp", e.timestamp)
                        put("isRecurring", e.isRecurring)
                        put("dueDate", e.dueDate)
                        if (e.actualPayerName != null) put("actualPayerName", e.actualPayerName)
                    })
                }
                root.put("expenses", expensesArr)

                val splitsArr = org.json.JSONArray()
                allSplitsList.forEach { s ->
                    splitsArr.put(org.json.JSONObject().apply {
                        put("id", s.id)
                        put("expenseId", s.expenseId)
                        put("memberId", s.memberId)
                        put("amount", s.amount)
                        put("percentage", s.percentage)
                    })
                }
                root.put("splits", splitsArr)

                val settlementsArr = org.json.JSONArray()
                allSettlementsList.forEach { st ->
                    settlementsArr.put(org.json.JSONObject().apply {
                        put("id", st.id)
                        put("groupId", st.groupId)
                        put("payerId", st.payerId)
                        put("payeeId", st.payeeId)
                        put("amount", st.amount)
                        put("timestamp", st.timestamp)
                    })
                }
                root.put("settlements", settlementsArr)

                onResult(root.toString(2))
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun importDatabaseFromJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonString)
                val groupsList = mutableListOf<Group>()
                if (root.has("groups")) {
                    val arr = root.getJSONArray("groups")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        groupsList.add(Group(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            outingDate = if (obj.has("outingDate") && !obj.isNull("outingDate")) obj.getLong("outingDate") else null,
                            isFinished = obj.optBoolean("isFinished", false),
                            groupType = obj.optString("groupType", "FRIENDS")
                        ))
                    }
                }

                val membersList = mutableListOf<Member>()
                if (root.has("members")) {
                    val arr = root.getJSONArray("members")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        membersList.add(Member(
                            id = obj.optInt("id", 0),
                            groupId = obj.optInt("groupId", 0),
                            name = obj.optString("name", ""),
                            avatarColor = obj.optString("avatarColor", "#3B82F6"),
                            headcount = obj.optInt("headcount", 1),
                            userId = if (obj.has("userId") && !obj.isNull("userId")) obj.getString("userId") else null
                        ))
                    }
                }

                val expensesList = mutableListOf<Expense>()
                if (root.has("expenses")) {
                    val arr = root.getJSONArray("expenses")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        expensesList.add(Expense(
                            id = obj.optInt("id", 0),
                            groupId = obj.optInt("groupId", 0),
                            title = obj.optString("title", ""),
                            amount = obj.optDouble("amount", 0.0),
                            category = obj.optString("category", "Other"),
                            payerId = obj.optInt("payerId", 0),
                            splitType = obj.optString("splitType", "EQUAL"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isRecurring = obj.optBoolean("isRecurring", false),
                            dueDate = obj.optLong("dueDate", 0L),
                            actualPayerName = if (obj.has("actualPayerName") && !obj.isNull("actualPayerName")) obj.getString("actualPayerName") else null
                        ))
                    }
                }

                val splitsList = mutableListOf<ExpenseSplit>()
                if (root.has("splits")) {
                    val arr = root.getJSONArray("splits")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        splitsList.add(ExpenseSplit(
                            id = obj.optInt("id", 0),
                            expenseId = obj.optInt("expenseId", 0),
                            memberId = obj.optInt("memberId", 0),
                            amount = obj.optDouble("amount", 0.0),
                            percentage = obj.optDouble("percentage", 0.0)
                        ))
                    }
                }

                val settlementsList = mutableListOf<Settlement>()
                if (root.has("settlements")) {
                    val arr = root.getJSONArray("settlements")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        settlementsList.add(Settlement(
                            id = obj.optInt("id", 0),
                            groupId = obj.optInt("groupId", 0),
                            payerId = obj.optInt("payerId", 0),
                            payeeId = obj.optInt("payeeId", 0),
                            amount = obj.optDouble("amount", 0.0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                }

                repository.restoreBackupData(groupsList, membersList, expensesList, splitsList, settlementsList)
                onResult(true, "بازیابی اطلاعات با موفقیت انجام شد / Restore completed successfully (${groupsList.size} groups, ${expensesList.size} expenses)")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "خطا در خواندن فایل پشتیبان / Invalid JSON backup format: ${e.message}")
            }
        }
    }
}
