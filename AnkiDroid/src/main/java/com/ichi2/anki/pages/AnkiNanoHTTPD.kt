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

import android.content.Context
import android.content.SharedPreferences
import com.ichi2.libanki.Collection
import com.ichi2.libanki.stats.getGraphPreferencesRaw
import com.ichi2.libanki.stats.graphsRaw
import fi.iki.elonen.NanoHTTPD
import timber.log.Timber
import java.io.ByteArrayInputStream

class AnkiNanoHTTPD : NanoHTTPD {
    private var context: Context
    var sharedPreferences: SharedPreferences? = null
    var col: Collection? = null

    constructor(port: Int, context: Context) : super(port) {
        this.context = context
    }

    constructor(hostname: String?, port: Int, context: Context, col: Collection) : super(hostname, port) {
        this.context = context
        this.col = col
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        val mime = when (uri.substringAfterLast(".")) {
            "ico" -> "image/x-icon"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "html" -> "text/html"
            else -> "application/binary"
        }

        if (method == Method.GET) {
            return newChunkedResponse(
                Response.Status.OK,
                mime,
                this.javaClass.classLoader!!.getResourceAsStream("web$uri")
            )
        }

        if (method == Method.POST) {
            when (uri) {
                "/_anki/i18nResources" -> {
                    val contentLength = session.headers["content-length"]!!.toInt()
                    val bytes = ByteArray(contentLength)
                    session.inputStream.read(bytes, 0, contentLength)
                    Timber.d("RequestBody: " + String(bytes))

                    return newChunkedResponse(
                        Response.Status.OK,
                        mime,
                        ByteArrayInputStream(col?.newBackend?.i18nResourcesRaw(bytes))
                    )
                }
                "/_anki/getGraphPreferences" -> {
                    return newChunkedResponse(
                        Response.Status.OK,
                        mime,
                        ByteArrayInputStream(col?.newBackend?.getGraphPreferencesRaw())
                    )
                }
                "/_anki/graphs" -> {
                    val contentLength = session.headers["content-length"]!!.toInt()
                    val bytes = ByteArray(contentLength)
                    session.inputStream.read(bytes, 0, contentLength)
                    Timber.d("RequestBody: " + String(bytes))

                    return newChunkedResponse(
                        Response.Status.OK,
                        mime,
                        ByteArrayInputStream(col?.newBackend?.graphsRaw(bytes))
                    )
                }
            }
        }

        return newFixedLengthResponse("")
    }
}
