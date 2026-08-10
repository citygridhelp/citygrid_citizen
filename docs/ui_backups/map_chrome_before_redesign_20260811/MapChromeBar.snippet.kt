// SNAPSHOT: map chrome bar (Severity / Trip / Heat / Weather) from HomeScreen.kt
// Lines ~2106–2345 as of backup 2026-08-11 — restore by replacing the matching block in HomeScreen.kt


    val mapSeverityFilterLabel = mapSeverityFilter?.title ?: "ALL"
    val gpsAccuracyText = remember(gpsPinLocation?.accuracy, gpsPinLocation?.time) {
        val loc = gpsPinLocation
        if (loc != null && loc.hasAccuracy()) {
            "GPS +/-${loc.accuracy.toInt().coerceAtLeast(1)}m"
        } else {
            "GPS searching..."
        }
    }
    if (!mapFullscreen) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CitizenSpace.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                "Severity",
                style = CitizenType.Label,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.width(CitizenSpace.Sm))
            Box {
                AssistChip(
                    onClick = { severityMenuExpanded = true },
                    modifier = Modifier.height(CitizenIcon.CapsuleCompact),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mapSeverityFilterLabel,
                                style = CitizenType.Chip,
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(CitizenIcon.Chip),
                                tint = if (mapSeverityFilter != null) {
                                    CitizenSelection.Active
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                            )
                        }
                    },
                    shape = CitizenRadius.Capsule,
                    border = BorderStroke(
                        CitizenBorder.Thin,
                        if (mapSeverityFilter != null) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                    ),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (mapSeverityFilter != null) {
                            CitizenSelection.ActiveSoft
                        } else {
                            Color.Transparent
                        },
                        labelColor = if (mapSeverityFilter != null) {
                            CitizenSelection.Active
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                    ),
                )
                DropdownMenu(
                    expanded = severityMenuExpanded,
                    onDismissRequest = { severityMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("ALL") },
                        onClick = {
                            mapSeverityFilterKey = null
                            severityMenuExpanded = false
                        },
                    )
                    PotholeSeverity.entries.forEach { severity ->
                        DropdownMenuItem(
                            text = { Text(severity.title) },
                            onClick = {
                                mapSeverityFilterKey = severity.name
                                severityMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
        FilterChip(
            // Red only while an active Navigate session exists (dest set + Navigate pressed).
            selected = tripNavActive,
            onClick = {
                when {
                    // Resume minimized turn-by-turn (chip is red).
                    tripNavActive -> onMapFullscreenChange(true)
                    // Open planner; chip stays unselected on home until Navigate.
                    else -> {
                        tripModeEnabled = true
                        onMapFullscreenChange(true)
                    }
                }
            },
            label = {
                Text("Trip", style = CitizenType.Chip)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(CitizenIcon.Chip),
                )
            },
            shape = CitizenRadius.Capsule,
            border = BorderStroke(
                CitizenBorder.Thin,
                if (tripNavActive) CitizenSelection.Active else CitizenSelection.InactiveBorder,
            ),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = MaterialTheme.colorScheme.onBackground,
                iconColor = MaterialTheme.colorScheme.onBackground,
                selectedContainerColor = CitizenSelection.Active,
                selectedLabelColor = CitizenSelection.ActiveContent,
                selectedLeadingIconColor = CitizenSelection.ActiveContent,
            ),
            modifier = Modifier
                .height(CitizenIcon.CapsuleCompact)
                .padding(end = CitizenSpace.Sm),
        )
        if (heatAvailable) {
            FilterChip(
                selected = areaHeatEnabled,
                modifier = Modifier
                    .height(CitizenIcon.CapsuleCompact)
                    .padding(end = CitizenSpace.Sm),
                onClick = {
                    areaHeatEnabled = !areaHeatEnabled
                    if (areaHeatEnabled) weatherMapEnabled = false
                },
                label = {
                    Text(
                        "Heat",
                        style = CitizenType.Chip,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = null,
                        modifier = Modifier.size(CitizenIcon.Chip),
                    )
                },
                shape = CitizenRadius.Capsule,
                border = BorderStroke(
                    CitizenBorder.Thin,
                    if (areaHeatEnabled) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    iconColor = MaterialTheme.colorScheme.onBackground,
                    selectedContainerColor = CitizenSelection.Active,
                    selectedLabelColor = CitizenSelection.ActiveContent,
                    selectedLeadingIconColor = CitizenSelection.ActiveContent,
                ),
            )
        }
        if (weatherMapAvailable) {
            FilterChip(
                selected = weatherMapEnabled,
                modifier = Modifier
                    .height(CitizenIcon.CapsuleCompact)
                    .padding(end = CitizenSpace.Sm),
                onClick = {
                    weatherMapEnabled = !weatherMapEnabled
                    if (weatherMapEnabled) areaHeatEnabled = false
                },
                label = {
                    Text(
                        "Weather",
                        style = CitizenType.Chip,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(CitizenIcon.Chip),
                    )
                },
                shape = CitizenRadius.Capsule,
                border = BorderStroke(
                    CitizenBorder.Thin,
                    if (weatherMapEnabled) CitizenSelection.Active else CitizenSelection.InactiveBorder,
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    iconColor = MaterialTheme.colorScheme.onBackground,
                    selectedContainerColor = CitizenSelection.Active,
                    selectedLabelColor = CitizenSelection.ActiveContent,
                    selectedLeadingIconColor = CitizenSelection.ActiveContent,
                ),
            )
            if (weatherMapEnabled) {
                WeatherHorizon.entries.forEach { horizon ->
                    FilterChip(
                        selected = weatherHorizon == horizon,
                        modifier = Modifier
                            .height(CitizenIcon.CapsuleCompact)
                            .padding(end = CitizenSpace.Sm),
                        onClick = { weatherHorizonName = horizon.name },
                        label = {
                            Text(
                                horizon.chipLabel,
                                style = CitizenType.Chip,
                            )
                        },
                        shape = CitizenRadius.Capsule,
                        border = BorderStroke(
                            CitizenBorder.Thin,
                            if (weatherHorizon == horizon) {
                                CitizenSelection.Active
                            } else {
                                CitizenSelection.InactiveBorder
                            },
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onBackground,
                            selectedContainerColor = CitizenSelection.Active,
                            selectedLabelColor = CitizenSelection.ActiveContent,
                        ),
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(CitizenSpace.Sm))
    }
