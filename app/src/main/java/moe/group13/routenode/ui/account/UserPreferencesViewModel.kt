package moe.group13.routenode.ui.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.group13.routenode.data.model.PreferredPlace
import moe.group13.routenode.data.model.UserPreferences
import moe.group13.routenode.data.repository.UserPreferencesRepository

class UserPreferencesViewModel(
    private val repo: UserPreferencesRepository = UserPreferencesRepository()
) : ViewModel() {

    val preferences = MutableLiveData<UserPreferences?>()
    val preferredPlaces = MutableLiveData<List<PreferredPlace>>()
    val isLoading = MutableLiveData<Boolean>(false)
    val errorMessage = MutableLiveData<String?>()

    fun loadPreferences() = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val prefs = repo.getPreferences()
            preferences.value = prefs
            preferredPlaces.value = prefs?.preferredPlaces ?: emptyList()
        } catch (e: Exception) {
            errorMessage.value = "Failed to load preferences: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun savePreferences(prefs: UserPreferences) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.savePreferences(prefs)
            if (success) {
                loadPreferences()
            } else {
                errorMessage.value = "Failed to save preferences"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to save preferences: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun updatePreferences(
        favoriteCuisines: List<String>? = null,
        travelMode: String? = null,
        preferredDistanceKm: Double? = null,
        unitPreference: String? = null,
        theme: String? = null,
        notificationsEnabled: Boolean? = null,
        routeUpdatesEnabled: Boolean? = null
    ) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.updatePreferences(
                favoriteCuisines,
                travelMode,
                preferredDistanceKm,
                unitPreference,
                theme,
                notificationsEnabled,
                routeUpdatesEnabled
            )
            if (success) {
                loadPreferences()
            } else {
                errorMessage.value = "Failed to update preferences"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to update preferences: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun addPreferredPlace(place: PreferredPlace) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.addPreferredPlace(place)
            if (success) {
                loadPreferences()
            } else {
                errorMessage.value = "Failed to add preferred place"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to add preferred place: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun removePreferredPlace(placeName: String) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.removePreferredPlace(placeName)
            if (success) {
                loadPreferences()
            } else {
                errorMessage.value = "Failed to remove preferred place"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to remove preferred place: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun updatePreferredPlace(placeName: String, updatedPlace: PreferredPlace) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.updatePreferredPlace(placeName, updatedPlace)
            if (success) {
                loadPreferences()
            } else {
                errorMessage.value = "Failed to update preferred place"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to update preferred place: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }
}

