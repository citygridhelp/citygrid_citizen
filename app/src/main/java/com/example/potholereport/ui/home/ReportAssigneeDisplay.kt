package com.example.potholereport.ui.home

import com.example.potholereport.data.PersistedPotholeReport

/** Citizen-facing labels for municipal routing on report summaries. */
internal object ReportAssigneeDisplay {

    const val ROUTING_DISCLAIMER =
        "Routed by GPS to the nearest published municipal unit for citizen reference only. " +
            "Not ward-level. City Grid is not a government submission portal."

    const val WARD_ROUTING_DISCLAIMER =
        "Routed by GPS to the published GBA corporation and ward for citizen reference only. " +
            "City Grid is not a government submission portal."

    fun routingDisclaimer(report: PersistedPotholeReport): String =
        if (report.hasWard()) WARD_ROUTING_DISCLAIMER else ROUTING_DISCLAIMER

    fun wardLabel(report: PersistedPotholeReport): String? =
        if (report.hasWard()) "Ward ${report.wardNumber} — ${report.wardName}" else null

    fun usesGbaCorporationRouting(report: PersistedPotholeReport): Boolean =
        report.assigneeKey.contains("GBA_", ignoreCase = true) ||
            report.assigneeCorporation.contains("Greater Bengaluru Authority", ignoreCase = true)

    /** Second line under authority — corporation name (GBA) or classic zone name (BBMP). */
    fun municipalUnitLabel(report: PersistedPotholeReport): String =
        if (usesGbaCorporationRouting(report)) "Corporation" else "Zone"

    fun authorityLabel(): String = "Authority"
}
