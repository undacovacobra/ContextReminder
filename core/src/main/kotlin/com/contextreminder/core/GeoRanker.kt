package com.contextreminder.core

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


data class GeoCandidate<T>(
    val value: T,
    val latitude: Double,
    val longitude: Double
)

data class RankedGeoCandidate<T>(
    val value: T,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double
)

object GeoRanker {
    fun <T> nearestFirst(
        originLatitude: Double,
        originLongitude: Double,
        candidates: List<GeoCandidate<T>>
    ): List<RankedGeoCandidate<T>> = candidates
        .map { candidate ->
            RankedGeoCandidate(
                value = candidate.value,
                latitude = candidate.latitude,
                longitude = candidate.longitude,
                distanceMeters = distanceMeters(
                    originLatitude,
                    originLongitude,
                    candidate.latitude,
                    candidate.longitude
                )
            )
        }
        .sortedBy { it.distanceMeters }

    fun distanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLat = Math.toRadians(latitude2 - latitude1)
        val deltaLon = Math.toRadians(longitude2 - longitude1)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }
}
