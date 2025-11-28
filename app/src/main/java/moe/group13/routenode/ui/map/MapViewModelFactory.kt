package moe.group13.routenode.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import moe.group13.routenode.data.repository.RouteRepository

//idea: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
class MapViewModelFactory(private val repository: RouteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}