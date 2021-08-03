package ru.mariarti.nsdkdubaiexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import ru.dgis.sdk.ApiKeys
import ru.dgis.sdk.Context
import ru.dgis.sdk.DGis
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.map.*
import ru.dgis.sdk.update.*

class MainActivity : AppCompatActivity() {

    private val dgisContext by lazy { initializeDGis() }
    private val updateManager by lazy { getPackageManager(dgisContext) }
    private val territoryManager by lazy { getTerritoryManager(dgisContext) }

    private lateinit var territoriesConnection: AutoCloseable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateManager.checkForUpdates()

        territoriesConnection = territoryManager.territoriesChannel.connect { territories ->
            val uaeTerritory = territories.find { it.info.name == "UAE" }
            check(uaeTerritory != null) { "UAE territory not found!" }

            if (uaeTerritory.info.compatible.not()) {
                uaeTerritory.progressChannel.connect {
                    Log.d("UAE_DOWNLOAD", "progress: $it")
                }
                uaeTerritory.install()
            }
        }

        val mapOptions = MapOptions().apply {
            source = DgisSource.createOfflineDgisSource(dgisContext)
            position = CameraPosition(
                point = GeoPoint(25.1866, 55.2464),
                zoom = Zoom(13.0f)
            )
        }

        val mapView = MapView(this, mapOptions)
        setContentView(mapView)
    }

    override fun onDestroy() {
        super.onDestroy()
        territoriesConnection.close()
    }

    private fun initializeDGis() : Context
    {
        val key = { id: Int -> String
            applicationContext.resources.getString(id)
        }

        return DGis.initialize(applicationContext, ApiKeys(
            directory = key(R.string.dgis_directory_api_key),
            map = key(R.string.dgis_map_api_key)
        ))
    }
}