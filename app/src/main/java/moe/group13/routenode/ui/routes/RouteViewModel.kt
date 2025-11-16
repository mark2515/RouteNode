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

    // Replace demoUid with Firebase Auth
    private val demoUid = "testUser1"
    fun loadPublicRoutes() = viewModelScope.launch {
        publicRoutes.value = repo.getPublicRoutes()
    }

    fun loadUserRoutes() = viewModelScope.launch {
        userRoutes.value = repo.getUserRoutes(demoUid)
    }

    fun loadFavorites() = viewModelScope.launch {
        favorites.value = repo.getFavorites(demoUid)
    }

    fun saveRoute(route: Route) = viewModelScope.launch {
        repo.saveRoute(route, callback = { success, id ->
            if (success) {
                loadUserRoutes()
            }
        })
    }

    fun saveFavorite(route: Route) = viewModelScope.launch {
        repo.saveFavorite(demoUid, route)
    }
}