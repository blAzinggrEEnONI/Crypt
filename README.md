# Crypt 🔐

A secure, offline-first Android password manager built with modern Android development practices. Crypt keeps your passwords safe with military-grade encryption, biometric authentication, and zero cloud dependencies.

## Features

### Security First
- **AES-256 Encryption**: All passwords encrypted using industry-standard AES-256-GCM
- **Android Keystore Integration**: Encryption keys stored securely in hardware-backed keystore
- **SQLCipher Database**: Encrypted local database with no cloud sync
- **Biometric Authentication**: Fingerprint and face unlock support
- **Auto-lock**: Configurable timeout to lock the app when inactive
- **Memory Protection**: Screenshot and screen recording prevention on sensitive screens

### Core Functionality
- **Password Vault**: Store unlimited passwords with title, username, password, and notes
- **Password Generator**: Create strong, random passwords with customizable options
  - Configurable length (8-32 characters)
  - Include/exclude uppercase, lowercase, numbers, and symbols
- **Search & Filter**: Quickly find passwords in your vault
- **Copy to Clipboard**: One-tap copy with auto-clear after 30 seconds

### User Experience
- **Material 3 Design**: Modern, clean interface following Material Design guidelines
- **Dark Mode Support**: Automatic theme switching
- **Offline-First**: No internet connection required, ever
- **Zero Tracking**: No analytics, no telemetry, complete privacy

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture with MVVM
- **Dependency Injection**: Hilt
- **Database**: Room + SQLCipher
- **Security**: Android Keystore, Biometric API
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## Architecture

The app follows Clean Architecture principles with clear separation of concerns:

```
app/
├── data/           # Data layer
│   ├── database/   # Room database and DAOs
│   ├── repository/ # Repository implementations
│   └── security/   # Encryption and keystore management
├── domain/         # Business logic layer
│   ├── model/      # Domain models
│   ├── usecase/    # Use cases
│   └── service/    # Domain services
└── ui/             # Presentation layer
    ├── screen/     # Composable screens
    ├── viewmodel/  # ViewModels
    ├── navigation/ # Navigation setup
    └── theme/      # Material 3 theming
```

## Security Architecture

1. **Master Password**: User creates a master password on first launch
2. **Key Derivation**: PBKDF2 derives encryption key from master password
3. **Keystore Storage**: Derived key encrypted and stored in Android Keystore
4. **Data Encryption**: All passwords encrypted with AES-256-GCM before database storage
5. **Database Encryption**: SQLCipher encrypts the entire database file
6. **Biometric Auth**: Optional biometric unlock using Android Biometric API

## Building the Project

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11 or later
- Android SDK 34

### Steps
1. Clone the repository
```bash
git clone https://github.com/blAzinggrEEnONI/Crypt.git
cd Crypt
```

2. Open in Android Studio

3. Sync Gradle dependencies

4. Run on emulator or physical device

## Testing

The project includes comprehensive test coverage:

- **Unit Tests**: Domain logic and use cases
- **Integration Tests**: Repository and database operations
- **Instrumented Tests**: Encryption, keystore, and end-to-end flows

Run tests:
```bash
./gradlew test           # Unit tests
./gradlew connectedTest  # Instrumented tests
```

## Privacy & Security

- **No Network Permissions**: App cannot access the internet
- **No Cloud Sync**: All data stays on your device
- **No Analytics**: Zero tracking or telemetry
- **Open Source**: Code is transparent and auditable
- **Local Only**: Your master password never leaves your device

## Roadmap

- [ ] Password strength indicator
- [ ] Import/Export functionality (encrypted)
- [ ] Password history tracking
- [ ] Breach detection (offline database)
- [ ] Custom categories/folders
- [ ] Secure notes
- [ ] Multi-language support

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the MIT License.

## Disclaimer

This is a personal project built for learning and portfolio purposes. While security best practices are followed, use at your own risk. Always maintain backups of critical passwords.

---

Built with ❤️ using Kotlin and Jetpack Compose
