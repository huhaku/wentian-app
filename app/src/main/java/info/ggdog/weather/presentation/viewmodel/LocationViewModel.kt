package info.ggdog.weather.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.ggdog.weather.domain.model.Location
import info.ggdog.weather.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val results: List<Location>) : SearchState()
    data class Error(val message: String) : SearchState()
}

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    init {
        loadLocations()
    }

    private fun loadLocations() {
        viewModelScope.launch {
            _locations.value = locationRepository.getLocations()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            _searchState.value = SearchState.Loading
            viewModelScope.launch {
                try {
                    val results = locationRepository.searchCity(query)
                    _searchState.value = SearchState.Success(results)
                } catch (e: Exception) {
                    _searchState.value = SearchState.Error(e.message ?: "搜索失败")
                }
            }
        } else {
            _searchState.value = SearchState.Idle
        }
    }

    fun selectLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.saveLocation(location)
            locationRepository.setDefaultLocation(location.id)
            loadLocations()
        }
    }
}