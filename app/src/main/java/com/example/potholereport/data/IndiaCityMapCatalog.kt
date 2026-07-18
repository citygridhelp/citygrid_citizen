package com.example.potholereport.data

import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import kotlin.math.cos

/**
 * Map centers and metro bounding boxes for cities in the citizen app picker.
 * Major metros use tuned boxes; others use a default span around the city center.
 */
object IndiaCityMapCatalog {

    /** Explicit metro boxes (north, east, south, west). */
    private val metroBounds: Map<String, BoundingBox> = mapOf(
        // Official GBA outer boundary bbox (Sept 2025); polygon in bengaluru_gba_boundary.json
        "BENGALURU" to BoundingBox(13.14266, 77.784361, 12.833625, 77.460051),
        "MUMBAI" to BoundingBox(19.28, 73.05, 18.86, 72.76),
        "DELHI" to BoundingBox(28.88, 77.45, 28.38, 76.84),
        "CHENNAI" to BoundingBox(13.24, 80.33, 12.90, 80.08),
        "HYDERABAD" to BoundingBox(17.58, 78.65, 17.22, 78.22),
        "KOLKATA" to BoundingBox(22.65, 88.55, 22.40, 88.22),
        "PUNE" to BoundingBox(18.64, 74.00, 18.40, 73.70),
        "AHMEDABAD" to BoundingBox(23.17, 72.72, 22.94, 72.44),
        "JAIPUR" to BoundingBox(27.00, 75.92, 26.74, 75.64),
        "LUCKNOW" to BoundingBox(27.00, 81.05, 26.74, 80.86),
        "SURAT" to BoundingBox(21.28, 72.92, 21.08, 72.72),
        "KANPUR" to BoundingBox(26.52, 80.42, 26.36, 80.24),
        "NAGPUR" to BoundingBox(21.22, 79.18, 21.02, 78.98),
        "INDORE" to BoundingBox(22.78, 75.94, 22.62, 75.76),
        "BHOPAL" to BoundingBox(23.32, 77.52, 23.16, 77.32),
        "PATNA" to BoundingBox(25.68, 85.22, 25.52, 85.02),
        "VISAKHAPATNAM" to BoundingBox(17.78, 83.36, 17.62, 83.18),
        "VADODARA" to BoundingBox(22.36, 73.24, 22.22, 73.08),
        "COIMBATORE" to BoundingBox(11.08, 77.02, 10.92, 76.86),
        "CHANDIGARH" to BoundingBox(30.78, 76.82, 30.68, 76.68),
        "KOCHI" to BoundingBox(10.08, 76.34, 9.92, 76.18),
        "MYSURU" to BoundingBox(12.36, 76.72, 12.22, 76.56),
        "GUWAHATI" to BoundingBox(26.20, 91.78, 26.06, 91.62),
        "THIRUVANANTHAPURAM" to BoundingBox(8.52, 77.00, 8.42, 76.86),
    )

