package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OSMMap(
    busLatitude: Double,
    busLongitude: Double,
    busAngle: Float = 0f,
    busName: String = "Bus 04",
    schoolLatitude: Double = 37.7850,
    schoolLongitude: Double = -122.4100,
    schoolName: String = "Greenwood High",
    waypointsJson: String = "[]", // Json array of stops
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isMapLoaded by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val htmlContent = remember(isDarkMode) {
        val tileUrl = if (isDarkMode) {
            "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        } else {
            "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
        }
        val textCol = if (isDarkMode) "#FFFFFF" else "#000000"
        val bgCol = if (isDarkMode) "#121212" else "#F5F5F5"

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background-color: $bgCol;
                }
                /* Custom CSS pulse effect for geofence */
                .pulse-geofence {
                    background: rgba(30, 144, 255, 0.2);
                    border: 2px dashed #1E90FF;
                    border-radius: 50%;
                }
                /* Custom style for bus icon */
                .bus-marker {
                    background-color: #FF9800;
                    border: 2px solid white;
                    border-radius: 50%;
                    color: white;
                    text-align: center;
                    font-weight: bold;
                    font-size: 10px;
                    line-height: 24px;
                    box-shadow: 0 0 8px rgba(0,0,0,0.4);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map;
                var busMarker;
                var schoolMarker;
                var stopMarkers = [];
                var routePolyline;
                var geofence3KmCircle;
                var geofence1KmCircle;

                function initMap() {
                    map = L.map('map', {
                        zoomControl: false,
                        attributionControl: false
                    }).setView([$busLatitude, $busLongitude], 14);

                    L.tileLayer('$tileUrl', {
                        maxZoom: 19
                    }).addTo(map);

                    // Signal Android that map is fully initialized
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onMapLoaded();
                    }
                }

                function updateBus(lat, lng, heading, name) {
                    if (!map) return;
                    var pos = [lat, lng];
                    
                    if (busMarker) {
                        busMarker.setLatLng(pos);
                    } else {
                        var busIcon = L.divIcon({
                            className: 'bus-marker',
                            html: '🚌',
                            iconSize: [28, 28],
                            iconAnchor: [14, 14]
                        });
                        busMarker = L.marker(pos, {icon: busIcon}).addTo(map);
                        busMarker.bindPopup("<b>" + name + "</b><br>Live Telemetry GPS Active");
                    }
                    
                    // Keep map centered on active tracking
                    map.panTo(pos);
                }

                function updateSchool(lat, lng, name) {
                    if (!map) return;
                    var pos = [lat, lng];
                    if (schoolMarker) {
                        schoolMarker.setLatLng(pos);
                    } else {
                        var schoolIcon = L.divIcon({
                            html: '🏫',
                            iconSize: [32, 32],
                            iconAnchor: [16, 16],
                            className: 'school-icon-div'
                        });
                        schoolMarker = L.marker(pos, {icon: schoolIcon}).addTo(map);
                        schoolMarker.bindPopup("<b>" + name + "</b> (Base Campus)");
                    }
                    
                    // Draw geofence circles around school (3 KM and 1 KM)
                    if (geofence3KmCircle) map.removeLayer(geofence3KmCircle);
                    if (geofence1KmCircle) map.removeLayer(geofence1KmCircle);

                    geofence3KmCircle = L.circle(pos, {
                        color: '#1E90FF',
                        fillColor: '#1E90FF',
                        fillOpacity: 0.08,
                        radius: 3000,
                        dashArray: '5, 5'
                    }).addTo(map);

                    geofence1KmCircle = L.circle(pos, {
                        color: '#4CAF50',
                        fillColor: '#4CAF50',
                        fillOpacity: 0.12,
                        radius: 1000,
                        dashArray: '2, 2'
                    }).addTo(map);
                }

                function drawRoute(waypointsStr) {
                    if (!map) return;
                    try {
                        var waypoints = JSON.parse(waypointsStr);
                        var latlngs = [];
                        
                        // Clear existing stops
                        stopMarkers.forEach(m => map.removeLayer(m));
                        stopMarkers = [];

                        if (routePolyline) {
                            map.removeLayer(routePolyline);
                        }

                        waypoints.forEach(function(p, index) {
                            var pos = [p.lat, p.lng];
                            latlngs.push(pos);

                            var stopIcon = L.divIcon({
                                html: index === 0 ? '📍' : (index === waypoints.length - 1 ? '🏁' : '🛑'),
                                iconSize: [24, 24],
                                iconAnchor: [12, 12],
                                className: 'stop-icon-div'
                            });

                            var marker = L.marker(pos, {icon: stopIcon}).addTo(map);
                            marker.bindPopup("<b>" + p.name + "</b><br>Stop #" + (index + 1));
                            stopMarkers.push(marker);
                        });

                        if (latlngs.length > 1) {
                            routePolyline = L.polyline(latlngs, {
                                color: '#6200EE',
                                weight: 5,
                                opacity: 0.8,
                                lineJoin: 'round'
                            }).addTo(map);
                            
                            // Fit map viewport to encapsulate entire route
                            var group = new L.featureGroup(stopMarkers);
                            map.fitBounds(group.getBounds().pad(0.1));
                        }
                    } catch (e) {
                        // Suppress error
                    }
                }

                window.onload = initMap;
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    // Reactively update WebView when bus position or parameters change
    LaunchedEffect(isMapLoaded, busLatitude, busLongitude, busName) {
        if (isMapLoaded) {
            webViewInstance?.evaluateJavascript(
                "updateBus($busLatitude, $busLongitude, $busAngle, '$busName');",
                null
            )
        }
    }

    LaunchedEffect(isMapLoaded, schoolLatitude, schoolLongitude, schoolName) {
        if (isMapLoaded) {
            webViewInstance?.evaluateJavascript(
                "updateSchool($schoolLatitude, $schoolLongitude, '$schoolName');",
                null
            )
        }
    }

    LaunchedEffect(isMapLoaded, waypointsJson) {
        if (isMapLoaded && waypointsJson.isNotEmpty()) {
            webViewInstance?.evaluateJavascript(
                "drawRoute('$waypointsJson');",
                null
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Map is initialized inside onload of HTML
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMapLoaded() {
                            isMapLoaded = true
                        }
                    }, "AndroidBridge")

                    loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                    webViewInstance = this
                }
            },
            update = { webView ->
                webViewInstance = webView
            }
        )

        if (!isMapLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
