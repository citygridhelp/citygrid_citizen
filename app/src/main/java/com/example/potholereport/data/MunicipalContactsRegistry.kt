package com.example.potholereport.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Official municipal contacts for pothole accountability routing.
 *
 * Sources (verified public directories, 2025–2026):
 * - Bengaluru: BBMP zone-wise officers (bbmp.gov.in / site.bbmp.gov.in)
 * - Mumbai: Brihanmumbai Municipal Corporation ward offices (mcgm.gov.in)
 * - Delhi: Municipal Corporation of Delhi zone directories (mcdonline.nic.in)
 * - Chennai: Greater Chennai Corporation RTI public information officers
 * - Hyderabad: GHMC Key Contacts (ghmc.gov.in)
 */
object MunicipalContactsRegistry {

    private val zones: List<MunicipalZoneRecord> = buildList {
        addAll(bbmpZones())
        addAll(bmcZones())
        addAll(mcdZones())
        addAll(gccZones())
        addAll(ghmcZones())
    }

    private val byKey: Map<String, MunicipalZoneRecord> = zones.associateBy { it.key }

    private val byCity: Map<String, List<MunicipalZoneRecord>> = zones.groupBy { it.cityKey }

    fun assigneeForKey(key: String): MunicipalAssignee? = byKey[key]?.toAssignee()

    fun nearestAssignee(cityKey: String, latitude: Double, longitude: Double): MunicipalAssignee? {
        val cityZones = byCity[cityKey] ?: return null
        return cityZones.minByOrNull { zone ->
            haversineKm(latitude, longitude, zone.centerLat, zone.centerLng)
        }?.toAssignee()
    }

    fun fallbackForCity(cityKey: String): MunicipalAssignee =
        cityFallbacks[cityKey] ?: defaultFallback

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /** BBMP — Joint Commissioners (zonal heads). */
    private fun bbmpZones(): List<MunicipalZoneRecord> {
        val corp = "Bruhat Bengaluru Mahanagara Palike (BBMP)"
        return listOf(
            zone("BENGALURU", "EAST", corp, "East Zone",
                "Smt. Saroja B B, K.A.S", "Joint Commissioner",
                "Office of the Joint Commissioner, BBMP East Zone, Mayo Hall Compound, Ashok Nagar, Bengaluru, Karnataka 560001",
                12.978, 77.640),
            zone("BENGALURU", "WEST", corp, "West Zone",
                "Sri. Sangappa", "Joint Commissioner",
                "Office of the Joint Commissioner, BBMP West Zone, Dr. Rajkumar Road, Rajajinagar, Bengaluru, Karnataka 560010",
                13.010, 77.555),
            zone("BENGALURU", "SOUTH", corp, "South Zone",
                "Shri. HR Shivakumar, K.A.S", "Joint Commissioner",
                "Office of the Joint Commissioner, BBMP South Zone, 3rd Block, Jayanagar, Bengaluru, Karnataka 560041",
                12.920, 77.585),
            zone("BENGALURU", "RR_NAGAR", corp, "Rajarajeshwari Nagar Zone",
                "Smt. Arathi Anand, K.A.S", "Joint Commissioner",
                "Office of the Joint Commissioner, BBMP RR Nagar Zone, Ideal Homes Township, RR Nagar, Bengaluru, Karnataka 560098",
                12.920, 77.520),
            zone("BENGALURU", "MAHADEVAPURA", corp, "Mahadevapura Zone",
                "Dr. Dakshayini, K.A.S", "Joint Commissioner",
                "Office of the Joint Commissioner, BBMP Mahadevapura Zone, Outer Ring Road, Marathahalli, Bengaluru, Karnataka 560037",
                12.995, 77.715),
            zone("BENGALURU", "BOMMANAHALLI", corp, "Bommanahalli Zone",
                "Shri. Ajith M, K.A.S", "Joint Commissioner",
                "Office of the Joint Commissioner, BBMP Bommanahalli Zone, Hosur Road, Bommanahalli, Bengaluru, Karnataka 560068",
                12.905, 77.625),
            zone("BENGALURU", "DASARAHALLI", corp, "Dasarahalli Zone",
                "Sri. Preetham Nasalapure", "Joint Commissioner",
                "Office of the Joint Commissioner, BBMP Dasarahalli Zone, Peenya 2nd Stage, Bengaluru, Karnataka 560091",
                13.060, 77.530),
            zone("BENGALURU", "YELAHANKA", corp, "Yelahanka Zone",
                "Sri Mohammed Naeem Momin", "Joint Commissioner",
                "Office of the Joint Commissioner, BBMP Yelahanka Zone, Yelahanka New Town, Bengaluru, Karnataka 560064",
                13.105, 77.600),
        )
    }

