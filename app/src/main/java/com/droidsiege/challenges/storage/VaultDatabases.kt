package com.droidsiege.challenges.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// L1 — an ordinary notes vault.
@Entity(tableName = "notes")
data class VaultNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
)

@Dao
interface VaultNoteDao {
    @Insert
    suspend fun insert(note: VaultNote)

    @Query("SELECT * FROM notes")
    suspend fun all(): List<VaultNote>

    @Query("DELETE FROM notes")
    suspend fun clear()
}

@Database(entities = [VaultNote::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun notes(): VaultNoteDao
}

// L2 — the same data behind "ops" naming nobody should look at.
@Suppress("ConstructorParameterNaming") // cfg_blob is the challenge: obfuscated column
@Entity(tableName = "sys_kv")
data class CacheEntry(
    @PrimaryKey val c1: String,
    val cfg_blob: String,
)

@Dao
interface CacheEntryDao {
    @Insert
    suspend fun insert(entry: CacheEntry)

    @Query("SELECT * FROM sys_kv")
    suspend fun all(): List<CacheEntry>

    @Query("DELETE FROM sys_kv")
    suspend fun clear()
}

@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun entries(): CacheEntryDao
}

// L3/L4 — ciphertext-at-rest stores.
@Entity(tableName = "sealed_records")
data class SealedRecord(
    @PrimaryKey val recordId: String,
    val payload: ByteArray,
    val nonce: ByteArray,
)

@Dao
interface SealedRecordDao {
    @Insert
    suspend fun insert(record: SealedRecord)

    @Query("SELECT * FROM sealed_records WHERE recordId = :id")
    suspend fun byId(id: String): SealedRecord?

    @Query("DELETE FROM sealed_records")
    suspend fun clear()
}

@Database(entities = [SealedRecord::class], version = 1, exportSchema = false)
abstract class SealedStoreDatabase : RoomDatabase() {
    abstract fun records(): SealedRecordDao
}

/** Databases are tiny and challenge-owned; build them on demand. */
object VaultDatabases {
    fun vault(context: Context): VaultDatabase =
        Room.databaseBuilder(context, VaultDatabase::class.java, "vault.db").build()

    fun cache(context: Context): CacheDatabase =
        Room.databaseBuilder(context, CacheDatabase::class.java, "sys_cache_v3.db").build()

    fun secureStore(context: Context): SealedStoreDatabase =
        Room.databaseBuilder(context, SealedStoreDatabase::class.java, "secure_store.db").build()

    fun warpVault(context: Context): SealedStoreDatabase =
        Room.databaseBuilder(context, SealedStoreDatabase::class.java, "warp_vault.db").build()
}
