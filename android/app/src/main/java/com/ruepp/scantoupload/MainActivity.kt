package com.ruepp.scantoupload

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ruepp.scantoupload.data.preferences.ServerConfig
import com.ruepp.scantoupload.ui.navigation.AppNavigation
import com.ruepp.scantoupload.ui.theme.ScanToUploadTheme

class MainActivity : ComponentActivity() {

    private lateinit var serverConfig: ServerConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        serverConfig = ServerConfig(this)

        val sharedUris = extractSharedUris(intent)

        setContent {
            ScanToUploadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        serverConfig = serverConfig,
                        sharedUris = sharedUris
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun extractSharedUris(intent: Intent): List<Uri> {
        if (intent.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
                return listOf(uri)
            }
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (uris != null) {
                for (uri in uris) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) { }
                }
                return uris
            }
        }
        return emptyList()
    }
}
