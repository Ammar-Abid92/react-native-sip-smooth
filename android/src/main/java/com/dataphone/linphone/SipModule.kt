package com.reactnativesip.smooth

import android.util.Log
import com.facebook.react.bridge.*

import com.facebook.react.modules.core.DeviceEventManagerModule
import org.linphone.core.*
import android.content.pm.PackageManager


class SipModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  private val context = reactContext.applicationContext
  private val packageManager = context.packageManager
  private val reactContext = reactContext

  private var bluetoothMic: AudioDevice? = null
  private var bluetoothSpeaker: AudioDevice? = null
  private var earpiece: AudioDevice? = null
  private var loudMic: AudioDevice? = null
  private var loudSpeaker: AudioDevice? = null
  private var microphone: AudioDevice? = null

  private lateinit var core: Core
  private var instanceId: String = ""

  companion object {
    const val TAG = "SipModule"
  }

  override fun getName(): String {
    return "Sip"
  }

  private fun delete() {
    // To completely remove an Account
    if (!::core.isInitialized) {
      Log.w(TAG, "Core not initialized, cannot delete account")
      return
    }
    val account = core.defaultAccount
    account ?: return
    core.removeAccount(account)

    // To remove all accounts use
    core.clearAccounts()

    // Same for auth info
    core.clearAllAuthInfo()
  }

  private fun scanAndSetupAudioDevices() {
    if (!::core.isInitialized) {
      Log.w(TAG, "Core not initialized, cannot scan audio devices")
      return
    }
    
    Log.i(TAG, "Scanning and setting up audio devices")
    
    // Reset audio device references
    microphone = null
    earpiece = null
    loudSpeaker = null
    loudMic = null
    bluetoothSpeaker = null
    bluetoothMic = null

    // Scan available audio devices with error handling
    try {
      for (audioDevice in core.audioDevices) {
        Log.d(TAG, "Found audio device: ${audioDevice.type} - ${audioDevice.deviceName}")
        when (audioDevice.type) {
          AudioDevice.Type.Microphone -> microphone = audioDevice
          AudioDevice.Type.Earpiece -> earpiece = audioDevice
          AudioDevice.Type.Speaker -> if (audioDevice.hasCapability(AudioDevice.Capabilities.CapabilityPlay)) {
            loudSpeaker = audioDevice
          } else {
            loudMic = audioDevice
          }
          AudioDevice.Type.Bluetooth -> if (audioDevice.hasCapability(AudioDevice.Capabilities.CapabilityPlay)) {
            bluetoothSpeaker = audioDevice
          } else {
            bluetoothMic = audioDevice
          }
          else -> {}
        }
      }

      // Set default audio devices (earpiece for output, microphone for input)
      if (earpiece != null && microphone != null) {
        core.outputAudioDevice = earpiece
        core.inputAudioDevice = microphone
        Log.i(TAG, "Set default audio: earpiece + microphone")
      } else if (loudSpeaker != null && microphone != null) {
        core.outputAudioDevice = loudSpeaker
        core.inputAudioDevice = microphone
        Log.i(TAG, "Set default audio: speaker + microphone")
      } else {
        Log.w(TAG, "Could not find suitable audio devices")
      }
      
      Log.i(TAG, "Audio setup complete. Input: ${core.inputAudioDevice?.deviceName}, Output: ${core.outputAudioDevice?.deviceName}")
    } catch (e: Exception) {
      Log.e(TAG, "Error scanning audio devices: ${e.message}")
    }
  }

  private fun sendEvent(eventName: String, body: Any? = null) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
    .emit(eventName, body)
  }

  @ReactMethod
  fun acceptCall(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    // Find incoming call
    val incomingCall = core.calls.find { it.state == Call.State.IncomingReceived }
    if (incomingCall != null) {
      Log.i(TAG, "Accepting incoming call")
      
      // If there's a current active call, put it on hold first
      val activeCall = core.calls.find { it.state == Call.State.StreamsRunning }
      if (activeCall != null && activeCall != incomingCall) {
        Log.i(TAG, "Putting current call on hold before accepting new call")
        activeCall.pause()
      }
      
      incomingCall.accept()
      promise.resolve(true)
    } else {
      Log.w(TAG, "No incoming call to accept")
      promise.reject("No Call", "No incoming call to accept")
    }
  }

  @ReactMethod
  fun rejectCall(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    val call = core.currentCall
    if (call != null && call.state == Call.State.IncomingReceived) {
      Log.i(TAG, "Rejecting incoming call")
      call.decline(Reason.Declined)
      promise.resolve(true)
    } else {
      Log.w(TAG, "No incoming call to reject")
      promise.reject("No Call", "No incoming call to reject")
    }
  }


  @ReactMethod
  fun addListener(eventName: String) {
    Log.d(TAG, "Added listener: $eventName")
  }

  @ReactMethod
  fun bluetoothAudio(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    if (bluetoothMic != null) {
      core.inputAudioDevice = bluetoothMic
    }

    if (bluetoothSpeaker != null) {
      core.outputAudioDevice = bluetoothSpeaker
    }

    promise.resolve(true)
  }

  @ReactMethod
  fun hangUp(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    Log.i(TAG, "Trying to hang up")
    if (core.callsNb == 0) return

    // If the call state isn't paused, we can get it using core.currentCall
    val call = if (core.currentCall != null) core.currentCall else core.calls[0]
    if (call != null) {
      // Terminating a call is quite simple
      call.terminate()
      promise.resolve(null)
    } else {
      promise.reject("No call", "No call to terminate")
    }
  }

  @ReactMethod
  fun initialise(options: ReadableMap?, promise: Promise) {
    Log.i(TAG, "initialise() method is called from react native")

    val factory = Factory.instance()
    
    // Configure logging before creating the core
    factory.setLoggerDomain("linphone")
    factory.enableLogcatLogs(true)
    val logger = factory.getLoggingService()
    logger.setLogLevel(LogLevel.Debug)
    
    core = factory.createCore(null, null, context)
    
    // Configure user agent with app name, platform and version from React Native
  val appName = options?.getString("userAgent") ?: "react-native-sip-smooth"
    val platformName = options?.getString("platform") ?: "Android ${android.os.Build.VERSION.RELEASE}"
    val appVersion = options?.getString("version") ?: try {
      val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
      packageInfo.versionName ?: "0.0.1"
    } catch (e: PackageManager.NameNotFoundException) {
      "0.0.1"
    }
    val instanceId = options?.getString("instanceId") ?: ""
    val callTimeout = options?.getInt("callTimeout") ?: 60 // Default 60 seconds
    
    // Store instance ID for later use
    this.instanceId = instanceId
    
    // Set user agent
    core.setUserAgent(appName, "$platformName v$appVersion")
    Log.i(TAG, "User agent set to: $appName $platformName v$appVersion")
    
    // Set instance ID if provided
    if (instanceId.isNotEmpty()) {
      Log.i(TAG, "Instance ID set to: $instanceId")
      // We'll use this instance ID when creating account params to ensure unique contact addresses
    }
    
    // Configure call timeout (how long the call will ring before timing out)
    core.incTimeout = callTimeout
    Log.i(TAG, "Call timeout set to: $callTimeout seconds")
    
    // Configure audio settings for proper media transmission
    core.isEchoCancellationEnabled = true
    core.isEchoLimiterEnabled = true

    core.start()
    Log.i(TAG, "SIP core started")

    // Initialize audio devices after core start
    scanAndSetupAudioDevices()

    val coreListener = object : CoreListenerStub() {
      override fun onAudioDevicesListUpdated(core: Core) {
        sendEvent("AudioDevicesChanged")
      }

      override fun onCallStateChanged(
        core: Core,
        call: Call,
        state: Call.State?,
        message: String
      ) {
        when (state) {
          Call.State.IncomingReceived -> {
            // Allow incoming calls to stay active - do not auto-terminate
            // call.terminate() // Commented out to allow incoming calls
            
            // A new call is incoming, notify the JS layer
            val map = Arguments.createMap()
            map.putString("remoteAddress", call.remoteAddress?.asStringUriOnly())
            map.putString("displayName", call.remoteAddress?.displayName)
            // We use the same event as push notifications for consistency
            sendEvent("CallPushIncomingReceived", map)
          }
          Call.State.OutgoingInit -> {
            // First state an outgoing call will go through
            sendEvent("ConnectionRequested")
          }
          Call.State.OutgoingProgress -> {
            // First state an outgoing call will go through
            sendEvent("CallRequested")
          }
          Call.State.OutgoingRinging -> {
            // Once remote accepts, ringing will commence (180 response)
            sendEvent("CallRinging")
          }
          Call.State.Connected -> {
            sendEvent("CallConnected")
          }
          Call.State.StreamsRunning -> {
            // This state indicates the call is active.
            // You may reach this state multiple times, for example after a pause/resume
            // or after the ICE negotiation completes
            // Wait for the call to be connected before allowing a call update
            sendEvent("CallStreamsRunning")
          }
          Call.State.Paused -> {
            sendEvent("CallPaused")
          }
          Call.State.PausedByRemote -> {
            sendEvent("CallPausedByRemote")
          }
          Call.State.Updating -> {
            // When we request a call update, for example when toggling video
            sendEvent("CallUpdating")
          }
          Call.State.UpdatedByRemote -> {
            sendEvent("CallUpdatedByRemote")
          }
          Call.State.Released -> {
            sendEvent("CallReleased")
          }
          Call.State.Error -> {
            sendEvent("CallError")
          }
          Call.State.End -> {
            sendEvent("CallEnd")
          }
          Call.State.PushIncomingReceived -> {
            sendEvent("CallPushIncomingReceived")
            // This event has no payload from the native side,
            // but we keep the case for compatibility.
            // The IncomingReceived case now handles passing data.
            sendEvent("CallPushIncomingReceived", null)
          }
          else -> {
          }
        }
      }

      override fun onAccountRegistrationStateChanged(core: Core, account: Account, state: RegistrationState?, message: String) {
        Log.i(TAG, "Account registration state changed: $state for ${account.params?.identityAddress?.asString()}")
        Log.i(TAG, "Registration message: $message")
        Log.i(TAG, "Account contact address: ${account.contactAddress?.asString()}")
        
        val map = Arguments.createMap()

        val coreMap = Arguments.createMap()
        coreMap.putString("accountCreatorUrl", core.accountCreatorUrl)
        coreMap.putString("adaptiveRateAlgorithm", core.adaptiveRateAlgorithm)
        coreMap.putString("httpProxyHost", core.httpProxyHost)
        coreMap.putString("identity", core.identity)
        coreMap.putString("mediaDevice", core.mediaDevice)
        coreMap.putString("primaryContact", core.primaryContact)
        coreMap.putString("remoteRingbackTone", core.remoteRingbackTone)
        coreMap.putString("provisioningUri", core.provisioningUri)
        coreMap.putString("rootCa", core.rootCa)
        coreMap.putString("tlsCert", core.tlsCert)
        coreMap.putString("tlsCertPath", core.tlsCertPath)
        coreMap.putString("tlsKey", core.tlsKey)
        coreMap.putString("tlsKeyPath", core.tlsKeyPath)
        map.putMap("core", coreMap)

        val accountMap = Arguments.createMap()
        accountMap.putString("contactAddressDomain", account.contactAddress?.domain)
        accountMap.putString("contactAddressUsername", account.contactAddress?.username)
        accountMap.putString("contactAddressDisplayName", account.contactAddress?.displayName)
        accountMap.putString("contactAddressPassword", account.contactAddress?.password)
        map.putMap("account", accountMap)

        map.putString("state", state.toString())
        map.putString("message", message)

        sendEvent("AccountRegistrationStateChanged", map)
      }
    }

    core.addListener(coreListener)
    promise.resolve(null)
  }

  @ReactMethod
  fun login(username: String, password: String, domain: String, transportType: Int, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized. Call initialise() first.")
      return
    }
    
    var _transportType = TransportType.Tcp
    if (transportType == 0) { _transportType = TransportType.Udp }
    if (transportType == 2) { _transportType = TransportType.Tls }
    if (transportType == 3) { _transportType = TransportType.Dtls }

    // To configure a SIP account, we need an Account object and an AuthInfo object
    // The first one is how to connect to the proxy server, the second one stores the credentials

    // The auth info can be created from the Factory as it's only a data class
    // userID is set to null as it's the same as the username in our case
    // ha1 is set to null as we are using the clear text password. Upon first register, the hash will be computed automatically.
    // The realm will be determined automatically from the first register, as well as the algorithm
    val authInfo =
      Factory.instance().createAuthInfo(username, null, password, null, null, domain, null)

    // Account object replaces deprecated ProxyConfig object
    // Account object is configured through an AccountParams object that we can obtain from the Core
    val accountParams = core.createAccountParams()

    // A SIP account is identified by an identity address that we can construct from the username and domain
    val identity = Factory.instance().createAddress("sip:$username@$domain")
    accountParams.identityAddress = identity

    // We also need to configure where the proxy server is located
    val address = Factory.instance().createAddress("sip:$domain")
    // We use the Address object to easily set the transport protocol
    address?.transport = _transportType
    accountParams.serverAddress = address
    // And we ensure the account will start the registration process
    accountParams.isRegisterEnabled = true
    
    // Configure NAT policy for this account
    accountParams.natPolicy = core.natPolicy
    
    // Enable NAT traversal
    accountParams.isPublishEnabled = false
    accountParams.isOutboundProxyEnabled = true

    // Use instance ID to create unique contact address if available
    if (instanceId.isNotEmpty()) {
      // Create a unique contact address using the instance ID
      // This helps distinguish between multiple app instances
      try {
        // Use setContactParameters method directly with standard SIP parameter format
        val contactParams = "instance=$instanceId"
        accountParams.setContactParameters(contactParams)
        Log.i(TAG, "Contact parameters set using setContactParameters with instance ID: $instanceId")
      } catch (e: Exception) {
        Log.w(TAG, "Failed to set contact parameters with instance ID: ${e.message}")
      }
    }

    // Now that our AccountParams is configured, we can create the Account object
    val account = core.createAccount(accountParams)

    // Now let's add our objects to the Core
    core.addAuthInfo(authInfo)
    core.addAccount(account)

    // Also set the newly added account as default
    core.defaultAccount = account

    // We can also register a callback on the Account object
    account.addListener { _, state, message ->
      when (state) {
        RegistrationState.Ok -> {
          promise.resolve(true)
        }
        RegistrationState.Cleared -> {
          promise.resolve(false)
        }
        RegistrationState.Failed -> {
          Log.e(TAG, "Registration failed: $message")
          Log.e(TAG, "Registration state: $state")
          promise.reject("Authentication error", message)
        }
        else -> {

        }
      }
    }
  }

  @ReactMethod
  fun loudAudio(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    if (loudMic != null) {
      core.inputAudioDevice = loudMic
    } else if (microphone != null) {
      core.inputAudioDevice = microphone
    }

    if (loudSpeaker != null) {
      core.outputAudioDevice = loudSpeaker
    }

    promise.resolve(true)
  }

  @ReactMethod
  fun micEnabled(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    promise.resolve(core.isMicEnabled)
  }

  @ReactMethod
  fun getMicStatus(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    promise.resolve(core.isMicEnabled)
  }

  @ReactMethod
  fun getRegistrationStatus(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    val account = core.defaultAccount
    if (account != null) {
      Log.i(TAG, "Default account found: ${account.params?.identityAddress?.asString()}")
      Log.i(TAG, "Account state: ${account.state}")
      Log.i(TAG, "Account contact address: ${account.contactAddress?.asString()}")
      Log.i(TAG, "Publish enabled: ${account.params?.isPublishEnabled}")
      
      val map = Arguments.createMap()
      map.putString("state", account.state.toString())
      map.putString("identity", account.params?.identityAddress?.asString())
      map.putString("contact", account.contactAddress?.asString())
      promise.resolve(map)
    } else {
      promise.reject("No Account", "No default account found")
    }
  }

  @ReactMethod
  fun setAudioDevice(device: String, promise: Promise) {
    when (device) {
      "bluetooth" -> bluetoothAudio(promise)
      "phone" -> phoneAudio(promise)
      "loudspeaker" -> loudAudio(promise)
      else -> promise.reject("Invalid Device", "Unknown audio device: $device")
    }
  }

  @ReactMethod
  fun outgoingCall(recipient: String, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    // Debug: Print default account information
    val defaultAccount = core.defaultAccount
    if (defaultAccount != null) {
      Log.i(TAG, "Default account found: ${defaultAccount.params?.identityAddress}")
      Log.i(TAG, "Account state: ${defaultAccount.state}")
      Log.i(TAG, "Account contact address: ${defaultAccount.contactAddress}")
    } else {
      Log.w(TAG, "No default account found")
    }
    
    // Ensure audio devices are properly set
    if (core.inputAudioDevice == null || core.outputAudioDevice == null) {
      Log.w(TAG, "Audio devices not set, re-scanning...")
      scanAndSetupAudioDevices()
    }
    
    // As for everything we need to get the SIP URI of the remote and convert it to an Address
    val remoteAddress = Factory.instance().createAddress(recipient)
    if (remoteAddress == null) {
      promise.reject("Invalid SIP URI", "Invalid SIP URI")
    } else {
      Log.i(TAG, "Outgoing call to $recipient")
      
      // We also need a CallParams object
      // Create call params expects a Call object for incoming calls, but for outgoing we must use null safely
      val params = core.createCallParams(null)
      params ?: return

      // Configure call parameters for proper audio
      params.mediaEncryption = MediaEncryption.None
      params.isAudioEnabled = true  // Ensure audio is enabled
      params.isVideoEnabled = false // Disable video to focus on audio
      
      Log.i(TAG, "Call params configured - Audio enabled: ${params.isAudioEnabled}")
      Log.i(TAG, "Current audio devices - Input: ${core.inputAudioDevice?.deviceName}, Output: ${core.outputAudioDevice?.deviceName}")

      // Finally we start the call
      val call = core.inviteAddressWithParams(remoteAddress, params)
      if (call != null) {
        Log.i(TAG, "Call initiated successfully")
      } else {
        Log.e(TAG, "Failed to initiate call")
      }

      promise.resolve(null)
    }

  }

  @ReactMethod
  fun phoneAudio(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    if (microphone != null) {
      core.inputAudioDevice = microphone
    }

    if (earpiece != null) {
      core.outputAudioDevice = earpiece
    }

    promise.resolve(true)
  }

  @ReactMethod
  fun removeListeners(count: Int) {
    Log.d(TAG, "Removed $count listener(s)")
  }

  @ReactMethod
  fun scanAudioDevices(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    scanAndSetupAudioDevices()

    val options = Arguments.createMap()
    options.putBoolean("phone", earpiece != null && microphone != null)
    options.putBoolean("bluetooth", bluetoothMic != null || bluetoothSpeaker != null)
    options.putBoolean("loudspeaker", loudSpeaker != null)

    var current = "phone"
    if (core.outputAudioDevice?.type == AudioDevice.Type.Bluetooth || core.inputAudioDevice?.type == AudioDevice.Type.Bluetooth) {
      current = "bluetooth"
    } else if (core.outputAudioDevice?.type == AudioDevice.Type.Speaker) {
      current = "loudspeaker"
    }

    val result = Arguments.createMap()
    result.putString("current", current)
    result.putMap("options", options)
    promise.resolve(result)
  }

  @ReactMethod
  fun sendDtmf(dtmf: String, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    val call = core.currentCall
    if (call == null) {
      promise.reject("No Active Call", "Cannot send DTMF: no active call")
      return
    }
    
    if (call.state != Call.State.StreamsRunning && call.state != Call.State.Connected) {
      promise.reject("Call Not Active", "Cannot send DTMF: call is not in active state")
      return
    }
    
    if (dtmf.isEmpty()) {
      promise.reject("Invalid DTMF", "DTMF string cannot be empty")
      return
    }
    
    try {
      // Send each character in the DTMF string
      for (char in dtmf) {
        if (char.isDigit() || char in "ABCD*#") {
          call.sendDtmf(char)
          Log.i(TAG, "Sent DTMF tone: $char")
        } else {
          Log.w(TAG, "Invalid DTMF character: $char")
        }
      }
      promise.resolve(true)
    } catch (e: Exception) {
      Log.e(TAG, "Error sending DTMF: ${e.message}")
      promise.reject("DTMF Error", "Failed to send DTMF: ${e.message}")
    }
  }

  @ReactMethod
  fun toggleMute(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    val micEnabled = core.isMicEnabled
    core.isMicEnabled = !micEnabled
    promise.resolve(!micEnabled)
  }

  @ReactMethod
  fun unregister(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    // Here we will disable the registration of our Account
    val account = core.defaultAccount
    account ?: return

    // Returned params object is const, so to make changes we first need to clone it
    val params = account.params.clone()

    params.isRegisterEnabled = false
    account.params = params
    core.removeAccount(account)
    core.clearAllAuthInfo()

    promise.resolve(true)
  }

  @ReactMethod
  fun holdCall(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    val call = core.currentCall
    if (call != null && call.state == Call.State.StreamsRunning) {
      call.pause()
      promise.resolve(true)
    } else if (call != null && call.state == Call.State.Paused) {
      // Already on hold
      promise.resolve(true)
    } else {
      promise.reject("No Active Call", "No active call to put on hold")
    }
  }

  @ReactMethod
  fun unholdCall(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    val call = core.currentCall
    if (call != null && call.state == Call.State.Paused) {
      call.resume()
      promise.resolve(true)
    } else if (call != null && call.state == Call.State.StreamsRunning) {
      // Already active
      promise.resolve(true)
    } else {
      // Check all calls for a paused one
      for (callInList in core.calls) {
        if (callInList.state == Call.State.Paused) {
          callInList.resume()
          promise.resolve(true)
          return
        }
      }
      promise.reject("No Call On Hold", "No call on hold to resume")
    }
  }

  @ReactMethod
  fun getCallStatus(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    val call = core.currentCall
    if (call != null) {
      val status = Arguments.createMap()
      status.putString("state", call.state.toString())
      status.putString("remoteAddress", call.remoteAddress?.asStringUriOnly())
      status.putString("displayName", call.remoteAddress?.displayName)
      status.putBoolean("isOnHold", call.state == Call.State.Paused)
      status.putBoolean("isActive", call.state == Call.State.StreamsRunning)
      promise.resolve(status)
    } else {
      promise.reject("No Call", "No current call")
    }
  }

  @ReactMethod
  fun getAllCallsStatus(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    val callsArray = Arguments.createArray()
    val currentCall = core.currentCall
    
    for (i in core.calls.indices) {
      val call = core.calls[i]
      val callInfo = Arguments.createMap()
      callInfo.putString("state", call.state.toString())
      callInfo.putString("remoteAddress", call.remoteAddress?.asStringUriOnly())
      callInfo.putString("displayName", call.remoteAddress?.displayName)
      callInfo.putBoolean("isOnHold", call.state == Call.State.Paused)
      callInfo.putBoolean("isActive", call.state == Call.State.StreamsRunning)
      callInfo.putBoolean("isCurrent", call == currentCall)
      callInfo.putBoolean("isIncoming", call.state == Call.State.IncomingReceived)
      callInfo.putInt("index", i)
      callsArray.pushMap(callInfo)
    }
    
    val result = Arguments.createMap()
    result.putArray("calls", callsArray)
    result.putInt("totalCalls", core.calls.size)
    promise.resolve(result)
  }

  @ReactMethod
  fun holdCallByIndex(callIndex: Int, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    if (callIndex < 0 || callIndex >= core.calls.size) {
      promise.reject("Invalid Index", "Invalid call index")
      return
    }
    
    val call = core.calls[callIndex]
    if (call.state == Call.State.StreamsRunning) {
      Log.i(TAG, "Putting call $callIndex on hold")
      call.pause()
      promise.resolve(true)
    } else if (call.state == Call.State.Paused) {
      Log.i(TAG, "Call $callIndex is already on hold")
      promise.resolve(true)
    } else {
      promise.reject("Invalid Call State", "Cannot hold call in state: ${call.state}")
    }
  }

  @ReactMethod
  fun unholdCallByIndex(callIndex: Int, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    if (callIndex < 0 || callIndex >= core.calls.size) {
      promise.reject("Invalid Index", "Invalid call index")
      return
    }
    
    val call = core.calls[callIndex]
    if (call.state == Call.State.Paused) {
      Log.i(TAG, "Resuming call $callIndex from hold")
      
      // Put any currently active call on hold first
      val activeCall = core.calls.find { it.state == Call.State.StreamsRunning }
      if (activeCall != null && activeCall != call) {
        Log.i(TAG, "Putting current active call on hold before resuming call $callIndex")
        activeCall.pause()
      }
      
      call.resume()
      promise.resolve(true)
    } else if (call.state == Call.State.StreamsRunning) {
      Log.i(TAG, "Call $callIndex is already active")
      promise.resolve(true)
    } else {
      promise.reject("Invalid Call State", "Cannot unhold call in state: ${call.state}")
    }
  }

  @ReactMethod
  fun hangupCallByIndex(callIndex: Int, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    if (callIndex < 0 || callIndex >= core.calls.size) {
      promise.reject("Invalid Index", "Invalid call index")
      return
    }
    
    val call = core.calls[callIndex]
    Log.i(TAG, "Terminating call $callIndex")
    call.terminate()
    promise.resolve(true)
  }

  @ReactMethod
  fun blindTransferCall(callIndex: Int, transferTo: String, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    if (callIndex < 0 || callIndex >= core.calls.size) {
      promise.reject("Invalid Index", "Invalid call index")
      return
    }
    
    val call = core.calls[callIndex]
    val transferAddress = Factory.instance().createAddress(transferTo)
    
    if (transferAddress == null) {
      promise.reject("Invalid Address", "Invalid transfer address: $transferTo")
      return
    }
    
    try {
      Log.i(TAG, "Performing blind transfer of call $callIndex to $transferTo")
      call.transferTo(transferAddress)
      promise.resolve(true)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to transfer call: ${e.message}")
      promise.reject("Transfer Failed", "Failed to transfer call: ${e.message}")
    }
  }

  @ReactMethod
  fun attendedTransferCall(fromCallIndex: Int, toCallIndex: Int, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    if (fromCallIndex < 0 || fromCallIndex >= core.calls.size) {
      promise.reject("Invalid Index", "Invalid from call index")
      return
    }
    
    if (toCallIndex < 0 || toCallIndex >= core.calls.size) {
      promise.reject("Invalid Index", "Invalid to call index")
      return
    }
    
    val fromCall = core.calls[fromCallIndex]
    val toCall = core.calls[toCallIndex]
    
    try {
      Log.i(TAG, "Performing attended transfer from call $fromCallIndex to call $toCallIndex")
      fromCall.transferToAnother(toCall)
      promise.resolve(true)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to transfer call: ${e.message}")
      promise.reject("Transfer Failed", "Failed to transfer call: ${e.message}")
    }
  }

  @ReactMethod
  fun setUserAgent(appName: String, version: String, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    val platformName = "Android ${android.os.Build.VERSION.RELEASE}"
    val userAgentString = if (version.isNotEmpty()) {
      "$platformName v$version"
    } else {
      platformName
    }
    
    core.setUserAgent(appName, userAgentString)
    Log.i(TAG, "User agent updated to: $appName $userAgentString")
    promise.resolve(true)
  }

  @ReactMethod
  fun setCallTimeout(timeoutSeconds: Int, promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    // Set the incoming call timeout (how long the call will ring)
    core.incTimeout = timeoutSeconds
    Log.i(TAG, "Call timeout updated to: $timeoutSeconds seconds")
    promise.resolve(true)
  }

  @ReactMethod
  fun getCallTimeout(promise: Promise) {
    if (!::core.isInitialized) {
      promise.reject("Core Not Initialized", "Linphone core has not been initialized")
      return
    }
    
    val timeout = core.incTimeout
    Log.i(TAG, "Current call timeout: $timeout seconds")
    promise.resolve(timeout)
  }
}
