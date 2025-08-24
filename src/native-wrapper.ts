import { NativeModules, Platform } from 'react-native';
import { NativeEventEmitter } from 'react-native';
import React from 'react';

const LINKING_ERROR =
  `The package 'react-native-leenphone' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const Sip = NativeModules.Sip
  ? NativeModules.Sip
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export enum TransportType {
  Udp = 0,
  Tcp = 1,
  Tls = 2,
  Dtls = 3,
}

export interface InitializeOptions {
  userAgent?: string;
  platform?: string;
  version?: string;
  instanceId?: string;
  callTimeout?: number; // Timeout in seconds for incoming calls (how long they will ring)
}

interface Callbacks {
  // First state an outgoing call will go through
  onConnectionRequested?: () => void;

  // First state an outgoing call will go through
  onCallRequested?: () => void;

  // Once remote accepts, ringing will commence (180 response)
  onCallRinging?: () => void;
  onCallConnected?: () => void;

  // This state indicates the call is active.
  // You may reach this state multiple times, for example after a pause/resume
  // or after the ICE negotiation completes
  // Wait for the call to be connected before allowing a call update
  onCallStreamsRunning?: () => void;
  onCallPaused?: () => void;
  onCallPausedByRemote?: () => void;

  // When we request a call update, for example when toggling video
  onCallUpdating?: () => void;
  onCallUpdatedByRemote?: () => void;
  onCallReleased?: () => void;
  onCallError?: () => void;
  onCallEnd?: () => void;
  onCallPushIncomingReceived?: () => void;
  onAccountRegistrationStateChanged?: (param: any) => void;
}

export async function initialise(options?: InitializeOptions): Promise<void> {
  return Sip.initialise(options || null);
}

export async function unregister(): Promise<void> {
  return Sip.unregister();
}

export function login(
  username: string,
  password: string,
  domain: string,
  transport: TransportType
): Promise<void> {
  return Sip.login(username, password, domain, transport);
}

export function useCall(callbacks: Callbacks = {}): void {
  React.useEffect(() => {
    // console.log('Initialising phone')
    const eventEmitter = new NativeEventEmitter(Sip);

    const eventListeners = Object.entries(callbacks).map(
      ([event, callback]) => {
        return eventEmitter.addListener(event.slice(2), callback);
      }
    );
    return () => eventListeners.forEach((listener) => listener.remove());
  }, []);
}

export async function call(remoteUri: string): Promise<void> {
  return Sip.outgoingCall(remoteUri);
}

export async function hangup(): Promise<void> {
  return Sip.hangUp();
}

export async function sendDtmf(dtmf: string): Promise<void> {
  return Sip.sendDtmf(dtmf);
}

export type AudioDevice = 'bluetooth' | 'phone' | 'loudspeaker';

export interface AudioDevices {
  options: { [device in AudioDevice]: boolean };
  current: AudioDevice;
}

export function useAudioDevices(
  callback: (device: AudioDevices) => void
): void {
  const scanAudioDevices = React.useCallback(
    () => Sip.scanAudioDevices().then(callback),
    [callback]
  );

  React.useEffect(() => {
    const eventEmitter = new NativeEventEmitter(Sip);

    const deviceListener = eventEmitter.addListener(
      'AudioDevicesChanged',
      scanAudioDevices
    );
    return () => deviceListener.remove();
  }, []);

  React.useEffect(() => {
    scanAudioDevices();
  }, []);
}

export async function setAudioDevice(device: AudioDevice) {
  if (device === 'bluetooth') return Sip.bluetoothAudio();
  if (device === 'loudspeaker') return Sip.loudAudio();
  if (device === 'phone') return Sip.phoneAudio();
}

export async function getMicStatus(): Promise<boolean> {
  return Sip.getMicStatus();
}

export async function toggleMute(): Promise<void> {
  return Sip.toggleMute();
}

export async function getAllCallsStatus(): Promise<any> {
  return Sip.getAllCallsStatus();
}

export async function getCallStatus(): Promise<any> {
  return Sip.getCallStatus();
}

export async function acceptCall(): Promise<void> {
  return Sip.acceptCall();
}

export async function rejectCall(): Promise<void> {
  return Sip.rejectCall();
}

export async function holdCall(): Promise<void> {
  return Sip.holdCall();
}

export async function unholdCall(): Promise<void> {
  return Sip.unholdCall();
}

export async function holdCallByIndex(callIndex: number): Promise<void> {
  return Sip.holdCallByIndex(callIndex);
}

export async function unholdCallByIndex(callIndex: number): Promise<void> {
  return Sip.unholdCallByIndex(callIndex);
}

export async function hangupCallByIndex(callIndex: number): Promise<void> {
  return Sip.hangupCallByIndex(callIndex);
}

export async function blindTransferCall(callIndex: number, transferTo: string): Promise<void> {
  return Sip.blindTransferCall(callIndex, transferTo);
}

export async function attendedTransferCall(fromCallIndex: number, toCallIndex: number): Promise<void> {
  return Sip.attendedTransferCall(fromCallIndex, toCallIndex);
}

export async function getRegistrationStatus(): Promise<any> {
  return Sip.getRegistrationStatus();
}

export async function setAudioDeviceByName(device: string): Promise<void> {
  return Sip.setAudioDevice(device);
}

export async function setUserAgent(appName: string, version: string): Promise<void> {
  return Sip.setUserAgent(appName, version);
}

export async function setCallTimeout(timeoutSeconds: number): Promise<void> {
  return Sip.setCallTimeout(timeoutSeconds);
}

export async function getCallTimeout(): Promise<number> {
  return Sip.getCallTimeout();
}
