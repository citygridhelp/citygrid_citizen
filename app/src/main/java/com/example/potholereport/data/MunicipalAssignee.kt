package com.example.potholereport.data

/**
 * Municipal officer assigned to a pothole report for public accountability.
 * Officer details are sourced from official corporation directories (see [MunicipalContactsRegistry]).
 */
data class MunicipalAssignee(
    val key: String,
    val cityKey: String,
    val corporationName: String,
    val zoneLabel: String,
    val officerName: String,
    val officerPosition: String,
    val officeAddress: String,
) {
    fun toPersistedFields(): PersistedAssigneeFields = PersistedAssigneeFields(
        assigneeKey = key,
        assigneeCorporation = corporationName,
        assigneeZone = zoneLabel,
        assigneeName = officerName,
        assigneePosition = officerPosition,
        assigneeOfficeAddress = officeAddress,
    )
}

data class PersistedAssigneeFields(
    val assigneeKey: String,
    val assigneeCorporation: String,
    val assigneeZone: String,
    val assigneeName: String,
    val assigneePosition: String,
    val assigneeOfficeAddress: String,
)

internal data class MunicipalZoneRecord(
    val key: String,
    val cityKey: String,
    val zoneLabel: String,
    val corporationName: String,
    val officerName: String,
    val officerPosition: String,
    val officeAddress: String,
    val centerLat: Double,
    val centerLng: Double,
) {
    fun toAssignee(): MunicipalAssignee = MunicipalAssignee(
        key = key,
        cityKey = cityKey,
        corporationName = corporationName,
        zoneLabel = zoneLabel,
        officerName = officerName,
        officerPosition = officerPosition,
        officeAddress = officeAddress,
    )
}
