/***************************************************************************************
 * Copyright (c) 2022 Ankitects Pty Ltd <http://apps.ankiweb.net>                       *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki.pages

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.R
import timber.log.Timber
import java.io.IOException
import java.net.ServerSocket

class AnkiPagesWebview : AnkiActivity() {
    private lateinit var ankiServer: AnkiNanoHTTPD
    private lateinit var webview: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.anki_pages_webview)
        webview = findViewById(R.id.anki_wb)

        val port = startServer()

        webview.settings.javaScriptEnabled = true
        webview.settings.allowFileAccess = true
        webview.webChromeClient = WebChromeClient()
        webview.loadUrl("http://127.0.0.1:$port/import-csv.html")
    }

    // start server
    private fun startServer(): Int {
        val port = getAvailablePort()
        ankiServer = AnkiNanoHTTPD("127.0.0.1", port, applicationContext, col)
        ankiServer.start()
        Timber.i("Running server on 127.0.0.1$port")
        return port
    }

    @Throws(IOException::class)
    private fun getAvailablePort(): Int {
        ServerSocket(0).use { socket -> return socket.localPort }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ankiServer.isAlive) {
            ankiServer.stop()
        }
    }
}
