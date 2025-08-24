export declare enum TransportType {
    Udp = 0,
    Tcp = 1,
    Tls = 2,
    Dtls = 3
}
export interface InitializeOptions {
    userAgent?: string;
    platform?: string;
    version?: string;
    instanceId?: string;
    callTimeout?: number;
}
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
export declare function initialise(options?: InitializeOptions): Promise<void>;
export declare function unregister(): Promise<void>;
export declare function login(username: string, password: string, domain: string, transport: TransportType): Promise<void>;
export declare function useCall(callbacks?: Callbacks): void;
export declare function call(remoteUri: string): Promise<void>;
export declare function hangup(): Promise<void>;
export declare function sendDtmf(dtmf: string): Promise<void>;
export type AudioDevice = 'bluetooth' | 'phone' | 'loudspeaker';
export interface AudioDevices {
    options: {
        [device in AudioDevice]: boolean;
    };
    current: AudioDevice;
}
export declare function useAudioDevices(callback: (device: AudioDevices) => void): void;
export declare function setAudioDevice(device: AudioDevice): Promise<any>;
export declare function getMicStatus(): Promise<boolean>;
export declare function toggleMute(): Promise<void>;
export declare function getAllCallsStatus(): Promise<any>;
export declare function getCallStatus(): Promise<any>;
export declare function acceptCall(): Promise<void>;
export declare function rejectCall(): Promise<void>;
export declare function holdCall(): Promise<void>;
export declare function unholdCall(): Promise<void>;
export declare function holdCallByIndex(callIndex: number): Promise<void>;
export declare function unholdCallByIndex(callIndex: number): Promise<void>;
export declare function hangupCallByIndex(callIndex: number): Promise<void>;
export declare function blindTransferCall(callIndex: number, transferTo: string): Promise<void>;
export declare function attendedTransferCall(fromCallIndex: number, toCallIndex: number): Promise<void>;
export declare function getRegistrationStatus(): Promise<any>;
export declare function setAudioDeviceByName(device: string): Promise<void>;
export declare function setUserAgent(appName: string, version: string): Promise<void>;
export declare function setCallTimeout(timeoutSeconds: number): Promise<void>;
export declare function getCallTimeout(): Promise<number>;
export {};
//# sourceMappingURL=native-wrapper.d.ts.map