    /** BMC — Assistant Municipal Commissioners (ward-level; roads via Maintenance & Repair). */
    private fun bmcZones(): List<MunicipalZoneRecord> {
        val corp = "Brihanmumbai Municipal Corporation (BMC)"
        return listOf(
            zone("MUMBAI", "ISLAND_CITY", corp, "City Ward Cluster (A–E)",
                "Assistant Municipal Commissioner (Ward A – Fort)", "Assistant Municipal Commissioner",
                "BMC Ward A Office, 134 E Shahid Bhagat Singh Marg, Fort, Mumbai, Maharashtra 400001",
                18.935, 72.835),
            zone("MUMBAI", "CENTRAL", corp, "Central Ward Cluster (F–N)",
                "Assistant Municipal Commissioner (Ward F/North)", "Assistant Municipal Commissioner",
                "BMC F/North Ward Office, Plot No. 96, Bhau Daji Road, King's Circle, Matunga (East), Mumbai, Maharashtra 400019",
                19.030, 72.860),
            zone("MUMBAI", "WESTERN", corp, "Western Suburbs (K–P)",
                "Assistant Municipal Commissioner (Ward K/West)", "Assistant Municipal Commissioner",
                "BMC K/West Ward Office, Paliram Road, Near S.V. Road, Andheri (West), Mumbai, Maharashtra 400058",
                19.130, 72.835),
            zone("MUMBAI", "EASTERN", corp, "Eastern Suburbs (L–T)",
                "Assistant Municipal Commissioner (Ward N)", "Assistant Municipal Commissioner",
                "BMC Ward N Office, L.B.S. Marg, Ghatkopar (West), Mumbai, Maharashtra 400086",
                19.090, 72.910),
            zone("MUMBAI", "HARBOUR", corp, "Harbour & Dock Belt",
                "Assistant Municipal Commissioner (Ward B)", "Assistant Municipal Commissioner",
                "BMC Ward B Office, 121 Ramchandra Bhatt Marg, Babula Tank, Mumbai, Maharashtra 400009",
                18.960, 72.855),
        )
    }

    /** MCD — Executive Engineers (Maintenance) for road/pothole works. */
    private fun mcdZones(): List<MunicipalZoneRecord> {
        val corp = "Municipal Corporation of Delhi (MCD)"
        return listOf(
            zone("DELHI", "CENTRAL", corp, "Central Zone",
                "Sh. V.K. Shah", "Executive Engineer (Maintenance Division-I)",
                "MCD Central Zone Engineering Office, Dr. S.P.M. Civic Centre, Minto Road, New Delhi 110002",
                28.630, 77.220),
            zone("DELHI", "SOUTH", corp, "South Zone",
                "Sh. Deepak Gehlot", "Executive Engineer (Maintenance Division-I)",
                "MCD South Zone Office, R.K. Puram Sector-9, near Sangam Cinema, New Delhi 110022",
                28.555, 77.195),
            zone("DELHI", "WEST", corp, "West Zone",
                "Sh. Surender Dabas", "Executive Engineer (Maintenance Division-I)",
                "MCD West Zone Engineering Office, F-Block, Rajouri Garden, New Delhi 110027",
                28.645, 77.120),
            zone("DELHI", "ROHINI", corp, "Rohini Zone",
                "Executive Engineer (Maintenance Division-I)", "Executive Engineer (Maintenance Division-I)",
                "MCD Rohini Zone Office, Sector 7, Rohini, New Delhi 110085",
                28.740, 77.105),
            zone("DELHI", "SHAHDARA_NORTH", corp, "Shahdara North Zone",
                "Himanshu Shekhar", "Executive Engineer (Maintenance Division-I)",
                "MCD Shahdara North Zone Office, Shahdara, Delhi 110032",
                28.690, 77.290),
            zone("DELHI", "SHAHDARA_SOUTH", corp, "Shahdara South Zone",
                "Sh. Satish Kumar Katariya", "Superintending Engineer (Engineering)",
                "MCD Shahdara South Zone Office, 18 Block, Geeta Colony, Delhi 110031",
                28.660, 77.275),
            zone("DELHI", "NAJAFGARH", corp, "Najafgarh Zone",
                "Sh. Sumit Kumar, IOFS", "Deputy Commissioner",
                "MCD Najafgarh Zone Office, Najafgarh, New Delhi 110043",
                28.610, 77.040),
        )
    }

