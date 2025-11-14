package com.example.crypt.di

import android.content.Context
import com.example.crypt.data.database.CryptDatabase
import com.example.crypt.data.database.PasswordDao
import com.example.crypt.data.repository.PasswordRepository
import com.example.crypt.data.repository.PasswordRepositoryImpl
import com.example.crypt.data.security.EncryptionService
import com.example.crypt.data.security.KeystoreManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database and encryption dependencies.
 * Configures Room database with SQLCipher encryption and related services.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    
    /**
     * Binds the PasswordRepositoryImpl to the PasswordRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindPasswordRepository(
        passwordRepositoryImpl: PasswordRepositoryImpl
    ): PasswordRepository
    
    companion object {
        
        /**
         * Provides the KeystoreManager instance for hardware-backed key management.
         */
        @Provides
        @Singleton
        fun provideKeystoreManager(): KeystoreManager {
            return KeystoreManager()
        }
        
        /**
         * Provides the SecureMemoryManager instance for secure memory operations.
         */
        @Provides
        @Singleton
        fun provideSecureMemoryManager(): com.example.crypt.data.security.SecureMemoryManager {
            return com.example.crypt.data.security.SecureMemoryManager()
        }
        
        /**
         * Provides the EncryptionService instance for AES-256-GCM operations.
         */
        @Provides
        @Singleton
        fun provideEncryptionService(
            keystoreManager: KeystoreManager,
            secureMemoryManager: com.example.crypt.data.security.SecureMemoryManager
        ): EncryptionService {
            return EncryptionService(keystoreManager, secureMemoryManager)
        }
        
        /**
         * Provides the CryptDatabase instance with SQLCipher configuration.
         * Uses hardware-backed master key for database encryption.
         */
        @Provides
        @Singleton
        fun provideCryptDatabase(
            @ApplicationContext context: Context,
            keystoreManager: KeystoreManager
        ): CryptDatabase {
            return CryptDatabase.create(context, keystoreManager)
        }
        
        /**
         * Provides the PasswordDao from the CryptDatabase.
         */
        @Provides
        fun providePasswordDao(database: CryptDatabase): PasswordDao {
            return database.passwordDao()
        }
    }
}