# react-native-sip-smooth

[![npm version](https://img.shields.io/npm/v/react-native-sip-smooth.svg)](https://www.npmjs.com/package/react-native-sip-smooth)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/platform-iOS%20%7C%20Android-lightgrey.svg)](https://github.com/Ammar-Abid92/react-native-sip-smooth)

A comprehensive React Native library providing SIP/VoIP functionality using the Linphone SDK. This package offers advanced call management, user agent configuration, and instance ID support for enterprise-grade telephony applications.

## 🚀 Features

- **Full SIP/VoIP Support** - Complete implementation of SIP protocol using Linphone SDK
- **Multi-Transport Support** - UDP, TCP, and TLS transport protocols
- **Advanced Call Management** - Handle multiple calls, transfers, hold/unhold operations
- **Audio Device Control** - Manage speakers, microphones, and Bluetooth devices
- **Real-time Events** - Comprehensive callback system for call state changes
- **TypeScript Support** - Full type definitions included
- **Cross-Platform** - iOS and Android support
- **Instance Management** - Support for multiple SIP accounts and configurations

## 📋 Prerequisites

- React Native >= 0.72.0
- iOS 11.0+ / Android API level 21+
- react-native-device-info >= 10.0.0

## 📦 Installation

**Note: This is a public package.**

### 1. Install the Package

yarn add react-native-sip-smooth

# or

npm install react-native-sip-smooth

````

### 3. iOS Setup

```bash
cd ios && pod install
````

### 4. Android Setup

No additional setup required for Android.

## 🔧 Configuration

### iOS Permissions

Add the following to your `Info.plist`:

```xml
<key>NSMicrophoneUsageDescription</key>
<string>This app needs access to microphone for voice calls</string>
<key>NSCameraUsageDescription</key>
<string>This app needs access to camera for video calls</string>
```

### Android Permissions

Add to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## 🚀 Quick Start

```typescript
import {
  initialise,
  login,
  call,
  useCall,
  TransportType,
  InitializeOptions,
} from 'react-native-sip-smooth';

// Initialize the SIP engine
const initOptions: InitializeOptions = {
  userAgent: 'MyApp',
  platform: 'mobile',
  version: '1.0.0',
  instanceId: 'unique-instance-id',
  callTimeout: 30,
};

await initialise(initOptions);

// Login to SIP server
await login('username', 'password', 'sip.example.com', TransportType.Udp);

// Make a call
await call('sip:destination@example.com');
```

## 📚 API Reference

### Core Functions

#### `initialise(options?: InitializeOptions): Promise<void>`

Initialize the Linphone SDK with configuration options.

```typescript
interface InitializeOptions {
  userAgent?: string; // Custom user agent string
  platform?: string; // Platform identifier
  version?: string; // Application version
  instanceId?: string; // Unique instance identifier
  callTimeout?: number; // Call timeout in seconds
}
```

#### `login(username: string, password: string, domain: string, transport: TransportType): Promise<void>`

Authenticate with a SIP server.

#### `unregister(): Promise<void>`

Unregister from the SIP server.

### Call Management

#### `call(remoteUri: string): Promise<void>`

Initiate an outgoing call to the specified SIP URI.

#### `acceptCall(): Promise<void>`

Accept an incoming call.

#### `rejectCall(): Promise<void>`

Reject an incoming call.

#### `hangup(): Promise<void>`

End the current call.

#### `holdCall(): Promise<void>`

Put the current call on hold.

#### `unholdCall(): Promise<void>`

Resume a held call.

### Audio Control

#### `toggleMute(): Promise<void>`

Toggle microphone mute state.

#### `getMicStatus(): Promise<boolean>`

Get current microphone status.

#### `setAudioDevice(device: AudioDevice): Promise<void>`

Set the active audio device.

```typescript
type AudioDevice = 'bluetooth' | 'phone' | 'loudspeaker';
```

### Call State Hooks

#### `useCall(callbacks: Callbacks): void`

React hook for handling call state changes.

```typescript
interface Callbacks {
  onConnectionRequested?: () => void;
  onCallRequested?: () => void;
  onCallRinging?: () => void;
  onCallConnected?: () => void;
  onCallStreamsRunning?: () => void;
  onCallPaused?: () => void;
  onCallPausedByRemote?: () => void;
  onCallUpdating?: () => void;
  onCallUpdatedByRemote?: () => void;
  onCallReleased?: () => void;
  onCallError?: () => void;
  onCallEnd?: () => void;
  onCallPushIncomingReceived?: () => void;
  onAccountRegistrationStateChanged?: (param: any) => void;
}
```

### Audio Device Management

#### `useAudioDevices(callback: (devices: AudioDevices) => void): void`

React hook for monitoring audio device changes.

```typescript
interface AudioDevices {
  options: { [device in AudioDevice]: boolean };
  current: AudioDevice;
}
```

## 💡 Usage Examples

### Basic SIP Client

```typescript
import React, { useEffect, useState } from 'react';
import { View, Button, Alert } from 'react-native';
import {
  initialise,
  login,
  call,
  hangup,
  useCall,
  TransportType
} from 'react-native-sip-smooth';

const SipClient = () => {
  const [isConnected, setIsConnected] = useState(false);
  const [isInCall, setIsInCall] = useState(false);

  useCall({
    onCallConnected: () => {
      setIsInCall(true);
      Alert.alert('Call Connected');
    },
    onCallEnd: () => {
      setIsInCall(false);
      Alert.alert('Call Ended');
    },
    onCallError: () => {
      setIsInCall(false);
      Alert.alert('Call Error');
    }
  });

  const handleLogin = async () => {
    try {
      await initialise({
        userAgent: 'MyVoIPApp',
        callTimeout: 30
      });

      await login(
        'your-username',
        'your-password',
        'your-sip-server.com',
        TransportType.Udp
      );

      setIsConnected(true);
      Alert.alert('Connected to SIP server');
    } catch (error) {
      Alert.alert('Login failed', error.message);
    }
  };

  const handleCall = async () => {
    try {
      await call('sip:destination@example.com');
    } catch (error) {
      Alert.alert('Call failed', error.message);
    }
  };

  const handleHangup = async () => {
    try {
      await hangup();
    } catch (error) {
      Alert.alert('Hangup failed', error.message);
    }
  };

  return (
    <View style={{ padding: 20 }}>
      {!isConnected ? (
        <Button title="Connect" onPress={handleLogin} />
      ) : (
        <>
          {!isInCall ? (
            <Button title="Make Call" onPress={handleCall} />
          ) : (
            <Button title="Hang Up" onPress={handleHangup} />
          )}
        </>
      )}
    </View>
  );
};
```

### Advanced Call Management

```typescript
import React from 'react';
import {
  holdCall,
  unholdCall,
  toggleMute,
  blindTransferCall,
  getAllCallsStatus,
} from 'react-native-sip-smooth';

const AdvancedCallControls = () => {
  const handleHold = async () => {
    await holdCall();
  };

  const handleUnhold = async () => {
    await unholdCall();
  };

  const handleMute = async () => {
    await toggleMute();
  };

  const handleTransfer = async () => {
    await blindTransferCall(0, 'sip:transfer-target@example.com');
  };

  const checkCallStatus = async () => {
    const status = await getAllCallsStatus();
    console.log('Current calls:', status);
  };

  // Render your UI components
};
```

## 🔧 Transport Types

```typescript
enum TransportType {
  Udp = 0, // UDP transport
  Tcp = 1, // TCP transport
  Tls = 2, // TLS transport (secure)
  Dtls = 3, // DTLS transport (secure UDP)
}
```

## 🐛 Troubleshooting

### Common Issues

1. **"Package doesn't seem to be linked"**
   - Ensure you've run `pod install` on iOS
   - Rebuild your app after installation

2. **Authentication failures**
   - Verify your SIP credentials
   - Check network connectivity
   - Ensure correct transport type

3. **Audio issues**
   - Check microphone permissions
   - Verify audio device settings
   - Test with different audio routes

4. **Package conflicts with react-native-leenphone**
   - Completely uninstall `react-native-leenphone`: `npm uninstall react-native-leenphone`
   - Clean build artifacts: `npx react-native clean`
   - Remove build directories: `rm -rf android/app/build android/build`

- Reinstall this package: `npm install react-native-sip-smooth@latest`

5. **Android build errors (duplicate classes)**
   - Follow step 4 above to remove conflicts
   - Ensure only one SIP package is installed
   - Clean and rebuild: `cd android && ./gradlew clean && cd .. && npx react-native run-android`

### Debug Mode

Enable debug logging:

```typescript
// Add this before initializing
console.log('Enabling debug mode');
```

## 📄 License

This project is licensed under the GPL-3.0-or-later License - see the [LICENSE](LICENSE.md) file for details.

## 🤝 Contributing

Contributions are welcome! Please open issues or pull requests on GitHub.

## 📞 Support

For technical support and questions:

- **Email**: ammarabid890@gmail.com
- **Issues**: [GitHub Issues](https://github.com/Ammar-Abid92/react-native-sip-smooth/issues)

## 📋 Changelog

### [1.0.1] - 2025-08-25

#### Added

- Initial release with full SIP/VoIP functionality
- Support for multiple transport protocols
- Advanced call management features
- Audio device control
- TypeScript definitions
- Comprehensive event system

---

**Built with ❤️ by Muhammad Ammar Abid (@Ammar-Abid92)**
