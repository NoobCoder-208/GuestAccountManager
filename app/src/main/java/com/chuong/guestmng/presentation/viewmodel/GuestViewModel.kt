package com.chuong.guestmng.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.chuong.guestmng.GuestApplication
import com.chuong.guestmng.domain.model.GuestAccount
import com.chuong.guestmng.domain.repository.GuestRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SortOption(val displayName: String) {
    UID_ASC("UID tăng dần"),
    UID_DESC("UID giảm dần"),
    NICKNAME_A_Z("Nickname A-Z"),
    NICKNAME_Z_A("Nickname Z-A"),
    LEVEL_ASC("Level tăng dần"),
    LEVEL_DESC("Level giảm dần"),
    NEWEST("Mới nhất (Ngày tạo)"),
    OLDEST("Cũ nhất (Ngày tạo)"),
    EXPIRE_CLOSEST("Hạn dùng gần nhất"),
    EXPIRE_FURTHEST("Hạn dùng xa nhất")
}

enum class TokenStatusFilter(val displayName: String) {
    ACTIVE("Token còn hạn"),
    NEAR_EXPIRATION("Token sắp hết hạn"),
    EXPIRED("Token hết hạn")
}

enum class CreatedAtFilter(val displayName: String) {
    ALL("Tất cả thời gian"),
    TODAY("Tạo hôm nay"),
    THIS_WEEK("Tạo tuần này")
}

data class AccountStats(
    val totalAccounts: Int = 0,
    val regionStats: Map<String, Int> = emptyMap(),
    val levelStats: Map<String, Int> = emptyMap(),
    val tokenActive: Int = 0,
    val tokenNearExp: Int = 0,
    val tokenExpired: Int = 0,
    val createdToday: Int = 0
)

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val error: String) : UiState
}

class GuestViewModel(private val repository: GuestRepository) : ViewModel() {

    // Central UI Toast/Snack Event stream
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Central action progress state
    private val _actionState = MutableStateFlow<UiState>(UiState.Idle)
    val actionState = _actionState.asStateFlow()

    // Query states
    val searchQuery = MutableStateFlow("")
    val selectedGroups = MutableStateFlow<Set<String>>(emptySet())
    val selectedRegions = MutableStateFlow<Set<String>>(emptySet())
    val selectedTokenStatus = MutableStateFlow<Set<TokenStatusFilter>>(emptySet())
    val selectedCreatedAtFilter = MutableStateFlow(CreatedAtFilter.ALL)
    val sortOption = MutableStateFlow(SortOption.NEWEST)

    // Pull to Refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // Multi select state
    val selectedAccountIds = MutableStateFlow<Set<Int>>(emptySet())

    // All distinct regions in the DB to dynamic options in Filter
    val availableRegions = repository.getAllAccounts()
        .map { accounts -> accounts.map { it.region }.distinct().filter { it.isNotBlank() }.sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered accounts stream
    val filteredAccounts: StateFlow<List<GuestAccount>> = combine(
        repository.getAllAccounts(),
        searchQuery,
        selectedGroups,
        selectedRegions,
        selectedTokenStatus,
        selectedCreatedAtFilter,
        sortOption
    ) { flowArray: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val accounts = flowArray[0] as List<GuestAccount>
        val query = flowArray[1] as String
        @Suppress("UNCHECKED_CAST")
        val groups = flowArray[2] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val regions = flowArray[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val statuses = flowArray[4] as Set<TokenStatusFilter>
        val dateFilter = flowArray[5] as CreatedAtFilter
        val sort = flowArray[6] as SortOption

        var list = accounts

        // 1. Searching
        if (query.isNotBlank()) {
            val q = query.lowercase().trim()
            list = list.filter {
                it.uid.lowercase().contains(q) ||
                it.nickname.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.region.lowercase().contains(q)
            }
        }

        // 2. Custom Group Filtering
        if (groups.isNotEmpty()) {
            list = list.filter { account ->
                account.groupNames.any { it in groups }
            }
        }

        // 3. Region Filtering
        if (regions.isNotEmpty()) {
            list = list.filter { it.region in regions }
        }

        // 4. Token status indicator filtering
        if (statuses.isNotEmpty()) {
            list = list.filter { account ->
                val status = when {
                    account.isExpired() -> TokenStatusFilter.EXPIRED
                    account.isNearExpired() -> TokenStatusFilter.NEAR_EXPIRATION
                    else -> TokenStatusFilter.ACTIVE
                }
                status in statuses
            }
        }

        // 5. Date Creation time filtering
        when (dateFilter) {
            CreatedAtFilter.TODAY -> {
                val startOfToday = getStartOfToday()
                list = list.filter { it.createdAt >= startOfToday }
            }
            CreatedAtFilter.THIS_WEEK -> {
                val startOfWeek = System.currentTimeMillis() - (7 * 86400000L)
                list = list.filter { it.createdAt >= startOfWeek }
            }
            CreatedAtFilter.ALL -> {}
        }

        // 6. Sorting algorithms implementation
        list = when (sort) {
            SortOption.UID_ASC -> list.sortedBy { it.uid }
            SortOption.UID_DESC -> list.sortedByDescending { it.uid }
            SortOption.NICKNAME_A_Z -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nickname })
            SortOption.NICKNAME_Z_A -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.nickname })
            SortOption.LEVEL_ASC -> list.sortedBy { it.level }
            SortOption.LEVEL_DESC -> list.sortedByDescending { it.level }
            SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> list.sortedBy { it.createdAt }
            SortOption.EXPIRE_CLOSEST -> list.sortedBy { it.expiresAt }
            SortOption.EXPIRE_FURTHEST -> list.sortedByDescending { it.expiresAt }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics stream
    val statsState: StateFlow<AccountStats> = repository.getAllAccounts().map { accounts ->
        val total = accounts.size
        
        val regionMap = accounts.groupBy { it.region }
            .mapValues { it.value.size }
            .filterKeys { it.isNotBlank() }

        val levelGroups = mapOf(
            "Level 1-10" to accounts.count { it.level in 1..10 },
            "Level 11-40" to accounts.count { it.level in 11..40 },
            "Level 41-70" to accounts.count { it.level in 41..70 },
            "Level 71-99" to accounts.count { it.level in 71..99 },
            "Level 100+" to accounts.count { it.level >= 100 }
        ).filterValues { it > 0 }

        val tokenActive = accounts.count { it.isActiveToken() && !it.isNearExpired() }
        val tokenNearExp = accounts.count { it.isNearExpired() }
        val tokenExpired = accounts.count { it.isExpired() }

        val startOfToday = getStartOfToday()
        val createdToday = accounts.count { it.createdAt >= startOfToday }

        AccountStats(
            totalAccounts = total,
            regionStats = regionMap,
            levelStats = levelGroups,
            tokenActive = tokenActive,
            tokenNearExp = tokenNearExp,
            tokenExpired = tokenExpired,
            createdToday = createdToday
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountStats())

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // --- Actions ---

    fun toggleAccountSelection(id: Int) {
        val current = selectedAccountIds.value
        selectedAccountIds.value = if (id in current) current - id else current + id
    }

    fun clearAccountSelection() {
        selectedAccountIds.value = emptySet()
    }

    fun selectAllFiltered() {
        val visibleIds = filteredAccounts.value.map { it.localId }.toSet()
        selectedAccountIds.value = visibleIds
    }

    fun addAccount(
        uidInput: String,
        passwordInput: String,
        selectedGroups: Set<String>,
        note: String
    ) {
        if (uidInput.isBlank() || passwordInput.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("Vui lòng nhập UID và mật khẩu") }
            return
        }

        viewModelScope.launch {
            _actionState.value = UiState.Loading
            repository.addAccount(uidInput, passwordInput, selectedGroups, note)
                .onSuccess { account ->
                    _actionState.value = UiState.Success("Thêm tài khoản thành công: ${account.nickname}")
                    _uiEvent.emit("Thêm tài khoản ${account.nickname} thành công!")
                }
                .onFailure { error ->
                    _actionState.value = UiState.Error(error.localizedMessage ?: "Lỗi máy chủ")
                    _uiEvent.emit("Lỗi: ${error.localizedMessage}")
                }
        }
    }

