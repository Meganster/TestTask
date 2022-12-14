package com.appsfactory.testtask.ui.album.top

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.appsfactory.testtask.R
import com.appsfactory.testtask.domain.album.top.TopAlbumsInteractor
import com.appsfactory.testtask.domain.model.Album
import com.appsfactory.testtask.domain.model.Artist
import com.appsfactory.testtask.domain.model.DetailsAlbum
import com.appsfactory.testtask.ui.base.compose.BaseComposeViewModel
import com.appsfactory.testtask.utils.Effect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TopAlbumsViewModel @Inject constructor(
    private val topAlbumsInteractor: TopAlbumsInteractor
) : BaseComposeViewModel() {

    private val defaultExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Error when loading TopAlbumsViewModel")
        showSnackbar(R.string.something_went_wrong)
    }
    private val defaultScope = viewModelScope + defaultExceptionHandler

    private val artistStateFlow = MutableStateFlow<Artist?>(null)

    val navigation = Effect<DetailsAlbum>()

    val topAlbums = artistStateFlow
        .flatMapLatest {
            it?.let {
                topAlbumsInteractor.topAlbumsArtist(it, DEFAULT_PAGE_SIZE)
            } ?: emptyFlow()
        }
        .cachedIn(defaultScope)

    fun loadTopAlbums(artist: Artist) {
        artistStateFlow.value = artist
    }

    fun onAlbumClicked(album: Album) {
        defaultScope.launch {
            topAlbumsInteractor.loadDetailsAlbum(artistStateFlow.value!!, album).collect {
                navigation.set(it)
            }
        }
    }

    fun onAlbumLongClicked(album: Album) {
        showSnackbar(R.string.top_albums_start_processing, album.name)

        defaultScope.launch {
            if (topAlbumsInteractor.isDetailsAlbumSavedLocally(album)) {
                topAlbumsInteractor.deleteDetailsAlbum(album)
                showSnackbar(R.string.top_albums_delete_completed, album.name)
            } else {
                topAlbumsInteractor.saveArtistAndDetailsAlbum(artistStateFlow.value!!, album)
                showSnackbar(R.string.top_albums_saving_completed, album.name)
            }
        }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 10
    }
}