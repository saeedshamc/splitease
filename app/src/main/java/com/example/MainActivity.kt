package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.spring
import com.example.data.model.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Localization
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.ColorNeutral
import com.example.ui.viewmodel.SettleTransaction
import com.example.ui.viewmodel.SplitEaseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: SplitEaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val isFarsi by viewModel.isFarsi.collectAsStateWithLifecycle()
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                // Handle RTL vs LTR composition
                val layoutDirection = if (isFarsi) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    SplitEaseApp(viewModel = viewModel)
                }
            }
        }
    }
}

// Bottom navigation enum
enum class SplitEaseTab {
    DASHBOARD, ADD_EXPENSE, REPORTS, GROUPS, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitEaseApp(viewModel: SplitEaseViewModel) {
    var currentTab by remember { mutableStateOf(SplitEaseTab.DASHBOARD) }
    val isFarsi by viewModel.isFarsi.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val customCurrency by viewModel.customCurrency.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val members by viewModel.currentMembers.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val activeGroup = groups.find { it.id == selectedGroupId }
    
    // Edit Expense state
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingSplits by remember { mutableStateOf<List<ExpenseSplit>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // If edit action triggered, redirect tab
    LaunchedEffect(editingExpense) {
        if (editingExpense != null) {
            currentTab = SplitEaseTab.ADD_EXPENSE
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.splitease_logo_1783405927924),
                            contentDescription = "SplitEase Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = Localization.getString("app_name", isFarsi),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Fast locale / theme quick-toggles in the bar
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("toggle_lang_button")
                    ) {
                        Text(
                            text = if (isFarsi) "EN" else "FA",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                val tabs = listOf(
                    Triple(SplitEaseTab.DASHBOARD, Icons.Rounded.Dashboard, "home"),
                    Triple(SplitEaseTab.ADD_EXPENSE, Icons.Rounded.Add, "add_expense"),
                    Triple(SplitEaseTab.REPORTS, Icons.Rounded.BarChart, "reports"),
                    Triple(SplitEaseTab.GROUPS, Icons.Rounded.Groups, "groups"),
                    Triple(SplitEaseTab.SETTINGS, Icons.Rounded.Settings, "settings")
                )
                
                tabs.forEach { (tab, icon, labelKey) ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { 
                            if (tab != SplitEaseTab.ADD_EXPENSE) {
                                editingExpense = null // Reset edit state if user moves away
                            }
                            currentTab = tab 
                        },
                        icon = { Icon(imageVector = icon, contentDescription = Localization.getString(labelKey, isFarsi)) },
                        label = { 
                            Text(
                                text = Localization.getString(labelKey, isFarsi),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = currentTab,
                animationSpec = spring(),
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    SplitEaseTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        isFarsi = isFarsi,
                        customCurrency = customCurrency,
                        onEditExpense = { expense ->
                            coroutineScope.launch {
                                editingSplits = viewModel.getSplitsForExpense(expense.id)
                                editingExpense = expense
                            }
                        }
                    )
                    SplitEaseTab.ADD_EXPENSE -> AddExpenseScreen(
                        viewModel = viewModel,
                        isFarsi = isFarsi,
                        customCurrency = customCurrency,
                        editingExpense = editingExpense,
                        editingSplits = editingSplits,
                        onCancel = {
                            editingExpense = null
                            currentTab = SplitEaseTab.DASHBOARD
                        },
                        onSaved = {
                            editingExpense = null
                            currentTab = SplitEaseTab.DASHBOARD
                            Toast.makeText(
                                context,
                                Localization.getString(if (editingExpense == null) "add_expense_success" else "edit_expense_success", isFarsi),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    SplitEaseTab.REPORTS -> ReportsScreen(
                        viewModel = viewModel,
                        isFarsi = isFarsi,
                        customCurrency = customCurrency
                    )
                    SplitEaseTab.GROUPS -> GroupsScreen(
                        viewModel = viewModel,
                        isFarsi = isFarsi,
                        customCurrency = customCurrency
                    )
                    SplitEaseTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        isFarsi = isFarsi,
                        customCurrency = customCurrency
                    )
                }
            }
        }
    }
}

// ---------------- DASHBOARD SCREEN ----------------
@Composable
fun DashboardScreen(
    viewModel: SplitEaseViewModel,
    isFarsi: Boolean,
    customCurrency: String?,
    onEditExpense: (Expense) -> Unit
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val members by viewModel.currentMembers.collectAsStateWithLifecycle()
    val expenses by viewModel.currentExpenses.collectAsStateWithLifecycle()
    val settlements by viewModel.currentSettlements.collectAsStateWithLifecycle()
    val smartTx by viewModel.smartTransactions.collectAsStateWithLifecycle()
    val balances by viewModel.memberBalances.collectAsStateWithLifecycle()
    val allMembersList by viewModel.allMembers.collectAsStateWithLifecycle()
    
    var showSettleDialog by remember { mutableStateOf<SettleTransaction?>(null) }
    var activeHistoryView by remember { mutableStateOf(false) } // toggle between settlements & expenses in activity history

    val activeGroup = groups.find { it.id == selectedGroupId }

    if (activeGroup == null) {
        EmptyStateView(
            title = Localization.getString("no_groups_yet", isFarsi),
            isFarsi = isFarsi
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // Group Header Selector Brief
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Localization.getString("active_group", isFarsi),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = activeGroup.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Show Outing Date if set
                        if (activeGroup.outingDate != null) {
                            val sdf = if (isFarsi) SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) else SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            val rawDate = sdf.format(Date(activeGroup.outingDate))
                            val dateStr = if (isFarsi) Localization.formatPersianDigits(rawDate) else rawDate
                            Text(
                                text = "${Localization.getString("outing_date", isFarsi)}: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        
                        // Show Finished Status badge if isFinished
                        if (activeGroup.isFinished) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = Localization.getString("finished", isFarsi),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = Localization.formatNumber(members.size.toDouble(), isFarsi) + " " + Localization.getString("group_members", isFarsi),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (activeGroup.groupType == "FAMILY_TRIP" || activeGroup.groupType == "MULTI_GROUP_TRIP") {
            item {
                val totalExp = expenses.sumOf { it.amount }
                val totalHc = members.sumOf { it.headcount.coerceAtLeast(1) }
                val costPerPerson = if (totalHc > 0) totalExp / totalHc else 0.0

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = if (activeGroup.groupType == "MULTI_GROUP_TRIP") Icons.Rounded.Groups else Icons.Rounded.FamilyRestroom, contentDescription = "Summary", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (activeGroup.groupType == "MULTI_GROUP_TRIP") Localization.getString("group_cost_breakdown", isFarsi) else Localization.getString("trip_summary", isFarsi),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = Localization.getString("total_spending", isFarsi) + ":", fontSize = 13.sp)
                            Text(text = Localization.formatCurrency(totalExp, isFarsi, customCurrency), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = Localization.getString("total_headcount", isFarsi) + ":", fontSize = 13.sp)
                            Text(text = "${Localization.formatNumber(totalHc.toDouble(), isFarsi)} ${if (isFarsi) "نفر" else "people"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = Localization.getString("cost_per_person", isFarsi) + ":", fontSize = 13.sp)
                            Text(text = Localization.formatCurrency(costPerPerson, isFarsi, customCurrency), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        if (activeGroup.groupType == "MULTI_GROUP_TRIP" && members.isNotEmpty()) {
                            Box(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)))
                            Text(
                                text = if (isFarsi) "تفکیک سهم و هزینه هر گروه (سرانه و جمع کل):" else "Group Breakdown (Per-Person & Total):",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            members.forEach { m ->
                                val grpId = m.userId?.removePrefix("GROUP_")?.toIntOrNull()
                                val masterPayer = remember(allMembersList, grpId) { if (grpId != null) allMembersList.filter { it.groupId == grpId }.firstOrNull()?.name else null }
                                val groupShare = costPerPerson * m.headcount.coerceAtLeast(1)
                                val paidByGroup = expenses.filter { it.payerId == m.id }.sumOf { it.amount }
                                val netBalance = balances[m.id] ?: 0.0

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = m.name + if (masterPayer != null) " (👑 ${Localization.getString("master_payer", isFarsi)}: $masterPayer)" else "",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${m.headcount} ${if (isFarsi) "نفر" else "people"}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = Localization.getString("per_person_share", isFarsi) + ":", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = Localization.formatCurrency(costPerPerson, isFarsi, customCurrency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = Localization.getString("group_total_share", isFarsi) + ":", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = Localization.formatCurrency(groupShare, isFarsi, customCurrency), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = Localization.getString("family_paid", isFarsi) + ":", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = Localization.formatCurrency(paidByGroup, isFarsi, customCurrency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = Localization.getString("net_status", isFarsi) + ":", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val balTxt = if (kotlin.math.abs(netBalance) < 1.0) {
                                                Localization.getString("settled", isFarsi)
                                            } else if (netBalance > 0) {
                                                (if (isFarsi) "بستانکار / دریافت: +" else "Receives: +") + Localization.formatCurrency(netBalance, isFarsi, customCurrency)
                                            } else {
                                                (if (isFarsi) "بدهکار / پرداخت: -" else "Pays: -") + Localization.formatCurrency(kotlin.math.abs(netBalance), isFarsi, customCurrency)
                                            }
                                            Text(
                                                text = balTxt,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (netBalance > 0) Color(0xFF10B981) else if (netBalance < -1.0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val innerMems = remember(allMembersList, grpId) { if (grpId != null) allMembersList.filter { it.groupId == grpId } else emptyList() }
                                        if (innerMems.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                                            Text(
                                                text = if (isFarsi) "👥 مرحله دوم: تفکیک هزینه نفرات در این گروه:" else "👥 Stage 2: Per-Person Breakdown in Group:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            innerMems.forEachIndexed { idx, ind ->
                                                val indShare = costPerPerson * ind.headcount.coerceAtLeast(1)
                                                val indPaid = expenses.filter { it.payerId == m.id && (it.actualPayerName == ind.name || (it.actualPayerName == null && idx == 0)) }.sumOf { it.amount }
                                                val indNet = indPaid - indShare
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "• ${ind.name} (${ind.headcount} ${if (isFarsi) "نفر" else "p"})" + if (idx == 0) " (👑)" else "",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    val indBalTxt = if (kotlin.math.abs(indNet) < 1.0) {
                                                        if (isFarsi) "تسویه" else "Settled"
                                                    } else if (indNet > 0) {
                                                        (if (isFarsi) "طالب: +" else "+") + Localization.formatCurrency(indNet, isFarsi, customCurrency)
                                                    } else {
                                                        (if (isFarsi) "بدهی: -" else "-") + Localization.formatCurrency(kotlin.math.abs(indNet), isFarsi, customCurrency)
                                                    }
                                                    Text(
                                                        text = "${if (isFarsi) "سهم:" else "Share:"} ${Localization.formatCurrency(indShare, isFarsi, customCurrency)} | $indBalTxt",
                                                        fontSize = 11.sp,
                                                        color = if (indNet > 0) Color(0xFF10B981) else if (indNet < -1.0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Balance Cards
        item {
            Text(
                text = Localization.getString("balance", isFarsi),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Lent Card (Owed to group)
                val totalLent = balances.values.filter { it > 0 }.sum()
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = "Lent",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.getString("total_lent", isFarsi),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Localization.formatCurrency(totalLent, isFarsi, customCurrency),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Borrowed Card (Owed by group)
                val totalBorrowed = balances.values.filter { it < 0 }.sum()
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingDown,
                                contentDescription = "Borrowed",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.getString("total_borrowed", isFarsi),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Localization.formatCurrency(kotlin.math.abs(totalBorrowed), isFarsi, customCurrency),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Smart Settlements
        item {
            Text(
                text = Localization.getString("who_owes_whom", isFarsi),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (smartTx.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Celebration,
                            contentDescription = "Settled Up",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Localization.getString("settled_up_message", isFarsi),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(smartTx) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Debtor avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(android.graphics.Color.parseColor(tx.debtor.avatarColor)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tx.debtor.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tx.debtor.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (activeGroup.groupType == "FAMILY_TRIP" || activeGroup.groupType == "MULTI_GROUP_TRIP") Localization.getString("settle_instruction", isFarsi) else Localization.getString("owes", isFarsi),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tx.creditor.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Localization.formatCurrency(tx.amount, isFarsi, customCurrency),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFEF4444),
                                    fontSize = 15.sp
                                )

                                val debtorGrpId = tx.debtor.userId?.removePrefix("GROUP_")?.toIntOrNull()
                                val creditorGrpId = tx.creditor.userId?.removePrefix("GROUP_")?.toIntOrNull()
                                if (debtorGrpId != null || creditorGrpId != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val debtorMems = remember(allMembersList, debtorGrpId) { if (debtorGrpId != null) allMembersList.filter { it.groupId == debtorGrpId } else emptyList() }
                                    val creditorMems = remember(allMembersList, creditorGrpId) { if (creditorGrpId != null) allMembersList.filter { it.groupId == creditorGrpId } else emptyList() }
                                    
                                    if (debtorMems.isNotEmpty()) {
                                        Text(
                                            text = (if (isFarsi) "👤 اعضای ${tx.debtor.name}: " else "👤 ${tx.debtor.name}: ") + debtorMems.joinToString("، ") { it.name },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (creditorMems.isNotEmpty()) {
                                        Text(
                                            text = (if (isFarsi) "👤 اعضای ${tx.creditor.name}: " else "👤 ${tx.creditor.name}: ") + creditorMems.joinToString("، ") { it.name },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showSettleDialog = tx },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("settle_up_${tx.debtor.id}_${tx.creditor.id}")
                        ) {
                            Text(
                                text = Localization.getString("settle_up", isFarsi),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Bill Reminders Section (Due Soon)
        val recurringExpenses = expenses.filter { it.isRecurring }
        if (recurringExpenses.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = Localization.getString("reminder_before_due", isFarsi),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = Localization.getString("due_soon", isFarsi),
                            color = Color(0xFFD97706),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(recurringExpenses) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFFBBF24).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EventRepeat,
                                    contentDescription = "Recurring",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = expense.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                                Text(
                                    text = Localization.getString("category_${expense.category.lowercase()}", isFarsi) + " • Next Due: " + sdf.format(Date(expense.dueDate)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Text(
                            text = Localization.formatCurrency(expense.amount, isFarsi, customCurrency),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }
        }

        // Activity History Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = Localization.getString("activity_history", isFarsi),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Toggle between Expenses and Settlements in history
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!activeHistoryView) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeHistoryView = false }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = Localization.getString("add_expense", isFarsi),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!activeHistoryView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeHistoryView) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeHistoryView = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = Localization.getString("settle_up", isFarsi),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeHistoryView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!activeHistoryView) {
            // Expenses History List
            if (expenses.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.empty_state_ill_1783405942502),
                            contentDescription = "Empty History",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Localization.getString("no_expenses_yet", isFarsi),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(expenses) { expense ->
                    val payer = members.find { it.id == expense.payerId }
                    ExpenseItemCard(
                        expense = expense,
                        payer = payer,
                        isFarsi = isFarsi,
                        customCurrency = customCurrency,
                        onEdit = { onEditExpense(expense) },
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                }
            }
        } else {
            // Settlements History List
            if (settlements.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = "No settlements",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Localization.getString("no_settlements_yet", isFarsi),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(settlements) { settlement ->
                    val debtor = members.find { it.id == settlement.payerId }
                    val creditor = members.find { it.id == settlement.payeeId }
                    SettlementItemCard(
                        settlement = settlement,
                        debtor = debtor,
                        creditor = creditor,
                        isFarsi = isFarsi,
                        customCurrency = customCurrency,
                        onDelete = { viewModel.deleteSettlement(settlement) }
                    )
                }
            }
        }
    }

    // Settlement confirmation dialog
    showSettleDialog?.let { tx ->
        Dialog(onDismissRequest = { showSettleDialog = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Handshake,
                            contentDescription = "Settle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Localization.getString("settle_up", isFarsi),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = tx.debtor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingFlat,
                            contentDescription = "pays",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = tx.creditor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Localization.formatCurrency(tx.amount, isFarsi, customCurrency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSettleDialog = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = Localization.getString("cancel", isFarsi))
                        }
                        Button(
                            onClick = {
                                viewModel.settleDebt(tx.debtor.id, tx.creditor.id, tx.amount)
                                showSettleDialog = null
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_settlement_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = Localization.getString("save", isFarsi))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    payer: Member?,
    isFarsi: Boolean,
    customCurrency: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Category icon
                    val (icon, color) = getCategoryStyling(expense.category)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = expense.category,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = expense.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val payerDisplay = if (!expense.actualPayerName.isNullOrBlank() && expense.actualPayerName != payer?.name) {
                            "${expense.actualPayerName} (${payer?.name ?: ""})"
                        } else {
                            payer?.name ?: ""
                        }
                        Text(
                            text = "$payerDisplay • " + Localization.getString("category_${expense.category.lowercase()}", isFarsi),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Localization.formatCurrency(expense.amount, isFarsi, customCurrency),
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorExpense,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(expense.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_expense_${expense.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_expense_${expense.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = ColorExpense
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementItemCard(
    settlement: Settlement,
    debtor: Member?,
    creditor: Member?,
    isFarsi: Boolean,
    customCurrency: String?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ColorIncome.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Handshake,
                        contentDescription = "Settled",
                        tint = ColorIncome,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = debtor?.name ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.getString("settled", isFarsi),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = creditor?.name ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(settlement.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Localization.formatCurrency(settlement.amount, isFarsi, customCurrency),
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorIncome,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = ColorExpense.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun getCategoryStyling(category: String): Pair<ImageVector, Color> {
    return when (category) {
        "Food" -> Pair(Icons.Rounded.Restaurant, Color(0xFF10B981)) // Emerald
        "Rent" -> Pair(Icons.Rounded.Home, Color(0xFF3B82F6))       // Blue
        "Utilities" -> Pair(Icons.Rounded.Lightbulb, Color(0xFFF59E0B)) // Amber
        "Entertainment" -> Pair(Icons.Rounded.Movie, Color(0xFFEC4899)) // Pink
        "Shopping" -> Pair(Icons.Rounded.ShoppingBag, Color(0xFF8B5CF6)) // Purple
        else -> Pair(Icons.Rounded.Category, Color(0xFF6B7280))      // Gray
    }
}


// ---------------- ADD EXPENSE SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: SplitEaseViewModel,
    isFarsi: Boolean,
    customCurrency: String?,
    editingExpense: Expense?,
    editingSplits: List<ExpenseSplit>,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val members by viewModel.currentMembers.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val activeGroup = remember(groups, selectedGroupId) { groups.find { it.id == selectedGroupId } }
    val allMembersList by viewModel.allMembers.collectAsStateWithLifecycle()
    
    var title by remember { mutableStateOf(editingExpense?.title ?: "") }
    var amountStr by remember { mutableStateOf(editingExpense?.amount?.let { if (it % 1 == 0.0) String.format("%.0f", it) else it.toString() } ?: "") }
    var selectedCategory by remember { mutableStateOf(editingExpense?.category ?: "Food") }
    var selectedPayerId by remember { mutableStateOf(editingExpense?.payerId ?: -1) }
    var selectedActualPayerName by remember { mutableStateOf(editingExpense?.actualPayerName) }
    var splitType by remember { mutableStateOf(editingExpense?.splitType ?: "EQUAL") }
    var isRecurring by remember { mutableStateOf(editingExpense?.isRecurring ?: false) }

    // Custom split values: memberId -> custom raw double (exact or percent)
    val customShares = remember { mutableStateMapOf<Int, Double>() }

    // Pre-populate if editing
    LaunchedEffect(editingExpense, editingSplits, members) {
        if (editingExpense != null) {
            title = editingExpense.title
            amountStr = if (editingExpense.amount % 1 == 0.0) String.format("%.0f", editingExpense.amount) else editingExpense.amount.toString()
            selectedCategory = editingExpense.category
            selectedPayerId = editingExpense.payerId
            selectedActualPayerName = editingExpense.actualPayerName
            splitType = editingExpense.splitType
            isRecurring = editingExpense.isRecurring

            customShares.clear()
            editingSplits.forEach { split ->
                if (splitType == "PERCENTAGE") {
                    customShares[split.memberId] = split.percentage
                } else if (splitType == "CUSTOM") {
                    customShares[split.memberId] = split.amount
                }
            }
        } else {
            // Set first member as payer if not empty
            if (members.isNotEmpty() && selectedPayerId == -1) {
                selectedPayerId = members.first().id
            }
        }
    }

    if (members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = Localization.getString("no_groups_yet", isFarsi))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Localization.getString(if (editingExpense == null) "add_new_expense" else "edit_expense", isFarsi),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (activeGroup?.isFinished == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (isFarsi) {
                            "این گروه بایگانی و خاتمه یافته است. برای افزودن یا ویرایش هزینه‌ها ابتدا گروه را از بخش گروه‌ها بازگشایی کنید."
                        } else {
                            "This group is finished and archived. To add or edit expenses, please reopen the group from the Groups section first."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(text = Localization.getString("title", isFarsi)) },
            modifier = Modifier.fillMaxWidth().testTag("expense_title_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Amount Input
        OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            label = { Text(text = Localization.getString("amount", isFarsi) + " (" + (customCurrency ?: if (isFarsi) "تومان" else "$") + ")") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Category Selector Card Row
        Text(
            text = Localization.getString("category", isFarsi),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        val categories = listOf("Food", "Rent", "Utilities", "Entertainment", "Shopping", "Other")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                val (icon, color) = getCategoryStyling(cat)
                
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(text = Localization.getString("category_${cat.lowercase()}", isFarsi)) },
                    leadingIcon = { Icon(imageVector = icon, contentDescription = cat, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.2f),
                        selectedLabelColor = color,
                        selectedLeadingIconColor = color
                    ),
                    modifier = Modifier.testTag("cat_chip_$cat")
                )
            }
        }

        // Payer Selector Row
        Text(
            text = Localization.getString("paid_by", isFarsi),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            members.forEach { m ->
                val grpId = m.userId?.removePrefix("GROUP_")?.toIntOrNull()
                val innerMembers = remember(allMembersList, grpId) { if (grpId != null && activeGroup?.groupType == "MULTI_GROUP_TRIP") allMembersList.filter { it.groupId == grpId } else emptyList() }

                if (innerMembers.isNotEmpty()) {
                    innerMembers.forEach { ind ->
                        val isSelected = (selectedPayerId == m.id && selectedActualPayerName == ind.name) || (selectedPayerId == m.id && selectedActualPayerName == null && ind == innerMembers.firstOrNull())
                        Card(
                            modifier = Modifier
                                .clickable {
                                    selectedPayerId = m.id
                                    selectedActualPayerName = ind.name
                                }
                                .testTag("payer_card_${m.id}_${ind.id}"),
                            colors = CardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = Color.Transparent
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(android.graphics.Color.parseColor(m.avatarColor)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = ind.name.take(1).uppercase(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "${ind.name} (${m.name})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val isSelected = selectedPayerId == m.id
                    Card(
                        modifier = Modifier
                            .clickable {
                                selectedPayerId = m.id
                                selectedActualPayerName = null
                            }
                            .testTag("payer_card_${m.id}"),
                        colors = CardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = Color.Transparent
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.parseColor(m.avatarColor)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = m.name.take(1).uppercase(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = m.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // How to Split Section
        Text(
            text = Localization.getString("split_type", isFarsi),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        val activeGroup = viewModel.groups.collectAsStateWithLifecycle().value.find { it.id == viewModel.selectedGroupId.collectAsStateWithLifecycle().value }
        val isFamilyMode = activeGroup?.groupType == "FAMILY_TRIP"
        val splitTypes = if (isFamilyMode) listOf("BY_HEADCOUNT", "EQUAL", "PERCENTAGE", "CUSTOM") else listOf("EQUAL", "BY_HEADCOUNT", "PERCENTAGE", "CUSTOM")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            splitTypes.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = splitType == type,
                    onClick = { splitType = type },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = splitTypes.size),
                    modifier = Modifier.testTag("split_btn_$type")
                ) {
                    Text(
                        text = when (type) {
                            "EQUAL" -> Localization.getString("equally", isFarsi)
                            "BY_HEADCOUNT" -> Localization.getString("by_headcount", isFarsi)
                            "PERCENTAGE" -> Localization.getString("by_percentage", isFarsi)
                            else -> Localization.getString("by_exact_amount", isFarsi)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Active split configuration row fields
        val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (splitType) {
                "EQUAL" -> {
                    val portion = if (members.isNotEmpty()) parsedAmount / members.size else 0.0
                    Text(
                        text = "• Each member owes " + Localization.formatCurrency(portion, isFarsi, customCurrency),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                "BY_HEADCOUNT" -> {
                    val totalHc = members.sumOf { it.headcount.coerceAtLeast(1) }
                    val costPerPerson = if (totalHc > 0) parsedAmount / totalHc else if (members.isNotEmpty()) parsedAmount / members.size else 0.0
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "• ${Localization.getString("cost_per_person", isFarsi)}: " + Localization.formatCurrency(costPerPerson, isFarsi, customCurrency),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        members.forEach { m ->
                            val hc = m.headcount.coerceAtLeast(1)
                            val fairShare = costPerPerson * hc
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${m.name} ($hc ${Localization.getString("headcount", isFarsi)})", fontSize = 12.sp)
                                Text(text = Localization.formatCurrency(fairShare, isFarsi, customCurrency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                "PERCENTAGE" -> {
                    var totalPercent = 0.0
                    members.forEach { m ->
                        val pct = customShares[m.id] ?: 0.0
                        totalPercent += pct
                        val actualCost = (pct / 100.0) * parsedAmount

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color(android.graphics.Color.parseColor(m.avatarColor)), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = m.name, fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = if (pct == 0.0) "" else pct.toString(),
                                    onValueChange = { input ->
                                        val doubleVal = input.toDoubleOrNull() ?: 0.0
                                        customShares[m.id] = doubleVal
                                    },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.width(70.dp).height(50.dp).testTag("percent_input_${m.id}"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "%", fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Percent: ${Localization.formatNumber(totalPercent, isFarsi)}% / 100%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (kotlin.math.abs(totalPercent - 100.0) < 0.1) ColorIncome else ColorExpense
                    )
                }
                "CUSTOM" -> {
                    var totalSum = 0.0
                    members.forEach { m ->
                        val exact = customShares[m.id] ?: 0.0
                        totalSum += exact

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color(android.graphics.Color.parseColor(m.avatarColor)), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = m.name, fontSize = 13.sp)
                            }
                            OutlinedTextField(
                                value = if (exact == 0.0) "" else exact.toString(),
                                onValueChange = { input ->
                                    val doubleVal = input.toDoubleOrNull() ?: 0.0
                                    customShares[m.id] = doubleVal
                                },
                                placeholder = { Text("0") },
                                modifier = Modifier.width(100.dp).height(50.dp).testTag("exact_input_${m.id}"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                             )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sum of splits: ${Localization.formatCurrency(totalSum, isFarsi, customCurrency)} / ${Localization.formatCurrency(parsedAmount, isFarsi, customCurrency)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (kotlin.math.abs(totalSum - parsedAmount) < 0.1) ColorIncome else ColorExpense
                    )
                }
            }
        }

        // Recurring option (Monthly)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.EventRepeat, contentDescription = "Recurring")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = Localization.getString("recurring", isFarsi),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Save details of future repeats",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    modifier = Modifier.testTag("recurring_toggle")
                )
            }
        }

        // Submit and Cancel Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = Localization.getString("cancel", isFarsi))
            }
            
            val context = LocalContext.current
            Button(
                onClick = {
                    val amtVal = amountStr.toDoubleOrNull()
                    if (title.isBlank() || amtVal == null || amtVal <= 0.0) {
                        Toast.makeText(context, Localization.getString("fill_required", isFarsi), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedPayerId == -1) {
                        Toast.makeText(context, Localization.getString("select_payer", isFarsi), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Validation of sums
                    if (splitType == "PERCENTAGE") {
                        val pctSum = customShares.values.sum()
                        if (kotlin.math.abs(pctSum - 100.0) > 0.1) {
                            Toast.makeText(context, Localization.getString("percentage_sum_error", isFarsi), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    } else if (splitType == "CUSTOM") {
                        val costSum = customShares.values.sum()
                        if (kotlin.math.abs(costSum - amtVal) > 0.1) {
                            Toast.makeText(context, Localization.getString("sum_not_match", isFarsi), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }

                    if (editingExpense == null) {
                        viewModel.addExpense(
                            title = title,
                            amount = amtVal,
                            category = selectedCategory,
                            payerId = selectedPayerId,
                            splitType = splitType,
                            customShares = customShares.toMap(),
                            isRecurring = isRecurring,
                            actualPayerName = selectedActualPayerName
                        )
                        if (isRecurring) {
                            viewModel.addRecurringSchedule(
                                title = title,
                                amount = amtVal,
                                category = selectedCategory,
                                payerId = selectedPayerId,
                                splitType = splitType,
                                frequency = "MONTHLY"
                            )
                        }
                    } else {
                        viewModel.updateExpense(
                            expenseId = editingExpense.id,
                            title = title,
                            amount = amtVal,
                            category = selectedCategory,
                            payerId = selectedPayerId,
                            splitType = splitType,
                            customShares = customShares.toMap(),
                            isRecurring = isRecurring,
                            timestamp = editingExpense.timestamp,
                            actualPayerName = selectedActualPayerName
                        )
                    }
                    onSaved()
                },
                enabled = (activeGroup?.isFinished != true),
                modifier = Modifier.weight(1.5f).testTag("save_expense_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = Localization.getString("save", isFarsi), fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ---------------- REPORTS SCREEN ----------------
@Composable
fun ReportsScreen(
    viewModel: SplitEaseViewModel,
    isFarsi: Boolean,
    customCurrency: String?
) {
    val members by viewModel.currentMembers.collectAsStateWithLifecycle()
    val expenses by viewModel.currentExpenses.collectAsStateWithLifecycle()
    val splits by viewModel.currentSplits.collectAsStateWithLifecycle()

    val reportMonth by viewModel.reportMonth.collectAsStateWithLifecycle()
    val reportYear by viewModel.reportYear.collectAsStateWithLifecycle()

    // Filter expenses matching selected month & year
    val filteredExpenses = remember(expenses, reportMonth, reportYear) {
        expenses.filter { expense ->
            val cal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
            (cal.get(Calendar.MONTH) + 1) == reportMonth && cal.get(Calendar.YEAR) == reportYear
        }
    }

    // Category breakdown totals
    val categoryTotals = remember(filteredExpenses) {
        val map = mutableMapOf<String, Double>()
        filteredExpenses.forEach { exp ->
            map[exp.category] = (map[exp.category] ?: 0.0) + exp.amount
        }
        map
    }

    // Member spending totals
    val memberTotals = remember(filteredExpenses, splits, members) {
        val map = mutableMapOf<Int, Double>()
        // Calculate each member's actual share of spending in that month
        filteredExpenses.forEach { exp ->
            val expSplits = splits.filter { it.expenseId == exp.id }
            expSplits.forEach { split ->
                map[split.memberId] = (map[split.memberId] ?: 0.0) + split.amount
            }
        }
        map
    }

    val totalSpending = filteredExpenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report Dashboard Title & Selector
        Text(
            text = Localization.getString("reports", isFarsi),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Month Selector card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (reportMonth == 1) {
                            viewModel.setReportMonth(12)
                            viewModel.setReportYear(reportYear - 1)
                        } else {
                            viewModel.setReportMonth(reportMonth - 1)
                        }
                    },
                    modifier = Modifier.testTag("prev_month_btn")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Prev")
                }

                // Localized Month/Year text display
                val monthNames = if (isFarsi) {
                    listOf("ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن", "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر")
                } else {
                    listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                }
                val monthText = monthNames[reportMonth - 1] + " " + Localization.formatNumber(reportYear.toDouble(), isFarsi)
                
                Text(
                    text = monthText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = {
                        if (reportMonth == 12) {
                            viewModel.setReportMonth(1)
                            viewModel.setReportYear(reportYear + 1)
                        } else {
                            viewModel.setReportMonth(reportMonth + 1)
                        }
                    },
                    modifier = Modifier.testTag("next_month_btn")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next")
                }
            }
        }

        // Spending Summary Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Localization.getString("total_spending", isFarsi),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Localization.formatCurrency(totalSpending, isFarsi, customCurrency),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (filteredExpenses.isEmpty()) {
            EmptyStateView(
                title = Localization.getString("no_expenses_yet", isFarsi),
                isFarsi = isFarsi
            )
        } else {
            // Category Distribution breakdown chart
            Text(
                text = Localization.getString("spending_by_category", isFarsi),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    categoryTotals.forEach { (cat, amount) ->
                        val pct = if (totalSpending > 0) amount / totalSpending else 0.0
                        val (_, color) = getCategoryStyling(cat)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = Localization.getString("category_${cat.lowercase()}", isFarsi),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = Localization.formatCurrency(amount, isFarsi, customCurrency) + " (" + Localization.formatNumber(pct * 100.0, isFarsi) + "%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            // Custom progress bar chart drawing representation
                            LinearProgressIndicator(
                                progress = pct.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // Spending by member chart
            Text(
                text = Localization.getString("spending_by_member", isFarsi),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    members.forEach { m ->
                        val amount = memberTotals[m.id] ?: 0.0
                        val pct = if (totalSpending > 0) amount / totalSpending else 0.0
                        val color = Color(android.graphics.Color.parseColor(m.avatarColor))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = m.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = Localization.formatCurrency(amount, isFarsi, customCurrency) + " (" + Localization.formatNumber(pct * 100.0, isFarsi) + "%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            LinearProgressIndicator(
                                progress = pct.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ---------------- GROUPS SCREEN ----------------
@Composable
fun GroupsScreen(
    viewModel: SplitEaseViewModel,
    isFarsi: Boolean,
    customCurrency: String?
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val allMembersList by viewModel.allMembers.collectAsStateWithLifecycle()
    val allExpensesList by viewModel.allExpenses.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateTripDialog by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var isTripsTab by remember { mutableStateOf(true) }
    var selectedGroupForAddMember by remember { mutableStateOf<Group?>(null) }

    // Filter groups based on active main tab (Trips vs Groups)
    val tabGroups = remember(groups, isTripsTab) {
        if (isTripsTab) groups.filter { it.groupType == "MULTI_GROUP_TRIP" }
        else groups.filter { it.groupType != "MULTI_GROUP_TRIP" }
    }
    val activeGroupsList = remember(tabGroups) { tabGroups.filter { !it.isFinished } }
    val archivedGroupsList = remember(tabGroups) { tabGroups.filter { it.isFinished } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Segmented Main Tabs: Trips & Parties vs Groups & Families
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val mainTabs = listOf(
                true to Localization.getString("trips_and_parties", isFarsi),
                false to Localization.getString("groups_and_families", isFarsi)
            )
            mainTabs.forEach { (isTrips, label) ->
                val selected = isTripsTab == isTrips
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { isTripsTab = isTrips }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isTripsTab) Localization.getString("trips_and_parties", isFarsi) else Localization.getString("groups", isFarsi),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { if (isTripsTab) showCreateTripDialog = true else showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("create_new_group_btn")
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isTripsTab) Localization.getString("create_trip_party", isFarsi) else Localization.getString("create_group", isFarsi),
                    fontSize = 12.sp
                )
            }
        }

        // Custom styled filter selector tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                false to Localization.getString("active_groups", isFarsi),
                true to Localization.getString("history_archive", isFarsi)
            )
            tabs.forEach { (isArchivedTab, label) ->
                val selected = showArchived == isArchivedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { showArchived = isArchivedTab }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val displayedGroups = if (showArchived) archivedGroupsList else activeGroupsList

        if (displayedGroups.isEmpty()) {
            EmptyStateView(
                title = if (isTripsTab && !showArchived) Localization.getString("no_trips_yet", isFarsi) else Localization.getString(if (showArchived) "no_settlements_yet" else "no_groups_yet", isFarsi),
                isFarsi = isFarsi
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayedGroups) { group ->
                    val isSelected = group.id == selectedGroupId
                    val groupMembers = remember(allMembersList, group.id) { allMembersList.filter { it.groupId == group.id } }
                    val groupExpenses = remember(allExpensesList, group.id) { allExpensesList.filter { it.groupId == group.id } }
                    val totalSpent = groupExpenses.sumOf { it.amount }
                    val totalHeadcount = groupMembers.sumOf { it.headcount.coerceAtLeast(1) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectGroup(group.id) }
                            .testTag("group_card_${group.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (group.isFinished) Icons.Rounded.Archive else Icons.Rounded.Groups,
                                        contentDescription = "Group Icon",
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = group.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    // Outing Trip Date
                                    val dateStr = if (group.outingDate != null) {
                                        val sdf = if (isFarsi) SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) else SimpleDateFormat("MMM dd, yyyy", Locale.US)
                                        val rawDate = sdf.format(Date(group.outingDate))
                                        if (isFarsi) Localization.formatPersianDigits(rawDate) else rawDate
                                    } else {
                                        Localization.getString("not_set", isFarsi)
                                    }
                                    
                                    Text(
                                        text = "${Localization.getString("outing_date", isFarsi)}: $dateStr",
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Status Badge & Type Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        val badgeColor = if (group.isFinished) MaterialTheme.colorScheme.outline else Color(0xFF4CAF50)
                                        val statusText = Localization.getString(if (group.isFinished) "finished" else "ongoing", isFarsi)
                                        Box(
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        }

                                        val typeBadgeColor = when (group.groupType) {
                                            "MULTI_GROUP_TRIP" -> Color(0xFF9C27B0)
                                            "FAMILY_TRIP" -> Color(0xFFE91E63)
                                            else -> Color(0xFF2196F3)
                                        }
                                        val typeText = when (group.groupType) {
                                            "MULTI_GROUP_TRIP" -> if (isFarsi) "سفر چند‌گروهی ($totalHeadcount نفر)" else "Multi-Group ($totalHeadcount hc)"
                                            "FAMILY_TRIP" -> if (isFarsi) "خانوادگی ($totalHeadcount نفر)" else "Family ($totalHeadcount hc)"
                                            else -> if (isFarsi) "انفرادی/دوستانه (${groupMembers.size} نفر)" else "Standard (${groupMembers.size} m)"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(typeBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = typeText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = typeBadgeColor
                                            )
                                        }
                                    }

                                    if (groupMembers.isNotEmpty()) {
                                        val membersStr = when (group.groupType) {
                                            "MULTI_GROUP_TRIP" -> {
                                                groupMembers.joinToString("، ") { m ->
                                                    val spentByM = groupExpenses.filter { it.payerId == m.id }.sumOf { it.amount }
                                                    val spentTxt = if (spentByM > 0) (if (isFarsi) " - پرداخت: " else " - paid: ") + Localization.formatCurrency(spentByM, isFarsi, customCurrency) else ""
                                                    "${m.name} (${m.headcount} نفر$spentTxt)"
                                                }
                                            }
                                            else -> {
                                                val master = groupMembers.firstOrNull()?.name
                                                val masterTxt = if (master != null && group.groupType == "FAMILY_TRIP") "👑 ${Localization.getString("master_payer", isFarsi)}: $master • " else ""
                                                masterTxt + groupMembers.joinToString("، ") { it.name }
                                            }
                                        }
                                        val labelPrefix = if (group.groupType == "MULTI_GROUP_TRIP") {
                                            if (isFarsi) "گروه‌های شرکت‌کننده: " else "Participating Groups: "
                                        } else {
                                            if (isFarsi) "اعضا: " else "Members: "
                                        }
                                        Text(
                                            text = labelPrefix + membersStr,
                                            fontSize = 11.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    if (totalSpent > 0 || group.isFinished) {
                                        Text(
                                            text = (if (isFarsi) "مجموع مخارج: " else "Total Spent: ") + Localization.formatCurrency(totalSpent, isFarsi, customCurrency),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!group.isFinished) {
                                    IconButton(
                                        onClick = { selectedGroupForAddMember = group },
                                        modifier = Modifier.testTag("add_member_btn_${group.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PersonAdd,
                                            contentDescription = "Add Member",
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Toggle finish / ongoing
                                IconButton(
                                    onClick = { viewModel.toggleGroupFinished(group) },
                                    modifier = Modifier.testTag("toggle_archive_btn_${group.id}")
                                ) {
                                    Icon(
                                        imageVector = if (group.isFinished) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                        contentDescription = "Toggle Archived Status",
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteGroup(group) },
                                    modifier = Modifier.testTag("delete_group_btn_${group.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else ColorExpense
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Group Dialog popup
    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        var outingDate by remember { mutableStateOf<Long?>(null) }
        var groupType by remember { mutableStateOf("STANDARD") }
        val memberList = remember { mutableStateListOf("", "", "") } // Initial 3 empty member fields
        val memberHeadcounts = remember { mutableStateListOf(1, 1, 1) }

        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = Localization.getString("create_group", isFarsi),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(text = Localization.getString("group_name", isFarsi)) },
                        modifier = Modifier.fillMaxWidth().testTag("new_group_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = Localization.getString("group_type", isFarsi),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = groupType == "STANDARD",
                            onClick = { groupType = "STANDARD" },
                            label = { Text(Localization.getString("standard_group", isFarsi), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = groupType == "FAMILY_TRIP",
                            onClick = { groupType = "FAMILY_TRIP" },
                            label = { Text(Localization.getString("family_trip_group", isFarsi), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = Localization.getString("outing_date", isFarsi),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    val context = LocalContext.current
                    val calendar = Calendar.getInstance()
                    val datePickerDialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }
                            outingDate = selectedCal.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { datePickerDialog.show() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateText = if (outingDate != null) {
                            val sdf = if (isFarsi) SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) else SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            val rawDate = sdf.format(Date(outingDate!!))
                            if (isFarsi) Localization.formatPersianDigits(rawDate) else rawDate
                        } else {
                            Localization.getString("not_set", isFarsi)
                        }
                        
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (outingDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = Localization.getString("set_date", isFarsi),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = Localization.getString("group_members", isFarsi),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👑", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.getString("master_payer_hint", isFarsi),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    memberList.forEachIndexed { index, mName ->
                        val labelTxt = if (index == 0) {
                            "${Localization.getString("member_name", isFarsi)} ۱ (👑 ${Localization.getString("master_payer", isFarsi)})"
                        } else {
                            "${Localization.getString("member_name", isFarsi)} ${Localization.formatNumber((index+1).toDouble(), isFarsi)}"
                        }
                        OutlinedTextField(
                            value = mName,
                            onValueChange = { memberList[index] = it },
                            label = { Text(text = labelTxt) },
                            modifier = Modifier.fillMaxWidth().testTag("new_member_input_$index"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Add member button within list
                    TextButton(
                        onClick = {
                            memberList.add("")
                            memberHeadcounts.add(1)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Member")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = Localization.getString("add_member", isFarsi))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = Localization.getString("cancel", isFarsi))
                        }
                        Button(
                            onClick = {
                                if (groupName.isNotBlank()) {
                                    viewModel.createGroup(groupName, memberList.toList(), outingDate, false, groupType, emptyList())
                                    showCreateDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.5f).testTag("save_group_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = Localization.getString("save", isFarsi), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCreateTripDialog) {
        var tripName by remember { mutableStateOf("") }
        var outingDate by remember { mutableStateOf<Long?>(null) }
        val availableGroups = remember(groups) { groups.filter { !it.isFinished && it.groupType != "MULTI_GROUP_TRIP" } }
        val selectedGroupIds = remember { mutableStateListOf<Int>() }

        Dialog(onDismissRequest = { showCreateTripDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = Localization.getString("create_trip_party", isFarsi),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = tripName,
                        onValueChange = { tripName = it },
                        label = { Text(text = if (isFarsi) "نام سفر یا مهمانی" else "Trip or Party Name") },
                        modifier = Modifier.fillMaxWidth().testTag("new_trip_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = Localization.getString("outing_date", isFarsi),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    val context = LocalContext.current
                    val calendar = Calendar.getInstance()
                    val datePickerDialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }
                            outingDate = selectedCal.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { datePickerDialog.show() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateText = if (outingDate != null) {
                            val sdf = if (isFarsi) SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) else SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            val rawDate = sdf.format(Date(outingDate!!))
                            if (isFarsi) Localization.formatPersianDigits(rawDate) else rawDate
                        } else {
                            Localization.getString("not_set", isFarsi)
                        }
                        
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (outingDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = Localization.getString("set_date", isFarsi),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = Localization.getString("select_participating_groups", isFarsi),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    if (availableGroups.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isFarsi) "ابتدا در تب «گروه‌ها و خانواده‌ها» حداقل یک گروه یا خانواده ایجاد کنید." else "First create at least one group or family in the 'Groups & Families' tab.",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        availableGroups.forEach { grp ->
                            val grpMembers = remember(allMembersList, grp.id) { allMembersList.filter { it.groupId == grp.id } }
                            val totalHc = grpMembers.sumOf { it.headcount.coerceAtLeast(1) }.coerceAtLeast(1)
                            val isChecked = selectedGroupIds.contains(grp.id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable {
                                        if (isChecked) selectedGroupIds.remove(grp.id) else selectedGroupIds.add(grp.id)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { chk ->
                                            if (chk) selectedGroupIds.add(grp.id) else selectedGroupIds.remove(grp.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        val masterPayer = if (grp.groupType == "FAMILY_TRIP") grpMembers.firstOrNull()?.name else null
                                        Text(
                                            text = grp.name + if (masterPayer != null) " (👑 ${Localization.getString("master_payer", isFarsi)}: $masterPayer)" else "",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isFarsi) "شامل ${totalHc} نفر (${grpMembers.joinToString("، ") { it.name }})" else "${totalHc} people (${grpMembers.joinToString(", ") { it.name }})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateTripDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = Localization.getString("cancel", isFarsi))
                        }
                        Button(
                            onClick = {
                                if (tripName.isNotBlank() && selectedGroupIds.isNotEmpty()) {
                                    viewModel.createMultiGroupTrip(tripName.trim(), selectedGroupIds.toList(), outingDate)
                                    showCreateTripDialog = false
                                }
                            },
                            enabled = tripName.isNotBlank() && selectedGroupIds.isNotEmpty(),
                            modifier = Modifier.weight(1.5f).testTag("save_trip_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = Localization.getString("save", isFarsi), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (selectedGroupForAddMember != null) {
        val grp = selectedGroupForAddMember!!
        var newMemberName by remember { mutableStateOf("") }
        var newMemberHeadcount by remember { mutableStateOf(1) }

        Dialog(onDismissRequest = { selectedGroupForAddMember = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isFarsi) "افزودن عضو جدید به ${grp.name}" else "Add Member to ${grp.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text(Localization.getString("member_name", isFarsi)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (grp.groupType == "FAMILY_TRIP" || grp.groupType == "MULTI_GROUP_TRIP") {
                        OutlinedTextField(
                            value = newMemberHeadcount.toString(),
                            onValueChange = { str -> newMemberHeadcount = str.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                            label = { Text(Localization.getString("headcount", isFarsi)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    if (grp.groupType == "MULTI_GROUP_TRIP") {
                        val availableToAdd = remember(groups, allMembersList) {
                            val existingGrpIds = allMembersList.filter { it.groupId == grp.id }.mapNotNull { m -> m.userId?.removePrefix("GROUP_")?.toIntOrNull() }
                            groups.filter { !it.isFinished && it.groupType != "MULTI_GROUP_TRIP" && it.id !in existingGrpIds }
                        }
                        if (availableToAdd.isNotEmpty()) {
                            Text(
                                text = if (isFarsi) "انتخاب سریع از گروه‌های موجود:" else "Quick Select from existing groups:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableToAdd.forEach { addGrp ->
                                    val addMems = remember(allMembersList, addGrp.id) { allMembersList.filter { it.groupId == addGrp.id } }
                                    val totalHc = addMems.sumOf { it.headcount.coerceAtLeast(1) }.coerceAtLeast(1)
                                    val masterPayer = if (addGrp.groupType == "FAMILY_TRIP") addMems.firstOrNull()?.name else null
                                    val dispName = if (masterPayer != null) "${addGrp.name} (👑 $masterPayer)" else addGrp.name
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val colors = listOf("#FF6B6B", "#4DABF7", "#51CF66", "#FCC419", "#FF922B", "#CC5DE8", "#20C997")
                                            viewModel.addMember(dispName, colors.random(), totalHc, userId = "GROUP_${addGrp.id}", targetGroupId = grp.id)
                                            selectedGroupForAddMember = null
                                        },
                                        label = { Text("$dispName ($totalHc نفر)", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedGroupForAddMember = null }) {
                            Text(Localization.getString("cancel", isFarsi))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newMemberName.isNotBlank()) {
                                    val colors = listOf("#FF6B6B", "#4DABF7", "#51CF66", "#FCC419", "#FF922B", "#CC5DE8", "#20C997")
                                    val randomColor = colors.random()
                                    viewModel.addMember(newMemberName.trim(), randomColor, newMemberHeadcount, targetGroupId = grp.id)
                                    selectedGroupForAddMember = null
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(Localization.getString("add", isFarsi))
                        }
                    }
                }
            }
        }
    }
}


// ---------------- SETTINGS SCREEN ----------------
@Composable
fun SettingsScreen(
    viewModel: SplitEaseViewModel,
    isFarsi: Boolean,
    customCurrency: String?
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    var currencyInput by remember { mutableStateOf(customCurrency ?: "") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = Localization.getString("settings", isFarsi),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Theme Row Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        contentDescription = "Theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Localization.getString("theme", isFarsi),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = Localization.getString(if (isDarkMode) "dark_mode" else "light_mode", isFarsi),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleTheme() },
                    modifier = Modifier.testTag("settings_theme_switch")
                )
            }
        }

        // Language Row Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = "Language",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Localization.getString("language", isFarsi),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isFarsi) "فارسی (Persian RTL)" else "English (LTR)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Button(
                    onClick = { viewModel.toggleLanguage() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("settings_lang_btn")
                ) {
                    Text(text = if (isFarsi) "English" else "فارسی", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Currency Setting Customize
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Paid,
                        contentDescription = "Currency",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Localization.getString("currency", isFarsi),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Customize symbol displayed next to amounts",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currencyInput,
                        onValueChange = { currencyInput = it },
                        placeholder = { Text(text = if (isFarsi) "تومان" else "$") },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("currency_unit_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.setCustomCurrency(if (currencyInput.isBlank()) null else currencyInput.trim())
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_currency_btn")
                    ) {
                        Text(text = Localization.getString("save", isFarsi), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recurring Bills Manager
        val schedules by viewModel.currentSchedules.collectAsStateWithLifecycle()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EventRepeat,
                        contentDescription = "Recurring",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Localization.getString("recurring_bills", isFarsi),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Automatic scheduled expenses for active group",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                if (schedules.isEmpty()) {
                    Text(
                        text = Localization.getString("no_schedules", isFarsi),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    schedules.forEach { sch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = sch.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "${Localization.formatCurrency(sch.amount, isFarsi, customCurrency)} (${sch.frequency})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteRecurringSchedule(sch) }) {
                                Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete Schedule", tint = ColorExpense)
                            }
                        }
                    }
                }
            }
        }

        // About / Offline Mode Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isFarsi) "حالت آفلاین و ماشین‌حساب سریع" else "Offline Calculator Utility",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isFarsi) "تمامی محاسبات، بایگانی‌ها و اطلاعات به‌صورت محلی و بدون نیاز به اینترنت یا ورود به حساب کاربری ذخیره می‌شوند." else "All splitting, history, and records are stored 100% locally on device without needing internet or login.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}


// ---------------- EMPTY STATE VIEW ----------------
@Composable
fun EmptyStateView(title: String, isFarsi: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.empty_state_ill_1783405942502),
                contentDescription = "Empty illustration",
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
