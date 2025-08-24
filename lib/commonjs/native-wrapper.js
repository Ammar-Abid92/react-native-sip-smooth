"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.TransportType = void 0;
exports.acceptCall = acceptCall;
exports.attendedTransferCall = attendedTransferCall;
exports.blindTransferCall = blindTransferCall;
exports.call = call;
exports.getAllCallsStatus = getAllCallsStatus;
exports.getCallStatus = getCallStatus;
exports.getCallTimeout = getCallTimeout;
exports.getMicStatus = getMicStatus;
exports.getRegistrationStatus = getRegistrationStatus;
exports.hangup = hangup;
exports.hangupCallByIndex = hangupCallByIndex;
exports.holdCall = holdCall;
exports.holdCallByIndex = holdCallByIndex;
exports.initialise = initialise;
exports.login = login;
exports.rejectCall = rejectCall;
exports.sendDtmf = sendDtmf;
exports.setAudioDevice = setAudioDevice;
exports.setAudioDeviceByName = setAudioDeviceByName;
exports.setCallTimeout = setCallTimeout;
exports.setUserAgent = setUserAgent;
exports.toggleMute = toggleMute;
exports.unholdCall = unholdCall;
exports.unholdCallByIndex = unholdCallByIndex;
exports.unregister = unregister;
exports.useAudioDevices = useAudioDevices;
exports.useCall = useCall;
var _reactNative = require("react-native");
var _react = _interopRequireDefault(require("react"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
const LINKING_ERROR = `The package 'react-native-leenphone' doesn't seem to be linked. Make sure: \n\n` + _reactNative.Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo managed workflow\n';
const Sip = _reactNative.NativeModules.Sip ? _reactNative.NativeModules.Sip : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
let TransportType = exports.TransportType = /*#__PURE__*/function (TransportType) {
  TransportType[TransportType["Udp"] = 0] = "Udp";
  TransportType[TransportType["Tcp"] = 1] = "Tcp";
  TransportType[TransportType["Tls"] = 2] = "Tls";
  TransportType[TransportType["Dtls"] = 3] = "Dtls";
  return TransportType;
}({});
async function initialise(options) {
  return Sip.initialise(options || null);
}
async function unregister() {
  return Sip.unregister();
}
function login(username, password, domain, transport) {
  return Sip.login(username, password, domain, transport);
}
function useCall(callbacks = {}) {
  _react.default.useEffect(() => {
    // console.log('Initialising phone')
    const eventEmitter = new _reactNative.NativeEventEmitter(Sip);
    const eventListeners = Object.entries(callbacks).map(([event, callback]) => {
      return eventEmitter.addListener(event.slice(2), callback);
    });
    return () => eventListeners.forEach(listener => listener.remove());
  }, []);
}
async function call(remoteUri) {
  return Sip.outgoingCall(remoteUri);
}
async function hangup() {
  return Sip.hangUp();
}
async function sendDtmf(dtmf) {
  return Sip.sendDtmf(dtmf);
}
function useAudioDevices(callback) {
  const scanAudioDevices = _react.default.useCallback(() => Sip.scanAudioDevices().then(callback), [callback]);
  _react.default.useEffect(() => {
    const eventEmitter = new _reactNative.NativeEventEmitter(Sip);
    const deviceListener = eventEmitter.addListener('AudioDevicesChanged', scanAudioDevices);
    return () => deviceListener.remove();
  }, []);
  _react.default.useEffect(() => {
    scanAudioDevices();
  }, []);
}
async function setAudioDevice(device) {
  if (device === 'bluetooth') return Sip.bluetoothAudio();
  if (device === 'loudspeaker') return Sip.loudAudio();
  if (device === 'phone') return Sip.phoneAudio();
}
async function getMicStatus() {
  return Sip.getMicStatus();
}
async function toggleMute() {
  return Sip.toggleMute();
}
async function getAllCallsStatus() {
  return Sip.getAllCallsStatus();
}
async function getCallStatus() {
  return Sip.getCallStatus();
}
async function acceptCall() {
  return Sip.acceptCall();
}
async function rejectCall() {
  return Sip.rejectCall();
}
async function holdCall() {
  return Sip.holdCall();
}
async function unholdCall() {
  return Sip.unholdCall();
}
async function holdCallByIndex(callIndex) {
  return Sip.holdCallByIndex(callIndex);
}
async function unholdCallByIndex(callIndex) {
  return Sip.unholdCallByIndex(callIndex);
}
async function hangupCallByIndex(callIndex) {
  return Sip.hangupCallByIndex(callIndex);
}
async function blindTransferCall(callIndex, transferTo) {
  return Sip.blindTransferCall(callIndex, transferTo);
}
async function attendedTransferCall(fromCallIndex, toCallIndex) {
  return Sip.attendedTransferCall(fromCallIndex, toCallIndex);
}
async function getRegistrationStatus() {
  return Sip.getRegistrationStatus();
}
async function setAudioDeviceByName(device) {
  return Sip.setAudioDevice(device);
}
async function setUserAgent(appName, version) {
  return Sip.setUserAgent(appName, version);
}
async function setCallTimeout(timeoutSeconds) {
  return Sip.setCallTimeout(timeoutSeconds);
}
async function getCallTimeout() {
  return Sip.getCallTimeout();
}
//# sourceMappingURL=native-wrapper.js.map