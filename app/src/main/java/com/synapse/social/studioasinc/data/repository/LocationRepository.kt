package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor() {

    companion object {
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 10000
        private const val MAX_RESPONSE_SIZE = 1024 * 1024
    }

    suspend fun searchLocations(query: String): Result<List<LocationData>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=20"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SynapseSocial/1.0 (com.synapse.social.studioasinc)")
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.reader().use { reader ->
                    val buffer = CharArray(1024)
                    val builder = StringBuilder()
                    var charsRead: Int
                    var totalChars = 0
                    while (reader.read(buffer).also { charsRead = it } != -1) {
                        if (totalChars + charsRead > MAX_RESPONSE_SIZE) {
                            throw Exception("Response too large")
                        }
                        builder.append(buffer, 0, charsRead)
                        totalChars += charsRead
                    }
                    builder.toString()
                }

                val jsonArray = JSONArray(responseString)
                val results = mutableListOf<LocationData>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.optString("name", obj.optString("display_name").split(",").firstOrNull() ?: "Unknown")
                    val displayName = obj.optString("display_name")
                    val lat = obj.optString("lat").toDoubleOrNull()
                    val lon = obj.optString("lon").toDoubleOrNull()


                    val address = obj.optJSONObject("address")
                    val city = address?.optString("city") ?: address?.optString("town") ?: address?.optString("village")
                    val country = address?.optString("country")
                    val shortAddress = listOfNotNull(city, country).joinToString(", ")

                    results.add(LocationData(
                        name = name,
                        address = if (shortAddress.isNotBlank()) shortAddress else displayName,
                        latitude = lat,
                        longitude = lon
                    ))
                }
                Result.success(results)
            } else {
                Result.failure(Exception("HTTP Error: $responseCode"))
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationRepository", "Location search failed", e)
            Result.failure(e)
        }
    }
}
