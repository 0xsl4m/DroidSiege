package com.droidsiege

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.droidsiege.challenges.components.ExportMessengerService
import com.droidsiege.challenges.components.MSG_GET_RECOVERY
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** M8: the manifest components are real, exported, and leak the documented flags. */
@RunWith(AndroidJUnit4::class)
class ComponentsInstrumentedTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun exportedComponentsAreExportedInTheManifest() {
        val pm = context.packageManager
        assertThat(
            pm.getActivityInfo(
                ComponentName(context, "com.droidsiege.challenges.components.ExportFlagActivity"),
                0,
            ).exported,
        ).isTrue()
        assertThat(
            pm.getServiceInfo(
                ComponentName(context, "com.droidsiege.challenges.components.ExportMessengerService"),
                0,
            ).exported,
        ).isTrue()
        assertThat(
            pm.getProviderInfo(
                ComponentName(context, "com.droidsiege.challenges.components.SiegeVaultProvider"),
                0,
            ).exported,
        ).isTrue()
    }

    @Test
    fun providerServesTheRecoveryRowInVulnerableMode() {
        context.contentResolver.query(
            Uri.parse("content://com.droidsiege.vault/secrets"),
            null,
            null,
            null,
            null,
        )!!.use { cursor ->
            val values = buildString {
                while (cursor.moveToNext()) append(cursor.getString(1))
            }
            assertThat(values).contains("DS{components_provider_L1_3f86d1}")
        }
    }

    @Test
    fun providerLookupIsInjectableInVulnerableMode() {
        val uri = Uri.parse("content://com.droidsiege.vault/lookup?filter=x' OR '1'='1")
        context.contentResolver.query(uri, null, null, null, null)!!.use { cursor ->
            val values = buildString {
                while (cursor.moveToNext()) append(cursor.getString(1))
            }
            assertThat(values).contains("DS{components_provider_L3_e07b52}")
        }
    }

    @Test
    fun messengerServiceAnswersTheRecoveryMessage() {
        val bound = CountDownLatch(1)
        var messenger: Messenger? = null

        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(
                    name: ComponentName,
                    binder: IBinder,
                ) {
                    messenger = Messenger(binder)
                    bound.countDown()
                }

                override fun onServiceDisconnected(name: ComponentName) = Unit
            }
        context.bindService(
            Intent(context, ExportMessengerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        assertThat(bound.await(5, TimeUnit.SECONDS)).isTrue()

        val replied = CountDownLatch(1)
        var reply: String? = null
        val replyMessenger =
            Messenger(
                android.os.Handler(context.mainLooper) { msg ->
                    reply = msg.data.getString("recovery")
                    replied.countDown()
                    true
                },
            )
        val message = Message.obtain(null, MSG_GET_RECOVERY)
        message.replyTo = replyMessenger
        messenger!!.send(message)

        assertThat(replied.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(reply).contains("DS{components_exported_L3_94ba30}")
        context.unbindService(connection)
    }
}
