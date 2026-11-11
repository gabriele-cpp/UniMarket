package com.example.unimarket.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.data.repository.UserRepository
import com.example.unimarket.ui.util.Validators
import com.example.unimarket.ui.util.normalizePhone
import com.example.unimarket.ui.util.phoneForDisplay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.unimarket.ui.util.ErrorMessages
import com.example.unimarket.ui.util.rethrowIfCancellation
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserEntity? = null,
    /** Barang aktif = TERSEDIA + NEGOSIASI. */
    val activeListings: Int = 0,
    val soldListings: Int = 0,
    val errorMessage: String? = null
)

data class EditProfileState(
    val isVisible: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null
)

class ProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    productRepository: ProductRepository
) : ViewModel() {

    /** Hitungan langsung dari database: (aktif, terjual). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val listingCountsFlow = sessionRepository.currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(0 to 0)
        else combine(
            productRepository.countBySellerAndStatus(id, ProductStatus.TERSEDIA),
            productRepository.countBySellerAndStatus(id, ProductStatus.NEGOSIASI),
            productRepository.countBySellerAndStatus(id, ProductStatus.TERJUAL)
        ) { available, negotiating, sold -> (available + negotiating) to sold }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        sessionRepository.currentUser,
        listingCountsFlow
    ) { user, (active, sold) ->
        ProfileUiState(user = user, activeListings = active, soldListings = sold)
    }.catch { e ->
        ErrorMessages.log(e)
        emit(ProfileUiState(errorMessage = ErrorMessages.LOAD_FAILED))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _editState = MutableStateFlow(EditProfileState())
    val editState: StateFlow<EditProfileState> = _editState.asStateFlow()

    fun onEditClick() {
        val user = uiState.value.user ?: return
        _editState.value = EditProfileState(
            isVisible = true,
            name = user.name,
            phone = phoneForDisplay(user.phone)
        )
    }

    fun onEditNameChange(value: String) {
        _editState.update { it.copy(name = value, nameError = null) }
    }

    fun onEditPhoneChange(value: String) {
        _editState.update { it.copy(phone = value.filter { c -> c.isDigit() }.take(14), phoneError = null) }
    }

    fun onEditDismiss() {
        _editState.value = EditProfileState()
    }

    fun onEditSave() {
        val state = _editState.value
        if (state.isSaving) return
        val userId = sessionRepository.currentUserId.value ?: return

        val nameError = Validators.name(state.name)
        val phoneError = Validators.phone(state.phone)
        if (nameError != null || phoneError != null) {
            _editState.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }

        _editState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                userRepository.updateProfile(userId, state.name, normalizePhone(state.phone))
                _editState.value = EditProfileState()
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                ErrorMessages.log(e)
                _editState.update { it.copy(isSaving = false, saveError = ErrorMessages.SAVE_FAILED) }
            }
        }
    }

    fun logout() {
        sessionRepository.logout()
    }
}
