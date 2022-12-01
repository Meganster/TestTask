package com.appsfactory.testtask.ui.album.favorite

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.appsfactory.testtask.R
import com.appsfactory.testtask.data.repository.LocalAlbumsRepository
import com.appsfactory.testtask.domain.album.favorite.FavoriteAlbumsDataSource
import com.appsfactory.testtask.domain.model.DetailsAlbum
import com.appsfactory.testtask.ui.album.favorite.NavigationState.OpenDetails
import com.appsfactory.testtask.ui.album.favorite.NavigationState.OpenSearch
import com.appsfactory.testtask.ui.base.compose.BaseComposeViewModel
import com.appsfactory.testtask.utils.Effect
import com.appsfactory.testtask.utils.Event
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import javax.inject.Inject

sealed interface NavigationState {
    object OpenSearch : NavigationState
    class OpenDetails(val details: DetailsAlbum) : NavigationState
}

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteAlbumsViewModel @Inject constructor(
    private val localAlbumsRepository: LocalAlbumsRepository
) : BaseComposeViewModel() {

    private val defaultExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Error when loading FavoriteAlbumsViewModel")
        showSnackbar(R.string.something_went_wrong)
    }
    private val defaultScope = viewModelScope + defaultExceptionHandler

    private val updateFavoriteAlbumsLiveData = MutableLiveData<Event<Unit>>()

    val favoriteDetailsAlbum = updateFavoriteAlbumsLiveData
        .asFlow()
        .flatMapLatest {
            Pager(PagingConfig(DEFAULT_PAGE_SIZE)) {
                FavoriteAlbumsDataSource(localAlbumsRepository, DEFAULT_PAGE_SIZE)
            }.flow
        }
        .cachedIn(defaultScope)

    val navigation = Effect<NavigationState>()

    fun onSearchButtonClicked() {
        navigation.set(OpenSearch)
    }

    fun onItemClicked(details: DetailsAlbum) {
        navigation.set(OpenDetails(details))
    }

    fun onItemLongClicked(details: DetailsAlbum) {
        showSnackbar(R.string.top_albums_start_processing, details.name)

        defaultScope.launch {
            localAlbumsRepository.deleteDetailsAlbum(details.name)
            refreshItems()
            showSnackbar(R.string.top_albums_delete_completed, details.name)
        }
    }

    fun refreshItems() {
        updateFavoriteAlbumsLiveData.value = Event(Unit)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 10
    }
}