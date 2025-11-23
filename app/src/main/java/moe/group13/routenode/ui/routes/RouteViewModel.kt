package moe.group13.routenode.ui.routes

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.group13.routenode.data.model.Route
import moe.group13.routenode.data.repository.RouteRepository

class RouteViewModel(
    private val repo: RouteRepository = RouteRepository()
) : ViewModel() {

    val publicRoutes = MutableLiveData<List<Route>>()
    val userRoutes = MutableLiveData<List<Route>>()
    val favorites = MutableLiveData<List<Route>>()
    val isLoading = MutableLiveData<Boolean>(false)
    val errorMessage = MutableLiveData<String?>()

    fun loadPublicRoutes() = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            publicRoutes.value = repo.getPublicRoutes()
        } catch (e: Exception) {
            errorMessage.value = "Failed to load public routes: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun loadUserRoutes() = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            userRoutes.value = repo.getUserRoutes()
        } catch (e: Exception) {
            errorMessage.value = "Failed to load user routes: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun loadFavorites() = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            favorites.value = repo.getFavorites()
        } catch (e: Exception) {
            errorMessage.value = "Failed to load favorites: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun saveRoute(route: Route, isPublic: Boolean = true) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            if (isPublic) {
                repo.saveRoute(route) { success, id ->
                    if (success) {
                        loadPublicRoutes()
                    } else {
                        errorMessage.postValue("Failed to save route")
                    }
                    isLoading.postValue(false)
                }
            } else {
                val id = repo.saveUserRoute(route)
                if (id != null) {
                    loadUserRoutes()
                } else {
                    errorMessage.value = "Failed to save route"
                }
                isLoading.value = false
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to save route: ${e.message}"
            isLoading.value = false
        }
    }

    fun saveFavorite(route: Route) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.saveFavorite(route)
            if (success) {
                loadFavorites()
            } else {
                errorMessage.value = "Failed to add favorite"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to add favorite: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun removeFavorite(routeId: String) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.removeFavorite(routeId)
            if (success) {
                loadFavorites()
            } else {
                errorMessage.value = "Failed to remove favorite"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to remove favorite: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun toggleFavorite(route: Route) = viewModelScope.launch {
        try {
            val isFav = repo.isFavorite(route.id)
            if (isFav) {
                removeFavorite(route.id)
            } else {
                saveFavorite(route)
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to toggle favorite: ${e.message}"
        }
    }

    fun updateRoute(routeId: String, route: Route) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.updateRoute(routeId, route)
            if (success) {
                loadPublicRoutes()
                loadUserRoutes()
            } else {
                errorMessage.value = "Failed to update route"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to update route: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun deleteRoute(routeId: String) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.deleteRoute(routeId)
            if (success) {
                loadPublicRoutes()
                loadUserRoutes()
            } else {
                errorMessage.value = "Failed to delete route"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to delete route: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    fun isFavorite(routeId: String, callback: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            val isFav = repo.isFavorite(routeId)
            callback(isFav)
        } catch (e: Exception) {
            callback(false)
        }
    }
    fun deleteFavorite(route: Route) = viewModelScope.launch {
        try {
            isLoading.value = true
            errorMessage.value = null
            val success = repo.removeFavorite(route.id)
            if (success) {
                loadFavorites()
            } else {
                errorMessage.value = "Failed to remove favorite"
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to remove favorite: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }
}