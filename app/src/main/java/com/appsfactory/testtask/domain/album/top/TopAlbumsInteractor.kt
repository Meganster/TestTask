package com.appsfactory.testtask.domain.album.top

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.appsfactory.testtask.data.repository.LocalAlbumsRepository
import com.appsfactory.testtask.data.repository.RemoteAlbumsRepository
import com.appsfactory.testtask.data.repository.ResourceCachingProvider
import com.appsfactory.testtask.domain.model.Album
import com.appsfactory.testtask.domain.model.Artist
import com.appsfactory.testtask.domain.model.DetailsAlbum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TopAlbumsInteractor @Inject constructor(
    private val remoteAlbumsRepository: RemoteAlbumsRepository,
    private val localAlbumsRepository: LocalAlbumsRepository
) {

    private var currentArtist: Artist? = null
    private var currentAlbum: Album? = null

    private val cache = ResourceCachingProvider<DetailsAlbum>(
        databaseInserter = { detailsAlbum ->
            flow {
                localAlbumsRepository.saveArtist(currentArtist!!)
                localAlbumsRepository.saveDetailsAlbum(detailsAlbum)

                emit(detailsAlbum)
            }
        },
        databaseGetter = {
            flow {
                val details = localAlbumsRepository.tryGetDetailsAlbum(currentAlbum!!)
                emit(details)
            }
        },
        remoteDataProvider = {
            flow {
                val details = remoteAlbumsRepository.getDetailsAlbum(currentArtist!!, currentAlbum!!)
                emit(details)
            }
        }
    )

    fun topAlbumsArtist(artist: Artist, pageSize: Int): Flow<PagingData<Album>> {
        if (currentArtist != artist) {
            cache.invalidateCachedData()
        }

        return Pager(PagingConfig(pageSize)) {
            TopAlbumsDataSource(
                remoteAlbumsRepository = remoteAlbumsRepository,
                artist = artist
            )
        }.flow
    }

    fun loadDetailsAlbum(artist: Artist, album: Album): Flow<DetailsAlbum> {
        if (currentAlbum != album || currentArtist != artist) {
            cache.invalidateCachedData()
        }
        currentArtist = artist
        currentAlbum = album
        return cache.getCurrentValue(false)
    }

    suspend fun isDetailsAlbumSavedLocally(album: Album): Boolean {
        return localAlbumsRepository.tryGetDetailsAlbum(album) != null
    }

    suspend fun saveArtistAndDetailsAlbum(artist: Artist, album: Album): DetailsAlbum {
        val detailsAlbum = remoteAlbumsRepository.getDetailsAlbum(artist, album)

        localAlbumsRepository.saveArtist(artist)
        localAlbumsRepository.saveDetailsAlbum(detailsAlbum)

        return detailsAlbum
    }

    suspend fun deleteDetailsAlbum(album: Album) {
        localAlbumsRepository.deleteDetailsAlbum(album.name)
    }
}