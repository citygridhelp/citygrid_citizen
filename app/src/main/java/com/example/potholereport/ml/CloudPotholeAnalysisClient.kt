package com.example.potholereport.ml

import android.content.Context
import android.net.Uri

/**
 * Future hook for offloading pothole analysis to a cloud service.
 *
 * The on-device AI is currently disabled in this prototype build — the app
 * uses heuristics for photo validation and risk estimation. When a cloud
 * pipeline is available, register an implementation via
 * [CloudPotholeAnalysisClient.install] and the risk analyzer will prefer
 * cloud results over local heuristics automatically.
 */
interface CloudPotholeAnalysisClient {

    /**
     * Run a remote analysis on the given close-up photo. Implementations are
     * suspending so they can do network IO without blocking the caller.
     * Return `null` to fall back to local heuristics.
     */
    suspend fun analyze(context: Context, closeUpUri: Uri): PotholeRiskInsight?

    companion object {
        @Volatile
        private var registered: CloudPotholeAnalysisClient? = null

        fun install(client: CloudPotholeAnalysisClient?) {
            registered = client
        }

        fun current(): CloudPotholeAnalysisClient? = registered
    }
}
