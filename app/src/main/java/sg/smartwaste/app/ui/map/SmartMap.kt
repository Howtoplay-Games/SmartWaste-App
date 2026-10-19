package sg.smartwaste.app.ui.map

import android.graphics.RectF
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.ui.theme.SW
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.GeoPoint

/** One-shot camera instruction. A new [nonce] re-triggers the same request. */
sealed interface CameraRequest {
    val nonce: Long

    data class Focus(val point: GeoPoint, val zoom: Double, override val nonce: Long = System.nanoTime()) : CameraRequest
    data class Fit(val points: List<GeoPoint>, override val nonce: Long = System.nanoTime()) : CameraRequest
    data class Follow(val point: GeoPoint, val bearing: Double, override val nonce: Long = System.nanoTime()) : CameraRequest
}

private const val SRC_PLACES = "sw-places"
private const val SRC_ROUTE = "sw-route"
private const val SRC_USER = "sw-user"
private const val L_ROUTE_CASING = "sw-route-casing"
private const val L_ROUTE = "sw-route-line"
private const val L_PLACES_HALO = "sw-places-halo"
private const val L_PLACES = "sw-places"
private const val L_USER_HALO = "sw-user-halo"
private const val L_USER = "sw-user"

// OpenFreeMap: free, key-less vector tiles built from OpenStreetMap.
private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/positron"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"
private const val STYLE_FALLBACK = "https://tiles.openfreemap.org/styles/liberty"

/**
 * Vector map (MapLibre + OpenFreeMap) with data-driven layers for bins and buyers,
 * a casing-styled route line and a live user location puck.
 */