    fun deleteAccount(localId: Int) {
        viewModelScope.launch {
            repository.deleteAccount(localId)
                .onSuccess {
                    _uiEvent.emit("Đã xóa tài khoản")
                }
        }
    }

    fun deleteSelectedAccounts() {
        val ids = selectedAccountIds.value.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            repository.deleteAccounts(ids)
                .onSuccess {
                    _uiEvent.emit("Đã xóa ${ids.size} tài khoản lựa chọn")
                    clearAccountSelection()
                }
        }
    }

    fun updateAccountGroupsAndNote(account: GuestAccount, newGroups: Set<String>, newNote: String) {
        viewModelScope.launch {
            val updated = account.copy(groupNames = newGroups, note = newNote)
            repository.updateAccount(updated)
                .onSuccess {
                    _uiEvent.emit("Đã cập nhật tài khoản ${account.nickname}")
                }
        }
    }

    // Refresh actions
    fun refreshSingleAccount(id: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAccounts(listOf(id))
            _isRefreshing.value = false
            _uiEvent.emit("Đã làm mới thông tin tài khoản")
        }
    }

    fun refreshSelectedAccounts() {
        val ids = selectedAccountIds.value.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAccounts(ids)
            _isRefreshing.value = false
            _uiEvent.emit("Đã làm mới ${ids.size} tài khoản")
            clearAccountSelection()
        }
    }

    fun refreshAllAccounts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAllAccounts()
            _isRefreshing.value = false
            _uiEvent.emit("Đã làm mới tất cả tài khoản")
        }
    }

    // Export / Import backups
    fun exportBackupJson(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackup()
            onExported(json)
            _uiEvent.emit("Đã trích xuất dữ liệu lưu sao lưu!")
        }
    }

    fun importBackupJson(jsonString: String) {
        if (jsonString.isBlank()) return
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            repository.importBackup(jsonString)
                .onSuccess { count ->
                    _actionState.value = UiState.Idle
                    _uiEvent.emit("Đã phục hồi $count tài khoản thành công!")
                }
                .onFailure { error ->
                    _actionState.value = UiState.Error(error.localizedMessage ?: "Dữ liệu JSON không hợp lệ")
                    _uiEvent.emit("Lỗi: Phục hồi thất bại (JSON sai dạn)")
                }
        }
    }

    fun resetFilters() {
        selectedGroups.value = emptySet()
        selectedRegions.value = emptySet()
        selectedTokenStatus.value = emptySet()
        selectedCreatedAtFilter.value = CreatedAtFilter.ALL
        sortOption.value = SortOption.NEWEST
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as GuestApplication
                return GuestViewModel(application.repository) as T
            }
        }
    }
}
