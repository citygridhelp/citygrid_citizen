package com.example.potholereport.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlaceLabelsTest {

    @Test
    fun placeName_preferredOverLeadingHouseNumberInAddressLine() {
        val fields = PlaceAddressFields(
            featureName = "Klay School",
            subLocality = "Harlur",
            locality = "Bengaluru",
            addressLines = listOf("21, Some Road, Harlur, Bengaluru, Karnataka 560102, India"),
        )
        val label = TripPlaceLabels.formatSearchResult(fields, queryHint = "Klay school harlur")
        assertTrue(label.startsWith("Klay School"))
        assertTrue(label.contains("Harlur"))
        assertFalse(label.startsWith("21"))
    }

    @Test
    fun houseNumberFeature_isNotUsedAsTitle() {
        val fields = PlaceAddressFields(
            featureName = "1701",
            thoroughfare = "MG Road",
            locality = "Bengaluru",
            addressLines = listOf("1701, MG Road, Bengaluru"),
        )
        val title = TripPlaceLabels.pickPlaceTitle(fields, queryHint = "MG Road")
        assertFalse(title == "1701")
        assertTrue(title.contains("MG Road") || title.contains("Bengaluru"))
    }

    @Test
    fun queryTokens_preferMatchingInstitutionName() {
        val fields = PlaceAddressFields(
            featureName = "12th Main",
            premises = "Oakridge International School",
            subLocality = "Bellandur",
            locality = "Bengaluru",
            addressLines = listOf(
                "Oakridge International School, 12th Main, Bellandur, Bengaluru",
            ),
        )
        val title = TripPlaceLabels.pickPlaceTitle(
            fields,
            queryHint = "oakridge school bellandur",
        )
        assertEquals("Oakridge International School", title)
    }

    @Test
    fun isPlaceNameCandidate_rejectsPlotAndBuildingStyle() {
        assertFalse(TripPlaceLabels.isPlaceNameCandidate("Plot 4"))
        assertFalse(TripPlaceLabels.isPlaceNameCandidate("1701"))
        assertFalse(TripPlaceLabels.isPlaceNameCandidate("Building 41"))
        assertFalse(TripPlaceLabels.isPlaceNameCandidate("Bldg 12"))
        assertFalse(TripPlaceLabels.isPlaceNameCandidate("Tower 2"))
        assertFalse(TripPlaceLabels.isPlaceNameCandidate("Block A"))
        assertTrue(TripPlaceLabels.isPlaceNameCandidate("Klay Preschool"))
        assertTrue(TripPlaceLabels.isPlaceNameCandidate("Bren Imperia"))
    }

    @Test
    fun buildingNumberFeature_fallsBackToUserQuery() {
        val fields = PlaceAddressFields(
            featureName = "Building 41",
            subLocality = "Harlur",
            locality = "Bengaluru",
            addressLines = listOf("Building 41, Harlur, Bengaluru, Karnataka, India"),
        )
        val title = TripPlaceLabels.pickPlaceTitle(fields, queryHint = "Klay harlur")
        assertTrue(title.startsWith("Klay"))
        assertFalse(title.contains("Building", ignoreCase = true))
        val label = TripPlaceLabels.formatSearchResult(fields, queryHint = "Klay harlur")
        assertTrue(label.contains("Bengaluru") || label.contains("Harlur"))
    }

    @Test
    fun societyQuery_preferredOverBareAreaName() {
        val fields = PlaceAddressFields(
            featureName = "12",
            subLocality = "Harlur",
            locality = "Bengaluru",
            addressLines = listOf("12, Harlur Main Road, Harlur, Bengaluru"),
        )
        val title = TripPlaceLabels.pickPlaceTitle(fields, queryHint = "Bren Imperia")
        assertEquals("Bren Imperia", title)
        val label = TripPlaceLabels.formatSearchResult(fields, queryHint = "Bren Imperia")
        assertTrue(label.startsWith("Bren Imperia"))
        assertTrue(label.contains("Harlur") || label.contains("Bengaluru"))
    }

    @Test
    fun titleFromQuery_titleCasesPlaceWords() {
        assertEquals("Bren Imperia", TripPlaceLabels.titleFromQuery("bren imperia"))
        assertEquals("Klay Harlur", TripPlaceLabels.titleFromQuery("klay harlur"))
    }

    @Test
    fun bangaloreDivision_neverShownInTripSearchSubtitle() {
        val fields = PlaceAddressFields(
            featureName = "Bren Imperia",
            subLocality = "Harlur",
            locality = "Bengaluru",
            subAdminArea = "Bangalore Division",
            addressLines = listOf("Bren Imperia, Harlur, Bengaluru, Bangalore Division, Karnataka, India"),
        )
        val label = TripPlaceLabels.formatSearchResult(fields, queryHint = "bren imperia")
        assertTrue(label.startsWith("Bren Imperia"))
        assertFalse(label.contains("Division", ignoreCase = true))
        assertTrue(label.contains("Harlur"))
        assertTrue(label.contains("Bengaluru"))
    }

    @Test
    fun bangaloreDivision_alone_becomesBengaluru() {
        val fields = PlaceAddressFields(
            featureName = "Bangalore Division",
            subAdminArea = "Bangalore Division",
            locality = null,
            addressLines = listOf("Bangalore Division, Karnataka, India"),
        )
        val label = TripPlaceLabels.formatSearchResult(fields)
        assertFalse(label.contains("Division", ignoreCase = true))
        assertTrue(label.contains("Bengaluru"))
        assertEquals("Bengaluru", TripPlaceLabels.formatCitySearchLabel(fields))
    }

    @Test
    fun bangalore_locality_canonicalizesToBengaluru() {
        val fields = PlaceAddressFields(
            thoroughfare = "MG Road",
            locality = "Bangalore",
            subAdminArea = "Bangalore Division",
        )
        val label = TripPlaceLabels.formatSearchResult(fields, queryHint = "MG Road")
        assertTrue(label.contains("MG Road"))
        assertTrue(label.contains("Bengaluru"))
        assertFalse(label.contains("Division", ignoreCase = true))
        assertFalse(label.contains("Bangalore"))
    }

    @Test
    fun urbanNest_notTreatedAsAdminUnit() {
        assertFalse(TripPlaceLabels.isAdministrativeUnitName("Urban Nest"))
        assertTrue(TripPlaceLabels.isPlaceNameCandidate("Urban Nest"))
    }

    @Test
    fun isAdministrativeUnitName_detectsCommonNoise() {
        assertTrue(TripPlaceLabels.isAdministrativeUnitName("Bangalore Division"))
        assertTrue(TripPlaceLabels.isAdministrativeUnitName("Bengaluru Urban"))
        assertTrue(TripPlaceLabels.isAdministrativeUnitName("Bengaluru Urban District"))
        assertFalse(TripPlaceLabels.isAdministrativeUnitName("Koramangala"))
        assertFalse(TripPlaceLabels.isAdministrativeUnitName("HSR Layout"))
    }
}
