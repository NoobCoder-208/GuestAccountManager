package com.chuong.guestmng.presentation.ui

import com.chuong.guestmng.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chuong.guestmng.domain.model.GuestAccount
import com.chuong.guestmng.presentation.viewmodel.CreatedAtFilter
import com.chuong.guestmng.presentation.viewmodel.GuestViewModel
import com.chuong.guestmng.presentation.viewmodel.SortOption
import com.chuong.guestmng.presentation.viewmodel.TokenStatusFilter
import com.chuong.guestmng.presentation.viewmodel.UiState
import com.chuong.guestmng.presentation.viewmodel.AccountStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Navigation route definitions
sealed class AppScreen(val route: String, val title: String, val icon: ImageVector) {
    object Accounts : AppScreen("accounts", "Danh sách tài khoản", Icons.Default.People)
    object Stats : AppScreen("stats", "Thống kê dữ liệu", Icons.Default.BarChart)
    object Backup : AppScreen("backup", "Sao lưu & Khôi phục", Icons.Default.Backup)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: GuestViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Observe streams from ViewModel
    val accounts by viewModel.filteredAccounts.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGroupFilters by viewModel.selectedGroups.collectAsStateWithLifecycle()
    val selectedRegions by viewModel.selectedRegions.collectAsStateWithLifecycle()
    val selectedTokenStatus by viewModel.selectedTokenStatus.collectAsStateWithLifecycle()
    val selectedCreatedAtFilter by viewModel.selectedCreatedAtFilter.collectAsStateWithLifecycle()
    val activeSortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedAccountIds.collectAsStateWithLifecycle()
    val availableRegions by viewModel.availableRegions.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    // Screen navigation state (Draw list)
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Accounts) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Modals state management
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<GuestAccount?>(null) }
    var accountToShowDetails by remember { mutableStateOf<GuestAccount?>(null) }

    // Group preset possibilities
    val groupPresets = listOf("Main", "Farm", "Test", "Guest", "Custom")

    // Listen to UI Toast alerts
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Modal Drawer wrapper
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "GuestManager Pro",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${accounts.size} Accounts Managed",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Navigation options
                listOf(AppScreen.Accounts, AppScreen.Stats, AppScreen.Backup).forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("nav_drawer_item_${screen.route}"),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Soph_ActiveBlueBg,
                            selectedIconColor = Soph_ActiveBlueText,
                            selectedTextColor = Soph_ActiveBlueText,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Duy trì hoạt động ngoại tuyến ổn định",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    title = {
                        if (currentScreen == AppScreen.Accounts && selectedIds.isNotEmpty()) {
                            Text(
                                "Đã chọn: ${selectedIds.size}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Text(currentScreen.title, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        if (currentScreen == AppScreen.Accounts) {
                            if (selectedIds.isNotEmpty()) {
                                // Multi-select actions
                                IconButton(
                                    onClick = { viewModel.refreshSelectedAccounts() },
                                    modifier = Modifier.testTag("bulk_refresh_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Làm mới mục chọn")
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSelectedAccounts() },
                                    modifier = Modifier.testTag("bulk_delete_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa các mục chọn", tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(
                                    onClick = { viewModel.clearAccountSelection() },
                                    modifier = Modifier.testTag("clear_select_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Bỏ chọn")
                                }
                            } else {
                                // Default Search and filter actions
                                IconButton(onClick = { viewModel.refreshAllAccounts() }) {
                                    Icon(imageVector = Icons.Default.Cached, contentDescription = "Làm mới tất cả")
                                }
                                IconButton(
                                    onClick = { showFilterBottomSheet = true },
                                    modifier = Modifier.testTag("open_filter_button")
                                ) {
                                    val isFiltering = selectedGroupFilters.isNotEmpty() ||
                                            selectedRegions.isNotEmpty() ||
                                            selectedTokenStatus.isNotEmpty() ||
                                            selectedCreatedAtFilter != CreatedAtFilter.ALL
                                    BadgedBox(
                                        badge = {
                                            if (isFiltering) {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                    Text("!")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Mở bộ lọc")
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                if (currentScreen == AppScreen.Accounts) {
                    SmallFloatingActionButton(
                        onClick = { showAddBottomSheet = true },
                        modifier = Modifier.testTag("add_account_fab"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm tài khoản")
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
                when (currentScreen) {
                    AppScreen.Accounts -> {
                        AccountsFlowSection(
                            accounts = accounts,
                            stats = stats,
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.searchQuery.value = it },
                            selectedGroupFilters = selectedGroupFilters,
                            onGroupToggle = { group ->
                                val updated = selectedGroupFilters.toMutableSet()
                                if (group in updated) updated.remove(group) else updated.add(group)
                                viewModel.selectedGroups.value = updated
                            },
                            groupPresets = groupPresets,
                            selectedIds = selectedIds,
                            onItemClick = { account ->
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.toggleAccountSelection(account.localId)
                                } else {
                                    accountToShowDetails = account
                                }
                            },
                            onItemLongPress = { account ->
                                viewModel.toggleAccountSelection(account.localId)
                            },
                            selectAllFiltered = { viewModel.selectAllFiltered() },
                            onDeleteAccount = { viewModel.deleteAccount(it.localId) },
                            onRefreshAccount = { viewModel.refreshSingleAccount(it.localId) },
                            onEditAccount = { accountToEdit = it },
                            isRefreshing = isRefreshing,
                            onRefreshTrigger = { viewModel.refreshAllAccounts() }
                        )
                    }
                    AppScreen.Stats -> {
                        StatsSection(stats = stats)
                    }
                    AppScreen.Backup -> {
                        BackupSection(
                            viewModel = viewModel,
                            actionState = actionState
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet: Add Account
    if (showAddBottomSheet) {
        AddAccountBottomSheet(
            onDismiss = { showAddBottomSheet = false },
            onAdd = { uid, pwd, groups, note ->
                viewModel.addAccount(uid, pwd, groups, note)
                showAddBottomSheet = false
            },
            groupPresets = groupPresets
        )
    }

    // Bottom Sheet: Edit Account Groups & Note
    accountToEdit?.let { account ->
        EditAccountGroupsBottomSheet(
            account = account,
            onDismiss = { accountToEdit = null },
            onSave = { groups, note ->
                viewModel.updateAccountGroupsAndNote(account, groups, note)
                accountToEdit = null
            },
            groupPresets = groupPresets
        )
    }

    // Bottom Sheet: Advanced Filter System
    if (showFilterBottomSheet) {
        AdvancedFilterBottomSheet(
            onDismiss = { showFilterBottomSheet = false },
            availableRegions = availableRegions,
            selectedRegions = selectedRegions,
            onRegionsChange = { viewModel.selectedRegions.value = it },
            selectedTokenStatus = selectedTokenStatus,
            onTokenStatusChange = { viewModel.selectedTokenStatus.value = it },
            selectedCreatedAtFilter = selectedCreatedAtFilter,
            onCreatedAtFilterChange = { viewModel.selectedCreatedAtFilter.value = it },
            sortOption = activeSortOption,
            onSortOptionChange = { viewModel.sortOption.value = it },
            onClearAll = {
                viewModel.resetFilters()
                showFilterBottomSheet = false
            }
        )
    }

    // Dialog: Full Account Attributes details
    accountToShowDetails?.let { account ->
        AccountDetailsDialog(
            account = account,
            onDismiss = { accountToShowDetails = null },
            groupPresets = groupPresets,
            onEdit = { 
                accountToShowDetails = null
                accountToEdit = account
            },
            onRefresh = {
                accountToShowDetails = null
                viewModel.refreshSingleAccount(account.localId)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsFlowSection(
    accounts: List<GuestAccount>,
    stats: AccountStats,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedGroupFilters: Set<String>,
    onGroupToggle: (String) -> Unit,
    groupPresets: List<String>,
    selectedIds: Set<Int>,
    onItemClick: (GuestAccount) -> Unit,
    onItemLongPress: (GuestAccount) -> Unit,
    selectAllFiltered: () -> Unit,
    onDeleteAccount: (GuestAccount) -> Unit,
    onRefreshAccount: (GuestAccount) -> Unit,
    onEditAccount: (GuestAccount) -> Unit,
    isRefreshing: Boolean,
    onRefreshTrigger: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Soph_ActiveBlueBg),
                border = BorderStroke(1.dp, Soph_ActiveBlueBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "CÒN HẠN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Soph_ActiveBlueText,
                        style = TextStyle(letterSpacing = 0.5.sp)
                    )
                    Text(
                        text = stats.tokenActive.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFFE2E2E6),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Soph_ExpiredRedBg),
                border = BorderStroke(1.dp, Soph_ExpiredRedBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "HẾT HẠN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Soph_ExpiredRedText,
                        style = TextStyle(letterSpacing = 0.5.sp)
                    )
                    Text(
                        text = stats.tokenExpired.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFFE2E2E6),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Soph_NormalGreenBg),
                border = BorderStroke(1.dp, Soph_NormalGreenBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "MỚI HÔM NAY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Soph_NormalGreenText,
                        style = TextStyle(letterSpacing = 0.5.sp)
                    )
                    val prefix = if (stats.createdToday > 0) "+" else ""
                    Text(
                        text = "$prefix${stats.createdToday}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFFE2E2E6),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Selected indicator if needed
        if (selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đang chọn ${selectedIds.size} tài khoản",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Chọn tất cả",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { selectAllFiltered() }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .testTag("search_text_input"),
            placeholder = { Text("Search UID, Nickname, or Region...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            groupPresets.forEach { group ->
                val isSelected = group in selectedGroupFilters
                FilterChip(
                    selected = isSelected,
                    onClick = { onGroupToggle(group) },
                    label = { Text(group, fontSize = 12.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Main List area featuring Skeleton Loading UI
        Box(modifier = Modifier.weight(1f)) {
            if (isRefreshing) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(6) {
                        LoadingSkeletonItem()
                    }
                }
            } else if (accounts.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    color = Color.Transparent
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Trống",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Danh sách trống",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Không tìm thấy tài khoản thích hợp. Hãy thử thay đổi bộ lọc hoặc thêm tài khoản mới bằng nút '+' phía dưới.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(accounts, key = { it.localId }) { account ->
                        val isSelected = account.localId in selectedIds
                        GuestAccountListItem(
                            account = account,
                            isSelected = isSelected,
                            onClick = { onItemClick(account) },
                            onLongClick = { onItemLongPress(account) },
                            onDelete = { onDeleteAccount(account) },
                            onRefresh = { onRefreshAccount(account) },
                            onEdit = { onEditAccount(account) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GuestAccountListItem(
    account: GuestAccount,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, valText: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, valText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label đã sao chép!", Toast.LENGTH_SHORT).show()
    }

    val itemBorder = if (isSelected) {
        BorderStroke(1.dp, Soph_ActiveBlueBorder)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val itemBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("account_item_${account.localId}")
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        border = itemBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else if (account.isExpired()) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            else Color(0xFF3F4759)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        val initials = if (account.nickname.contains("_") || account.nickname.contains(" ")) {
                            val parts = account.nickname.split("_", " ", "-")
                            (parts.getOrNull(0)?.take(1) ?: "").uppercase() + (parts.getOrNull(1)?.take(1) ?: "").uppercase()
                        } else {
                            account.nickname.take(2).uppercase()
                        }
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (account.isExpired()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else Soph_ActiveBlueText
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.nickname,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (account.isExpired()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "UID: ${account.uid}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LVL ${account.level}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Soph_NormalGreenText,
                            modifier = Modifier
                                .background(Soph_NormalGreenBg, RoundedCornerShape(4.dp))
                                .border(1.dp, Soph_NormalGreenBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Khu vực: ${account.region}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val statusLabel: String
                    val statusBg: Color
                    val statusBorder: Color
                    val statusTextCol: Color

                    when {
                        account.isExpired() -> {
                            statusLabel = "EXPIRED"
                            statusBg = Soph_ExpiredRedBg
                            statusBorder = Soph_ExpiredRedBorder
                            statusTextCol = Soph_ExpiredRedText
                        }
                        account.isNearExpired() -> {
                            statusLabel = "WARNING"
                            statusBg = Color(0xFF312811).copy(alpha = 0.5f)
                            statusBorder = Color(0xFF54463E).copy(alpha = 0.4f)
                            statusTextCol = Color(0xFFFFF3CD).copy(alpha = 0.7f)
                        }
                        else -> {
                            statusLabel = "ACTIVE"
                            statusBg = Soph_NormalGreenBg
                            statusBorder = Soph_NormalGreenBorder
                            statusTextCol = Soph_NormalGreenText
                        }
                    }

                    Text(
                        text = statusLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextCol,
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(4.dp))
                            .border(1.dp, statusBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand controls",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))

                    if (account.groupNames.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Nhóm",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                account.groupNames.forEach { group ->
                                    Text(
                                        text = group,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (account.note.isNotBlank()) {
                        Text(
                            text = "Ghi chú: ${account.note}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { copyToClipboard("UID", account.uid) },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.height(26.dp).testTag("copy_uid_${account.localId}")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Sao chép UID", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { copyToClipboard("Access Token", account.accessToken) },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.height(26.dp).testTag("copy_access_${account.localId}")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Access Token", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { copyToClipboard("JWT Token", account.token) },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.height(26.dp).testTag("copy_jwt_${account.localId}")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("JWT Token", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateForm = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val expStr = if (account.expiresAt <= 0) "Mãi mãi" else dateForm.format(Date(account.expiresAt))

                        Text(
                            text = "Hết hạn: $expStr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledIconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(26.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit groups", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            FilledIconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(26.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh API", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            FilledIconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(26.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingSkeletonItem() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f))
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f))
            )
        }
    }
}

// Bottom Sheet: Add Account Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (String, String, Set<String>, String) -> Unit,
    groupPresets: List<String>
) {
    val sheetState = rememberModalBottomSheetState()
    var uidVal by remember { mutableStateOf("") }
    var pwdVal by remember { mutableStateOf("") }
    var noteVal by remember { mutableStateOf("") }
    var chosenGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var passwordVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("add_account_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Thêm Tài Khoản Mới",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Nhập UID & Mật khẩu để lấy mã cấu hình từ API",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = uidVal,
                onValueChange = { uidVal = it },
                label = { Text("UID tài khoản") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_uid_input"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pwdVal,
                onValueChange = { pwdVal = it },
                label = { Text("Mật khẩu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_password_input"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Thuộc nhóm:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupPresets.forEach { group ->
                    val isChecked = group in chosenGroups
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            chosenGroups = if (isChecked) chosenGroups - group else chosenGroups + group
                        },
                        label = { Text(group) },
                        leadingIcon = if (isChecked) {
                            { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = noteVal,
                onValueChange = { noteVal = it },
                label = { Text("Ghi chú bổ sung (Tùy chọn)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_note_input"),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onAdd(uidVal, pwdVal, chosenGroups, noteVal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_account_button"),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("LẤY TOKEN & LƯU TRỮ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// Bottom Sheet: Edit account groups / metadata
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountGroupsBottomSheet(
    account: GuestAccount,
    onDismiss: () -> Unit,
    onSave: (Set<String>, String) -> Unit,
    groupPresets: List<String>
) {
    val sheetState = rememberModalBottomSheetState()
    var chosenGroups by remember { mutableStateOf(account.groupNames) }
    var noteVal by remember { mutableStateOf(account.note) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Sửa Thông Tin Thiết Lập",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Tài khoản: ${account.nickname}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Select groups Row
            Text("Nhóm phân bổ:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupPresets.forEach { group ->
                    val isChecked = group in chosenGroups
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            chosenGroups = if (isChecked) chosenGroups - group else chosenGroups + group
                        },
                        label = { Text(group) },
                        leadingIcon = if (isChecked) {
                            { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = noteVal,
                onValueChange = { noteVal = it },
                label = { Text("Ghi chú") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSave(chosenGroups, noteVal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("LƯU THAY ĐỔI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// Bottom Sheet: Filters and Sorting
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterBottomSheet(
    onDismiss: () -> Unit,
    availableRegions: List<String>,
    selectedRegions: Set<String>,
    onRegionsChange: (Set<String>) -> Unit,
    selectedTokenStatus: Set<TokenStatusFilter>,
    onTokenStatusChange: (Set<TokenStatusFilter>) -> Unit,
    selectedCreatedAtFilter: CreatedAtFilter,
    onCreatedAtFilterChange: (CreatedAtFilter) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("filter_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bộ lọc & Sắp xếp nâng cao",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onClearAll) {
                    Text("Xóa tất cả bộ lọc", color = MaterialTheme.colorScheme.error)
                }
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // 1. Regions presets list
            Text("Lọc theo Khu vực (Region):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (availableRegions.isEmpty()) {
                Text(
                    "Chưa có dữ liệu vùng miền",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableRegions.forEach { region ->
                        val isChecked = region in selectedRegions
                        FilterChip(
                            selected = isChecked,
                            onClick = {
                                val updated = selectedRegions.toMutableSet()
                                if (region in updated) updated.remove(region) else updated.add(region)
                                onRegionsChange(updated)
                            },
                            label = { Text(region) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Token Status
            Text("Trạng thái Token:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TokenStatusFilter.values().forEach { status ->
                    val isChecked = status in selectedTokenStatus
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            val updated = selectedTokenStatus.toMutableSet()
                            if (status in updated) updated.remove(status) else updated.add(status)
                            onTokenStatusChange(updated)
                        },
                        label = { Text(status.displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // 3. Created At filter
            Text("Bộ lọc theo ngày tạo:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreatedAtFilter.values().forEach { filter ->
                    val isChecked = filter == selectedCreatedAtFilter
                    FilterChip(
                        selected = isChecked,
                        onClick = { onCreatedAtFilterChange(filter) },
                        label = { Text(filter.displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // 4. Sorting logic options
            Text("Sắp xếp danh sách:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Column(modifier = Modifier.padding(top = 8.dp)) {
                SortOption.values().forEach { option ->
                    val isChecked = option == sortOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOptionChange(option) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isChecked,
                            onClick = { onSortOptionChange(option) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option.displayName,
                            fontSize = 14.sp,
                            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("ÁP DỤNG", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Dialog window: Detailed full fields copy controls and visual view
@Composable
fun AccountDetailsDialog(
    account: GuestAccount,
    onDismiss: () -> Unit,
    groupPresets: List<String>,
    onEdit: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    fun copy(label: String, txt: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, txt)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Đã chép: $label!", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Thông Tin Chi Tiết Guest",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Rows
                DetailRow(label = "Nickname", value = account.nickname, onCopy = { copy("Nickname", account.nickname) })
                DetailRow(label = "UID", value = account.uid, onCopy = { copy("UID", account.uid) })
                DetailRow(label = "Tên hiển thị", value = account.name, onCopy = { copy("Tên hiển thị", account.name) })
                DetailRow(label = "Cấp độ (Level)", value = account.level.toString())
                DetailRow(label = "Khu vực (Region)", value = account.region)
                DetailRow(label = "Open ID", value = account.openId, onCopy = { copy("Open ID", account.openId) })
                DetailRow(label = "Server URL", value = account.serverUrl, onCopy = { copy("Server URL", account.serverUrl) })
                
                DetailRow(
                    label = "Hạn Token (Expires)",
                    value = if (account.expiresAt <= 0) "Mãi mãi" else format.format(Date(account.expiresAt))
                )
                DetailRow(label = "Ngày tạo", value = format.format(Date(account.createdAt)))
                DetailRow(label = "Cập nhật lần cuối", value = format.format(Date(account.updatedAt)))
                
                if (account.note.isNotBlank()) {
                    DetailRow(label = "Ghi chú", value = account.note)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Token nội dung chính:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Access Token:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = account.accessToken,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { copy("Access Token", account.accessToken) }
                                .padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "JWT/Token Payload:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = account.token,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { copy("JWT/Token", account.token) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ĐÓNG")
                    }
                    Row {
                        FilledTonalButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REFRESH")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SỬA")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, onCopy: (() -> Unit)? = null) {
    Column(modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            if (onCopy != null) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Sao chép $label",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onCopy() }
                )
            }
        }
        Text(text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
        Divider(modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    }
}

// Statistics layout
@Composable
fun StatsSection(stats: AccountStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Bảng Thống Kê Tổng Quan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Basic cards grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatsGridCard(
                title = "Tổng Số Guest",
                value = stats.totalAccounts.toString(),
                icon = Icons.Default.AccountCircle,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatsGridCard(
                title = "Tạo Hôm Nay",
                value = stats.createdToday.toString(),
                icon = Icons.Default.Today,
                accentColor = Color(0xFF17A2B8),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Expirations stats layout
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Trạng thái Hoạt động Token", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Active meter
                StatsProgressBar(title = "Đang hoạt động (Còn hạn)", count = stats.tokenActive, total = stats.totalAccounts, color = Color(0xFF28A745))
                Spacer(modifier = Modifier.height(8.dp))
                
                // Near expiration meter
                StatsProgressBar(title = "Sắp hết hạn", count = stats.tokenNearExp, total = stats.totalAccounts, color = Color(0xFFFFC107))
                Spacer(modifier = Modifier.height(8.dp))
                
                // Expired meter
                StatsProgressBar(title = "Đã hết hạn", count = stats.tokenExpired, total = stats.totalAccounts, color = Color(0xFFDC3545))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Region breakdowns
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Thống kê theo Quốc gia / Khu vực", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (stats.regionStats.isEmpty()) {
                    Text(
                        "Không có dữ liệu, hãy thêm tài khoản",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    stats.regionStats.forEach { (region, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(region, fontSize = 13.sp)
                            }
                            Text(
                                "$count accounts (${String.format("%.1f", if (stats.totalAccounts > 0) (count.toFloat() / stats.totalAccounts * 100) else 0f)}%)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Level distribution
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Phân bố cấp độ nhân vật (Level)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))

                if (stats.levelStats.isEmpty()) {
                    Text(
                        "Không có dữ liệu",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    stats.levelStats.forEach { (levelRange, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(levelRange, fontSize = 13.sp)
                            Text("$count accounts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsGridCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StatsProgressBar(title: String, count: Int, total: Int, color: Color) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$count (~${String.format("%.0f", fraction * 100)}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = fraction,
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

// Backup & Maintenance Interface
@Composable
fun BackupSection(
    viewModel: GuestViewModel,
    actionState: UiState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pastedJson by remember { mutableStateOf("") }

    fun copyToClipboard(txt: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Đống Backup", txt)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Mã JSON sao lưu đã được chép vào Clipboard!", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Sao Lưu & Khôi Phục Cơ Sở Dữ Liệu",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Export card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Trích xuất dữ liệu (Export JSON)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Trích xuất tất cả thông tin tài khoản hiện có trong Room Database thành định dạng mã JSON chuẩn để phục vụ việc lưu trữ, chia sẻ hoặc đồng bộ hóa.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.exportBackupJson { backupText ->
                            copyToClipboard(backupText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAO CHÉP MÃ BACKUP JSON", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Import card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Nhập mã dữ liệu (Import JSON)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Dán chuỗi dữ liệu JSON sao lưu của bạn vào ô dưới đây để phục hồi toàn bộ danh sách tài khoản.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pastedJson,
                    onValueChange = { pastedJson = it },
                    placeholder = { Text("Dán văn bản JSON của tệp sao lưu tại đây...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("import_text_input"),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    maxLines = 10
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (pastedJson.isBlank()) {
                            Toast.makeText(context, "Dữ liệu JSON trống!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.importBackupJson(pastedJson)
                            pastedJson = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth().testTag("import_backup_button")
                ) {
                    Icon(Icons.Default.RotateLeft, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PHUK HỒI TỪ CHUỖI JSON", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Auto Backup Card info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Chế độ Tự Động Sao Lưu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Hệ thống lưu ứng dụng Android tự động kích hoạt tính năng tự phục hồi Room DB của Google Cloud khi chuyển đổi thiết bị.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
