import { NativeModules, Platform } from 'react-native';
import { NativeEventEmitter } from 'react-native';
import React from 'react';
const LINKING_ERROR = `The package 'react-native-leenphone' doesn't seem to be linked. Make sure: \n\n` + Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo managed workflow\n';
const Sip = NativeModules.Sip ? NativeModules.Sip : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
export let TransportType = /*#__PURE__*/function (TransportType) {
  TransportType[TransportType["Udp"] = 0] = "Udp";
  TransportType[TransportType["Tcp"] = 1] = "Tcp";
  TransportType[TransportType["Tls"] = 2] = "Tls";
  TransportType[TransportType["Dtls"] = 3] = "Dtls";
  return TransportType;
}({});
export async function initialise(options) {
  return Sip.initialise(options || null);
}
export async function unregister() {
  return Sip.unregister();
}
export function login(username, password, domain, transport) {
  return Sip.login(username, password, domain, transport);
}
export function useCall(callbacks = {}) {
  React.useEffect(() => {
    // console.log('Initialising phone')
    const eventEmitter = new NativeEventEmitter(Sip);
    const eventListeners = Object.entries(callbacks).map(([event, callback]) => {
      return eventEmitter.addListener(event.slice(2), callback);
    });
    return () => eventListeners.forEach(listener => listener.remove());
  }, []);
}
export async function call(remoteUri) {
  return Sip.outgoingCall(remoteUri);
}
export async function hangup() {
  return Sip.hangUp();
}
export async function sendDtmf(dtmf) {
  return Sip.sendDtmf(dtmf);
}
export function useAudioDevices(callback) {
  const scanAudioDevices = React.useCallback(() => Sip.scanAudioDevices().then(callback), [callback]);
  React.useEffect(() => {
    const eventEmitter = new NativeEventEmitter(Sip);
    const deviceListener = eventEmitter.addListener('AudioDevicesChanged', scanAudioDevices);
    return () => deviceListener.remove();
  }, []);
  React.useEffect(() => {
    scanAudioDevices();
  }, []);
}
export async function setAudioDevice(device) {
  if (device === 'bluetooth') return Sip.bluetoothAudio();
  if (device === 'loudspeaker') return Sip.loudAudio();
  if (device === 'phone') return Sip.phoneAudio();
}
export async function getMicStatus() {
  return Sip.getMicStatus();
}
export async function toggleMute() {
  return Sip.toggleMute();
}
export async function getAllCallsStatus() {
  return Sip.getAllCallsStatus();
}
export async function getCallStatus() {
  return Sip.getCallStatus();
}
export async function acceptCall() {
  return Sip.acceptCall();
}
export async function rejectCall() {
  return Sip.rejectCall();
}
export async function holdCall() {
  return Sip.holdCall();
}
export async function unholdCall() {
  return Sip.unholdCall();
}
export async function holdCallByIndex(callIndex) {
  return Sip.holdCallByIndex(callIndex);
}
export async function unholdCallByIndex(callIndex) {
  return Sip.unholdCallByIndex(callIndex);
}
export async function hangupCallByIndex(callIndex) {
  return Sip.hangupCallByIndex(callIndex);
}
export async function blindTransferCall(callIndex, transferTo) {
  return Sip.blindTransferCall(callIndex, transferTo);
}
export async function attendedTransferCall(fromCallIndex, toCallIndex) {
  return Sip.attendedTransferCall(fromCallIndex, toCallIndex);
}
export async function getRegistrationStatus() {
  return Sip.getRegistrationStatus();
}
export async function setAudioDeviceByName(device) {
  return Sip.setAudioDevice(device);
}
export async function setUserAgent(appName, version) {
  return Sip.setUserAgent(appName, version);
}
export async function setCallTimeout(timeoutSeconds) {
  return Sip.setCallTimeout(timeoutSeconds);
}
export async function getCallTimeout() {
  return Sip.getCallTimeout();
}
//# sourceMappingURL=native-wrapper.js.map