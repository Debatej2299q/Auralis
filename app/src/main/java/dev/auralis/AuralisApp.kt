package dev.auralis

import android.app.Application
import dev.auralis.core.AppPreferences
import dev.auralis.core.NetworkMonitor
import dev.auralis.data.MusicDatabase
import dev.auralis.data.MusicRepository
import dev.auralis.online.DownloadManager
import dev.auralis.online.YtEngine
import dev.auralis.player.PlayerController

class AuralisApp : Application() {

    lateinit var repository: MusicRepository
        private set
    lateinit var preferences: AppPreferences
        private set
    lateinit var network: NetworkMonitor
        private set
    lateinit var player: PlayerController
        private set
    lateinit var ytEngine: YtEngine
        private set
    lateinit var downloads: DownloadManager
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this, MusicDatabase.build(this))
        preferences = AppPreferences(this)
        network = NetworkMonitor(this)
        player = PlayerController.get(this)
        ytEngine = YtEngine(this)
        downloads = DownloadManager(this, repository, ytEngine, network)
    }
}
