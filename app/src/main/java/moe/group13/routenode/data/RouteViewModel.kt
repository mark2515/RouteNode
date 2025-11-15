package moe.group13.routenode.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.group13.routenode.data.RouteRepository
import moe.group13.routenode.data.Route

class RouteViewModel(
    private val repo: RouteRepository = RouteRepository()
) : ViewModel() {

    val publicRoutes = MutableLiveData<List<Route>>()
    val userRoutes = MutableLiveData<List<Route>>()
    val favorites = MutableLiveData<List<Route>>()
    val loading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String>()

    fun loadPublicRoutes() = viewModelScope.launch {
        try {
            loading.value = true
            publicRoutes.value = repo.getPublicRoutes()
        } catch (e: Exception) {
            error.value = e.message
        } finally {
            loading.value = false
        }
    }

    fun loadUserRoutes() = viewModelScope.launch {
        try {
            loading.value = true
            userRoutes.value = repo.getUserRoutes()
        } catch (e: Exception) {
            error.value = e.message
        } finally {
            loading.value = false
        }
    }

    fun loadFavorites() = viewModelScope.launch {
        try {
            loading.value = true
            favorites.value = repo.getFavorites()
        } catch (e: Exception) {
            error.value = e.message
        } finally {
            loading.value = false
        }
    }

    fun saveRoute(route: Route) = viewModelScope.launch {
        try {
            repo.saveUserRoute(route)
            loadUserRoutes()
        } catch (e: Exception) {
            error.value = e.message
        }
    }

    fun publishRoute(route: Route) = viewModelScope.launch {
        try {
            repo.publishRoute(route)
            loadPublicRoutes()
        } catch (e: Exception) {
            error.value = e.message
        }
    }
}