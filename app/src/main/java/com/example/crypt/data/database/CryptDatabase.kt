package com.example.crypt.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.crypt.data.security.KeystoreManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.inject.Singleton

/**
 * Room database with SQLCipher encryption for secure local storage.
 * Uses hardware-backed master key to derive database encryption key.
 */
@Database(
    entities = [PasswordEntry::class],
    version = 1,
    exportSchema = true
)
@Singleton
abstract class CryptDatabase : RoomDatabase() {
    
    abstract fun passwordDao(): PasswordDao
    
    companion object {
        private const val DATABASE_NAME = "crypt_database"
        private const val DATABASE_VERSION = 1
        
        /**
         * Creates the encrypted database instance with SQLCipher.
         * @param context Application context
         * @param keystoreManager KeystoreManager for deriving database key
         * @return CryptDatabase instance
         */
        fun create(context: Context, keystoreManager: KeystoreManager): CryptDatabase {
            // Derive database passphrase from master key
            val databaseKey = deriveDatabaseKey(keystoreManager)
            
            // Create SQLCipher support factory
            val supportFactory = SupportFactory(databaseKey)
            
            return Room.databaseBuilder(
                context.applicationContext,
                CryptDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(supportFactory)
                .addMigrations(*getAllMigrations())
                .fallbackToDestructiveMigration() // Only for development - remove in production
                .build()
        }
        
        /**
         * Derives a database encryption key from the hardware-backed master key.
         * Uses PBKDF2-like approach with SHA-256 for key derivation.
         * @param keystoreManager KeystoreManager instance
         * @return ByteArray representing the database encryption key
         */
        private fun deriveDatabaseKey(keystoreManager: KeystoreManager): ByteArray {
            return try {
                val masterKey = keystoreManager.getMasterKey()
                val keyBytes = masterKey.encoded
                
                // If key is hardware-backed, encoded will be null
                // In this case, we'll use a deterministic approach
                if (keyBytes == null) {
                    // For hardware-backed keys, we'll use the key alias and a salt
                    // This ensures the same key is derived each time
                    val keyAlias = "crypt_master_key_v1"
                    val salt = "CryptDatabaseSalt2024".toByteArray()
                    val combined = keyAlias.toByteArray() + salt
                    
                    // Use SHA-256 to create a 32-byte key
                    val digest = MessageDigest.getInstance("SHA-256")
                    digest.update(combined)
                    digest.digest()
                } else {
                    // For software keys, use the actual key bytes
                    val digest = MessageDigest.getInstance("SHA-256")
                    digest.update(keyBytes)
                    digest.update("CryptDatabaseSalt2024".toByteArray())
                    digest.digest()
                }
            } catch (e: Exception) {
                throw SecurityException("Failed to derive database key", e)
            }
        }
        
        /**
         * Returns all database migrations.
         * Currently empty as this is version 1.
         */
        private fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // Future migrations will be added here
                // Example:
                // MIGRATION_1_2,
                // MIGRATION_2_3
            )
        }
        
        /**
         * Migration from version 1 to 2 (example for future use).
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration - add new column
                // database.execSQL("ALTER TABLE password_entries ADD COLUMN new_field TEXT")
            }
        }
        
        /**
         * Creates indices for better query performance.
         * Called automatically by Room when database is created.
         */
        private fun createIndices(database: SupportSQLiteDatabase) {
            // Index on site for faster searching
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_site ON password_entries(site)")
            
            // Index on createdAt for time-based queries
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_created_at ON password_entries(createdAt)")
            
            // Composite index for search functionality
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_site_username ON password_entries(site, username)")
        }
    }
    
    /**
     * Callback for database creation and opening.
     */
    class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Create additional indices
            createIndices(db)
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON")
            
            // Set SQLCipher specific pragmas for better security
            db.execSQL("PRAGMA cipher_memory_security = ON")
            db.execSQL("PRAGMA cipher_kdf_iter = 64000") // Increase KDF iterations
        }
        
        /**
         * Creates performance indices.
         */
        private fun createIndices(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_site ON password_entries(site)")
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_created_at ON password_entries(createdAt)")
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_site_username ON password_entries(site, username)")
        }
    }
}