    /** GCC — Executive Engineers (Works) by zone. */
    private fun gccZones(): List<MunicipalZoneRecord> {
        val corp = "Greater Chennai Corporation (GCC)"
        return listOf(
            zone("CHENNAI", "ZONE_1", corp, "Zone 1 (Thiruvottiyur)",
                "Executive Engineer, Zone-1", "Executive Engineer (Works)",
                "Greater Chennai Corporation, No.947, T.H. Road, Thiruvottiyur, Chennai, Tamil Nadu 600019",
                13.160, 80.300),
            zone("CHENNAI", "ZONE_2", corp, "Zone 2 (Manali)",
                "Executive Engineer, Zone-2", "Executive Engineer (Works)",
                "Greater Chennai Corporation, No.112, Dr. Ambedkar Street, Manali, Chennai, Tamil Nadu 600068",
                13.170, 80.260),
            zone("CHENNAI", "ZONE_3", corp, "Zone 3 (Madhavaram)",
                "Executive Engineer, Zone-3", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Bazar Road, Madhavaram, Chennai, Tamil Nadu 600060",
                13.150, 80.230),
            zone("CHENNAI", "ZONE_4", corp, "Zone 4 (Tondiarpet)",
                "Executive Engineer, Zone-4", "Executive Engineer (Works)",
                "Greater Chennai Corporation, No.266, T.H. Road, Tondiarpet, Chennai, Tamil Nadu 600081",
                13.135, 80.290),
            zone("CHENNAI", "ZONE_5", corp, "Zone 5 (Royapuram)",
                "Executive Engineer, Zone-5", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 5 Office, Royapuram, Chennai, Tamil Nadu 600013",
                13.110, 80.295),
            zone("CHENNAI", "ZONE_6", corp, "Zone 6 (Thiru-Vi-Ka Nagar)",
                "Executive Engineer, Zone-6", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 6 Office, Perambur, Chennai, Tamil Nadu 600011",
                13.115, 80.240),
            zone("CHENNAI", "ZONE_7", corp, "Zone 7 (Ambattur)",
                "Executive Engineer, Zone-7", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 7 Office, Ambattur, Chennai, Tamil Nadu 600053",
                13.115, 80.160),
            zone("CHENNAI", "ZONE_8", corp, "Zone 8 (Anna Nagar)",
                "Executive Engineer, Zone-8", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 8 Office, Anna Nagar, Chennai, Tamil Nadu 600040",
                13.090, 80.210),
            zone("CHENNAI", "ZONE_9", corp, "Zone 9 (Teynampet)",
                "Executive Engineer, Zone-9", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 9 Office, Teynampet, Chennai, Tamil Nadu 600018",
                13.040, 80.250),
            zone("CHENNAI", "ZONE_10", corp, "Zone 10 (Kodambakkam)",
                "Executive Engineer, Zone-10", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 10 Office, Kodambakkam, Chennai, Tamil Nadu 600024",
                13.055, 80.220),
            zone("CHENNAI", "ZONE_11", corp, "Zone 11 (Valasaravakkam)",
                "Executive Engineer, Zone-11", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 11 Office, Valasaravakkam, Chennai, Tamil Nadu 600087",
                13.045, 80.175),
            zone("CHENNAI", "ZONE_12", corp, "Zone 12 (Alandur)",
                "Executive Engineer, Zone-12", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 12 Office, Alandur, Chennai, Tamil Nadu 600016",
                13.005, 80.205),
            zone("CHENNAI", "ZONE_13", corp, "Zone 13 (Adyar)",
                "Executive Engineer, Zone-13", "Executive Engineer (Works)",
                "Greater Chennai Corporation, No.115, Dr. Muthulakshmi Salai, Adyar, Chennai, Tamil Nadu 600020",
                13.005, 80.255),
            zone("CHENNAI", "ZONE_14", corp, "Zone 14 (Perungudi)",
                "Executive Engineer, Zone-14", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 14 Office, Perungudi, Chennai, Tamil Nadu 600096",
                12.965, 80.245),
            zone("CHENNAI", "ZONE_15", corp, "Zone 15 (Sholinganallur)",
                "Executive Engineer, Zone-15", "Executive Engineer (Works)",
                "Greater Chennai Corporation, Zone 15 Office, Sholinganallur, Chennai, Tamil Nadu 600119",
                12.925, 80.230),
        )
    }

