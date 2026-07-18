package com.example.potholereport.data

/**
 * Bengaluru GBA routing at report submit: ward polygon → corporation → public officer snapshot.
 * Does not affect the citizen home map.
 */
object BengaluruMunicipalRouting {

    data class Result(
        val assignee: MunicipalAssignee,
        val ward: GbaWardMatch?,
    )

    fun resolve(cityKey: String, latitude: Double, longitude: Double): Result {
        val metro = CityMetroKeys.canonical(cityKey)
        if (metro != "BENGALURU" || !latitude.isFinite() || !longitude.isFinite()) {
            val assignee = MunicipalAssignmentResolver.resolve(cityKey, latitude, longitude)
            return Result(assignee = assignee, ward = null)
        }

        val ward = if (BengaluruGbaWards.isInitialized()) {
            BengaluruGbaWards.findWard(latitude, longitude)
        } else {
            null
        }

        val assignee = if (ward != null) {
            MunicipalContactsRegistry.assigneeForKey(ward.corporationKey)
                ?: MunicipalAssignmentResolver.resolve(cityKey, latitude, longitude)
        } else {
            MunicipalAssignmentResolver.resolve(cityKey, latitude, longitude)
        }

        return Result(assignee = assignee, ward = ward)
    }

    fun formatSubmitRoutingMessage(report: PersistedPotholeReport): String {
        if (report.hasWard()) {
            val corp = report.assigneeZone.ifBlank { report.assigneeCorporation }
            return "Report saved. Routed to $corp, Ward ${report.wardNumber} — ${report.wardName}."
        }
        val label = report.assigneeZone.ifBlank { report.assigneeCorporation }
        return "Report saved. Routed to $label."
    }

    fun formatSubmitRoutingMessage(ward: GbaWardMatch?, assignee: MunicipalAssignee): String {
        return if (ward != null) {
            "Report saved. Routed to ${ward.corporationLabel}, Ward ${ward.wardNumber} — ${ward.wardName}."
        } else {
            "Report saved. Routed to ${assignee.zoneLabel.ifBlank { assignee.corporationName }}."
        }
    }
}
