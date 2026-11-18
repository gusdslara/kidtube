package com.kidstv.app.data.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageSnapshotTest {

    @Test
    fun calculatesRemainingAllowedBytesRespectingSafetyReserveAndQuota() {
        val mb = 1024L * 1024L
        val snapshot = StorageSnapshot(
            totalDeviceBytes = 32_000L * mb,
            freeDeviceBytes = 1_500L * mb,
            appVideosBytes = 1_000L * mb,
            quotaBytes = 4_000L * mb,
            safetyReserveBytes = 500L * mb
        )
        // Quota remaining = 3000 MB, device free minus safety = 1000 MB -> min is 1000 MB
        assertEquals(1_000L * mb, snapshot.remainingAllowedBytes)
        assertEquals(0.25f, snapshot.usedRatioOfQuota, 0.001f)
    }

    @Test
    fun capsAllowedBytesByQuotaWhenDeviceHasPlentyOfSpace() {
        val mb = 1024L * 1024L
        val snapshot = StorageSnapshot(
            totalDeviceBytes = 64_000L * mb,
            freeDeviceBytes = 30_000L * mb,
            appVideosBytes = 1_500L * mb,
            quotaBytes = 2_000L * mb,
            safetyReserveBytes = 512L * mb
        )
        assertEquals(500L * mb, snapshot.remainingAllowedBytes)
        assertEquals(0.75f, snapshot.usedRatioOfQuota, 0.001f)
    }
}
