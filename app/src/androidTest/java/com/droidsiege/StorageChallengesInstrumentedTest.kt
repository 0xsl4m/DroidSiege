package com.droidsiege

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.droidsiege.challenges.storage.ExtStorageVault
import com.droidsiege.challenges.storage.PrefsVault
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** M9: the insecure default leaves readable artifacts; secureMode must not. */
@RunWith(AndroidJUnit4::class)
class StorageChallengesInstrumentedTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun prefsPlaintextInVulnerableModeOnly() {
        PrefsVault.saveSessionPlain(context, secureMode = false)
        val raw = context
            .getSharedPreferences("siege_wallet_prefs", Context.MODE_PRIVATE)
            .all
            .toString()
        assertThat(raw).contains("DS{storage_prefs_L1_3f9a2c}")

        PrefsVault.saveSessionPlain(context, secureMode = true)
        val hardened = context
            .getSharedPreferences("siege_wallet_prefs", Context.MODE_PRIVATE)
            .all
            .toString()
        assertThat(hardened).doesNotContain("DS{storage_prefs_L1_3f9a2c}")
    }

    @Test
    fun encodedPrefsStillRecoverableVulnerably() {
        PrefsVault.saveSessionEncoded(context, secureMode = false)
        val stored = context
            .getSharedPreferences("siege_wallet_prefs", Context.MODE_PRIVATE)
            .getString("encrypted_flag", null)
        val decoded = android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
        assertThat(String(decoded)).isEqualTo("DS{storage_prefs_L2_71c4e8}")
    }

    @Test
    fun extStorageReceiptContainsFlagVulnerably() {
        ExtStorageVault.exportReceipt(context, secureMode = false)
        val receipt = File(context.getExternalFilesDir(null)!!, "receipt_export.txt").readText()
        assertThat(receipt).contains("DS{storage_extstorage_L1_6c2e85}")

        ExtStorageVault.exportReceipt(context, secureMode = true)
        val hardened = File(context.getExternalFilesDir(null)!!, "receipt_export.txt").readText()
        assertThat(hardened).doesNotContain("DS{storage_extstorage_L1_6c2e85}")
    }
}