    /** GHMC — Zonal Commissioners (ghmc.gov.in Key Contacts). */
    private fun ghmcZones(): List<MunicipalZoneRecord> {
        val corp = "Greater Hyderabad Municipal Corporation (GHMC)"
        return listOf(
            zone("HYDERABAD", "CHARMINAR", corp, "Charminar Zone",
                "Sri Venkanna", "Zonal Commissioner",
                "GHMC Charminar Zonal Office, Narqui Phool Bagh, Chandrayangutta, Hyderabad, Telangana 500005",
                17.350, 78.480),
            zone("HYDERABAD", "SECUNDERABAD", corp, "Secunderabad Zone",
                "Sri N. Ravi Kiran", "Zonal Commissioner",
                "GHMC Secunderabad Zonal Office, Secunderabad, Hyderabad, Telangana 500003",
                17.440, 78.500),
            zone("HYDERABAD", "SERILINGAMPALLY", corp, "Serilingampally Zone",
                "Sri P. Upender Reddy", "Zonal Commissioner",
                "GHMC Serilingampally Zonal Office, Serilingampally, Hyderabad, Telangana 500019",
                17.470, 78.320),
            zone("HYDERABAD", "KUKATPALLY", corp, "Kukatpally Zone",
                "Sri Apurv Chauhan, IAS", "Zonal Commissioner",
                "GHMC Kukatpally Zonal Office, Kukatpally, Hyderabad, Telangana 500072",
                17.490, 78.410),
            zone("HYDERABAD", "KHAIRATHABAD", corp, "Khairatabad Zone",
                "Sri Anuraag Jayanti, IAS", "Zonal Commissioner",
                "GHMC Khairatabad Zonal Office, Khairatabad, Hyderabad, Telangana 500004",
                17.405, 78.460),
            zone("HYDERABAD", "LB_NAGAR", corp, "L.B. Nagar Zone",
                "Sri Hemanta Keshav Patil, IAS", "Zonal Commissioner",
                "GHMC L.B. Nagar Zonal Office, L.B. Nagar, Hyderabad, Telangana 500074",
                17.350, 78.550),
        )
    }

    private fun zone(
        cityKey: String,
        zoneKey: String,
        corporationName: String,
        zoneLabel: String,
        officerName: String,
        officerPosition: String,
        officeAddress: String,
        centerLat: Double,
        centerLng: Double,
    ): MunicipalZoneRecord = MunicipalZoneRecord(
        key = "$cityKey:$zoneKey",
        cityKey = cityKey,
        zoneLabel = zoneLabel,
        corporationName = corporationName,
        officerName = officerName,
        officerPosition = officerPosition,
        officeAddress = officeAddress,
        centerLat = centerLat,
        centerLng = centerLng,
    )

    private val cityFallbacks: Map<String, MunicipalAssignee> = mapOf(
        "BENGALURU" to MunicipalAssignee(
            key = "BENGALURU:HQ",
            cityKey = "BENGALURU",
            corporationName = "Bruhat Bengaluru Mahanagara Palike (BBMP)",
            zoneLabel = "Headquarters",
            officerName = "Municipal Commissioner, BBMP",
            officerPosition = "Municipal Commissioner",
            officeAddress = "BBMP Head Office, N.R. Square, Bengaluru, Karnataka 560002",
        ),
        "MUMBAI" to MunicipalAssignee(
            key = "MUMBAI:HQ",
            cityKey = "MUMBAI",
            corporationName = "Brihanmumbai Municipal Corporation (BMC)",
            zoneLabel = "Headquarters",
            officerName = "Municipal Commissioner, BMC",
            officerPosition = "Municipal Commissioner",
            officeAddress = "BMC Head Office, Mahapalika Marg, Fort, Mumbai, Maharashtra 400001",
        ),
        "DELHI" to MunicipalAssignee(
            key = "DELHI:HQ",
            cityKey = "DELHI",
            corporationName = "Municipal Corporation of Delhi (MCD)",
            zoneLabel = "Headquarters",
            officerName = "Commissioner, MCD",
            officerPosition = "Municipal Commissioner",
            officeAddress = "Dr. S.P.M. Civic Centre, Minto Road, New Delhi 110002",
        ),
        "CHENNAI" to MunicipalAssignee(
            key = "CHENNAI:HQ",
            cityKey = "CHENNAI",
            corporationName = "Greater Chennai Corporation (GCC)",
            zoneLabel = "Headquarters",
            officerName = "Commissioner, GCC",
            officerPosition = "Municipal Commissioner",
            officeAddress = "Ripon Building, Chennai, Tamil Nadu 600003",
        ),
        "HYDERABAD" to MunicipalAssignee(
            key = "HYDERABAD:HQ",
            cityKey = "HYDERABAD",
            corporationName = "Greater Hyderabad Municipal Corporation (GHMC)",
            zoneLabel = "Headquarters",
            officerName = "Sri Ilambarithi K., IAS",
            officerPosition = "Commissioner, GHMC",
            officeAddress = "GHMC Head Office, CC Complex, Tank Bund Road, Lower Tank Bund, Hyderabad, Telangana 500063",
        ),
    )

    private val defaultFallback: MunicipalAssignee = cityFallbacks.getValue("BENGALURU")
}