@Composable
fun SmartMap(
    places: List<Place>,
    selectedId: String?,
    user: GeoPoint?,
    route: List<GeoPoint>?,
    dark: Boolean,
    camera: CameraRequest?,
    bottomPaddingPx: Int,
    onPlaceClick: (String) -> Unit,
    onMapClick: () -> Unit,
    onUserGesture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var styleFailed by remember { mutableStateOf(false) }

    val placeClick by rememberUpdatedState(onPlaceClick)
    val mapClick by rememberUpdatedState(onMapClick)
    val gesture by rememberUpdatedState(onUserGesture)

    // Forward lifecycle events to the MapView, tracking state so teardown is balanced.
    DisposableEffect(lifecycle, mapView) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> if (!resumed) { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> if (resumed) { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> if (started) { mapView.onStop(); started = false }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    DisposableEffect(mapView) {
        val failListener = MapView.OnDidFailLoadingMapListener { styleFailed = true }
        mapView.addOnDidFailLoadingMapListener(failListener)
        mapView.getMapAsync { m ->
            m.uiSettings.apply {
                isLogoEnabled = false
                isAttributionEnabled = true
                attributionGravity = Gravity.TOP or Gravity.START
                isCompassEnabled = true
                compassGravity = Gravity.TOP or Gravity.END
                isTiltGesturesEnabled = true
                isRotateGesturesEnabled = true
            }
            m.cameraPosition = CameraPosition.Builder()
                .target(LatLng(Geo.SINGAPORE.lat, Geo.SINGAPORE.lng))
                .zoom(11.0)
                .build()
            m.addOnMapClickListener { latLng ->
                val screen = m.projection.toScreenLocation(latLng)
                val r = 28f
                val hits = m.queryRenderedFeatures(RectF(screen.x - r, screen.y - r, screen.x + r, screen.y + r), L_PLACES)
                val id = hits.firstOrNull()?.getStringProperty("id")
                if (id != null) placeClick(id) else mapClick()
                true
            }
            m.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) gesture()
            }
            map = m
        }
        onDispose { mapView.removeOnDidFailLoadingMapListener(failListener) }
    }

    // (Re)load the basemap style when the theme changes; layers are re-added each time.
    LaunchedEffect(map, dark, styleFailed) {
        val m = map ?: return@LaunchedEffect
        style = null
        val url = when {
            styleFailed -> STYLE_FALLBACK
            dark -> STYLE_DARK
            else -> STYLE_LIGHT
        }
        m.setStyle(Style.Builder().fromUri(url)) { s ->
            addLayers(s)
            style = s
        }
    }

    // Data updates.
    LaunchedEffect(style, places, selectedId) {
        val s = style ?: return@LaunchedEffect
        val features = places.map { p ->
            Feature.fromGeometry(Point.fromLngLat(p.point.lng, p.point.lat)).apply {
                addStringProperty("id", p.id)
                addStringProperty(
                    "kind", when {
                        p.isSmartBin -> "smart"
                        p.type == PlaceType.BUYER -> "buyer"
                        else -> "bin"
                    }
                )
                addBooleanProperty("selected", p.id == selectedId)
                addBooleanProperty("full", p.isFull)
            }
        }
        s.getSourceAs<GeoJsonSource>(SRC_PLACES)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(style, route) {
        val s = style ?: return@LaunchedEffect
        val fc = if (route != null && route.size >= 2) {
            FeatureCollection.fromFeatures(
                listOf(Feature.fromGeometry(LineString.fromLngLats(route.map { Point.fromLngLat(it.lng, it.lat) })))
            )
        } else FeatureCollection.fromFeatures(emptyList<Feature>())
        s.getSourceAs<GeoJsonSource>(SRC_ROUTE)?.setGeoJson(fc)
    }

    LaunchedEffect(style, user) {
        val s = style ?: return@LaunchedEffect
        val fc = if (user != null) {
            FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(user.lng, user.lat))))
        } else FeatureCollection.fromFeatures(emptyList<Feature>())
        s.getSourceAs<GeoJsonSource>(SRC_USER)?.setGeoJson(fc)
    }

    // Camera requests.
    LaunchedEffect(map, camera, bottomPaddingPx) {
        val m = map ?: return@LaunchedEffect
        val pad = with(density) { 56.dp.roundToPx() }
        when (val c = camera) {
            null -> Unit
            is CameraRequest.Focus -> m.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(c.point.lat, c.point.lng)).zoom(c.zoom).tilt(0.0).bearing(0.0).build()
                ), 700
            )
            is CameraRequest.Fit -> {
                if (c.points.size >= 2) {
                    val bounds = LatLngBounds.Builder().includes(c.points.map { LatLng(it.lat, it.lng) }).build()
                    m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, pad, pad * 3, pad, bottomPaddingPx + pad), 800)
                } else if (c.points.size == 1) {
                    m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(c.points[0].lat, c.points[0].lng), 15.0), 700)
                }
            }
            is CameraRequest.Follow -> m.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(c.point.lat, c.point.lng))
                        .zoom(17.2)
                        .tilt(50.0)
                        .bearing(c.bearing)
                        .build()
                ), 900
            )
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun addLayers(s: Style) {
    s.addSource(GeoJsonSource(SRC_ROUTE))
    s.addSource(GeoJsonSource(SRC_PLACES))
    s.addSource(GeoJsonSource(SRC_USER))

    s.addLayer(
        LineLayer(L_ROUTE_CASING, SRC_ROUTE).withProperties(
            PropertyFactory.lineColor(SW.Bg.toArgb()),
            PropertyFactory.lineWidth(11f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    s.addLayer(
        LineLayer(L_ROUTE, SRC_ROUTE).withProperties(
            PropertyFactory.lineColor(SW.Lime.toArgb()),
            PropertyFactory.lineWidth(6.5f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )

    val kindColor = Expression.match(
        Expression.get("kind"),
        Expression.color(SW.Lime.toArgb()),
        Expression.stop("buyer", Expression.color(SW.Amber.toArgb())),
        Expression.stop("smart", Expression.color(SW.Mint.toArgb())),
    )
    val isSelected = Expression.eq(Expression.get("selected"), Expression.literal(true))

    s.addLayer(
        CircleLayer(L_PLACES_HALO, SRC_PLACES).withProperties(
            PropertyFactory.circleRadius(Expression.switchCase(isSelected, Expression.literal(22f), Expression.literal(0f))),
            PropertyFactory.circleColor(kindColor),
            PropertyFactory.circleOpacity(0.25f),
        )
    )
    s.addLayer(
        CircleLayer(L_PLACES, SRC_PLACES).withProperties(
            PropertyFactory.circleRadius(Expression.switchCase(isSelected, Expression.literal(11f), Expression.literal(7.5f))),
            PropertyFactory.circleColor(kindColor),
            PropertyFactory.circleStrokeColor(SW.Bg.toArgb()),
            PropertyFactory.circleStrokeWidth(2.5f),
            PropertyFactory.circleOpacity(
                Expression.switchCase(Expression.eq(Expression.get("full"), Expression.literal(true)), Expression.literal(0.45f), Expression.literal(1f))
            ),
        )
    )
    s.addLayer(
        CircleLayer(L_USER_HALO, SRC_USER).withProperties(
            PropertyFactory.circleRadius(20f),
            PropertyFactory.circleColor(SW.Lime.toArgb()),
            PropertyFactory.circleOpacity(0.18f),
        )
    )
    s.addLayer(
        CircleLayer(L_USER, SRC_USER).withProperties(
            PropertyFactory.circleRadius(7.5f),
            PropertyFactory.circleColor(SW.Lime.toArgb()),
            PropertyFactory.circleStrokeColor(SW.Text.toArgb()),
            PropertyFactory.circleStrokeWidth(3f),
        )
    )
}
