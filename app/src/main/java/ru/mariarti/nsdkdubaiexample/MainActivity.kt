package ru.mariarti.nsdkdubaiexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import ru.dgis.sdk.ApiKeys
import ru.dgis.sdk.Context
import ru.dgis.sdk.DGis
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.map.*
import ru.dgis.sdk.update.*

class MainActivity : AppCompatActivity() {

    private val closeables = mutableListOf<AutoCloseable>()

    private val dgisContext by lazy { initializeDGis() }
    private val updateManager by lazy { getPackageManager(dgisContext) }
    private val territoryManager by lazy { getTerritoryManager(dgisContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateManager.checkForUpdates()

        closeables.add(territoryManager.territoriesChannel.connect { territories ->
            val uaeTerritory = territories.find { it.info.name == "UAE" }
            check(uaeTerritory != null) { "UAE territory not found!" }

            if (uaeTerritory.info.compatible.not()) {

                val progressText = findViewById<TextView>(R.id.progressTv)!!
                findViewById<ProgressBar>(R.id.progressBar).apply {
                    progressText.visibility = VISIBLE
                    visibility = VISIBLE
                    closeables.add(uaeTerritory.progressChannel.connect {
                        progress = it.toInt()
                        Log.d("UAE_DOWNLOAD", "progress: $it")

                        progressText.text = "$it %"

                        if (it.toInt() == 100) {
                            Log.d("UAE_DOWNLOAD", "Downloaded")
                            postDelayed(
                                {
                                    visibility = GONE
                                    progressText.visibility = GONE
                                }, 2000 )
                        }
                    })
                }
                uaeTerritory.install()
            }
        })

        val mapOptions = MapOptions().apply {
            source = DgisSource.createOfflineDgisSource(dgisContext)
            position = CameraPosition(
                point = GeoPoint(25.1866, 55.2464),
                zoom = Zoom(13.0f)
            )
        }

        val mapView = MapView(this, mapOptions)
        findViewById<LinearLayout>(R.id.mapHolder).apply {
            addView(mapView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeables.forEach(AutoCloseable::close)
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