    /** Approximate centers for every city in the in-app picker list. */
    private val centers: Map<String, GeoPoint> = mapOf(
        "BENGALURU" to GeoPoint(12.9716, 77.5946),
        "MUMBAI" to GeoPoint(19.0760, 72.8777),
        "DELHI" to GeoPoint(28.6139, 77.2090),
        "CHENNAI" to GeoPoint(13.0827, 80.2707),
        "HYDERABAD" to GeoPoint(17.3850, 78.4867),
        "KOLKATA" to GeoPoint(22.5726, 88.3639),
        "PUNE" to GeoPoint(18.5204, 73.8567),
        "AHMEDABAD" to GeoPoint(23.0225, 72.5714),
        "JAIPUR" to GeoPoint(26.9124, 75.7873),
        "LUCKNOW" to GeoPoint(26.8467, 80.9462),
        "SURAT" to GeoPoint(21.1702, 72.8311),
        "KANPUR" to GeoPoint(26.4499, 80.3319),
        "NAGPUR" to GeoPoint(21.1458, 79.0882),
        "INDORE" to GeoPoint(22.7196, 75.8577),
        "BHOPAL" to GeoPoint(23.2599, 77.4126),
        "PATNA" to GeoPoint(25.5941, 85.1376),
        "VISAKHAPATNAM" to GeoPoint(17.6868, 83.2185),
        "VADODARA" to GeoPoint(22.3072, 73.1812),
        "LUDHIANA" to GeoPoint(30.9010, 75.8573),
        "AGRA" to GeoPoint(27.1767, 78.0081),
        "NASHIK" to GeoPoint(19.9975, 73.7898),
        "FARIDABAD" to GeoPoint(28.4089, 77.3178),
        "MEERUT" to GeoPoint(28.9845, 77.7064),
        "RAJKOT" to GeoPoint(22.3039, 70.8022),
        "VARANASI" to GeoPoint(25.3176, 82.9739),
        "SRINAGAR" to GeoPoint(34.0837, 74.7973),
        "AURANGABAD" to GeoPoint(19.8762, 75.3433),
        "DHANBAD" to GeoPoint(23.7957, 86.4304),
        "AMRITSAR" to GeoPoint(31.6340, 74.8723),
        "ALLAHABAD" to GeoPoint(25.4358, 81.8463),
        "RANCHI" to GeoPoint(23.3441, 85.3096),
        "HOWRAH" to GeoPoint(22.5958, 88.2636),
        "COIMBATORE" to GeoPoint(11.0168, 76.9558),
        "JABALPUR" to GeoPoint(23.1815, 79.9864),
        "GWALIOR" to GeoPoint(26.2183, 78.1828),
        "VIJAYAWADA" to GeoPoint(16.5062, 80.6480),
        "JODHPUR" to GeoPoint(26.2389, 73.0243),
        "MADURAI" to GeoPoint(9.9252, 78.1198),
        "RAIPUR" to GeoPoint(21.2514, 81.6296),
        "KOTA" to GeoPoint(25.2138, 75.8648),
        "GUWAHATI" to GeoPoint(26.1445, 91.7362),
        "CHANDIGARH" to GeoPoint(30.7333, 76.7794),
        "THIRUVANANTHAPURAM" to GeoPoint(8.5241, 76.9366),
        "MYSURU" to GeoPoint(12.2958, 76.6394),
        "MANGALURU" to GeoPoint(12.9141, 74.8560),
        "HUBBALLI" to GeoPoint(15.3647, 75.1240),
        "BELAGAVI" to GeoPoint(15.8497, 74.4977),
        "SHIVAMOGGA" to GeoPoint(13.9299, 75.5681),
        "KALABURAGI" to GeoPoint(17.3297, 76.8343),
        "UJJAIN" to GeoPoint(23.1765, 75.7885),
        "DEHRADUN" to GeoPoint(30.3165, 78.0322),
        "NOIDA" to GeoPoint(28.5355, 77.3910),
        "GURUGRAM" to GeoPoint(28.4595, 77.0266),
        "PUDUCHERRY" to GeoPoint(11.9416, 79.8083),
        "TIRUCHIRAPPALLI" to GeoPoint(10.7905, 78.7047),
        "SALEM" to GeoPoint(11.6643, 78.1460),
        "ERODE" to GeoPoint(11.3410, 77.7172),
        "TIRUPPUR" to GeoPoint(11.1085, 77.3411),
        "KANNUR" to GeoPoint(11.8745, 75.3704),
        "KOCHI" to GeoPoint(9.9312, 76.2673),
        "KOTTAYAM" to GeoPoint(9.5916, 76.5222),
        "KOZHIKODE" to GeoPoint(11.2588, 75.7804),
        "ALAPPUZHA" to GeoPoint(9.4981, 76.3388),
        "BHUBANESWAR" to GeoPoint(20.2961, 85.8245),
        "CUTTACK" to GeoPoint(20.4625, 85.8828),
        "SILIGURI" to GeoPoint(26.7271, 88.3953),
        "ASANSOL" to GeoPoint(23.6739, 86.9524),
        "JAMNAGAR" to GeoPoint(22.4707, 70.0577),
        "JUNAGADH" to GeoPoint(21.5222, 70.4579),
        "BHAVNAGAR" to GeoPoint(21.7645, 72.1519),
        "ANAND" to GeoPoint(22.5645, 72.9289),
        "VAPI" to GeoPoint(20.3893, 72.9106),
        "UDAIPUR" to GeoPoint(24.5854, 73.7125),
        "AJMER" to GeoPoint(26.4499, 74.6399),
        "BIKANER" to GeoPoint(28.0229, 73.3119),
        "SIKAR" to GeoPoint(27.6094, 75.1399),
        "JHANSI" to GeoPoint(25.4484, 78.5685),
        "ALIGARH" to GeoPoint(27.8974, 78.0880),
        "MORADABAD" to GeoPoint(28.8386, 78.7733),
        "GHAZIABAD" to GeoPoint(28.6692, 77.4538),
        "BAREILLY" to GeoPoint(28.3670, 79.4304),
        "GORAKHPUR" to GeoPoint(26.7606, 83.3732),
        "AMRAVATI" to GeoPoint(20.9374, 77.7796),
        "SOLAPUR" to GeoPoint(17.6599, 75.9064),
        "KOLHAPUR" to GeoPoint(16.7050, 74.2433),
        "NANDED" to GeoPoint(19.1383, 77.3210),
        "AKOLA" to GeoPoint(20.7002, 77.0082),
        "SANGALI" to GeoPoint(16.8524, 74.5815),
        "WARANGAL" to GeoPoint(17.9784, 79.5941),
        "NELLORE" to GeoPoint(14.4426, 79.9865),
        "GUNTUR" to GeoPoint(16.3067, 80.4365),
        "KAKINADA" to GeoPoint(16.9891, 82.2475),
    )

    fun centerFor(cityKey: String): GeoPoint? = centers[CityMetroKeys.canonical(cityKey)]

    fun allCityKeys(): Set<String> = centers.keys

    fun metroBoundsFor(cityKey: String): BoundingBox? {
        val key = CityMetroKeys.canonical(cityKey)
        metroBounds[key]?.let { return it }
        val center = centers[key] ?: return null
        return defaultBoundsAround(center)
    }

    fun defaultBoundsAround(center: GeoPoint, latHalfSpan: Double = 0.18): BoundingBox {
        val lonHalfSpan = latHalfSpan / cos(Math.toRadians(center.latitude)).coerceIn(0.14, 0.38)
        return BoundingBox(
            center.latitude + latHalfSpan,
            center.longitude + lonHalfSpan,
            center.latitude - latHalfSpan,
            center.longitude - lonHalfSpan,
        )
    }
}
