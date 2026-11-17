//
//  IosSpotifyRemoteBridge.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 17.11.2026.
//

import Foundation
import SpotifyiOS
import SharedLogic

final class IosSpotifyRemoteBridge: NSObject, IosSpotifyBridge, SPTAppRemoteDelegate, SPTAppRemotePlayerStateDelegate {

    static let shared = IosSpotifyRemoteBridge()

    private var appRemote: SPTAppRemote?
    private weak var kotlinClient: IosSpotifyRemoteClient?

    private var pendingConnectSuccess: (() -> Void)?
    private var pendingConnectFailure: ((String) -> Void)?

    var isConnected: Bool {
        appRemote?.isConnected ?? false
    }

    func register(with client: IosSpotifyRemoteClient) {
        self.kotlinClient = client
        client.attachBridge(swiftBridge: self)
    }

    @discardableResult
    func handleOpenURL(_ url: URL) -> Bool {
        guard let appRemote else { return false }
        let params = appRemote.authorizationParameters(from: url)

        if let token = params?[SPTAppRemoteAccessTokenKey] {
            appRemote.connectionParameters.accessToken = token
            appRemote.connect()
            return true
        } else if let errorDesc = params?[SPTAppRemoteErrorDescriptionKey] {
            pendingConnectFailure?(errorDesc)
            clearPendingConnect()
            return true
        }
        return false
    }

    func connect(
        clientId: String,
        redirectUri: String,
        onSuccess: @escaping () -> Void,
        onFailure: @escaping (String) -> Void
    ) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }

            if self.isConnected {
                onSuccess()
                return
            }

            guard let url = URL(string: redirectUri) else {
                onFailure("Некоректний redirectUri: \(redirectUri)")
                return
            }

            if self.appRemote == nil {
                let config = SPTConfiguration(clientID: clientId, redirectURL: url)
                let remote = SPTAppRemote(configuration: config, logLevel: .debug)
                remote.delegate = self
                self.appRemote = remote
            }

            self.pendingConnectSuccess = onSuccess
            self.pendingConnectFailure = onFailure

            if self.appRemote?.connectionParameters.accessToken != nil {
                self.appRemote?.connect()
            } else {
                self.appRemote?.authorizeAndPlayURI("")
            }
        }
    }

    func disconnect() {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            if self.appRemote?.isConnected == true {
                self.appRemote?.disconnect()
            }
            self.kotlinClient?.clearPlaybackState()
        }
    }

    func play(uri: String, onSuccess: @escaping () -> Void, onFailure: @escaping (String) -> Void) {
        DispatchQueue.main.async { [weak self] in
            self?.appRemote?.playerAPI?.play(uri, callback: self?.wrapCallback(onSuccess, onFailure))
        }
    }

    func queue(uri: String, onSuccess: @escaping () -> Void, onFailure: @escaping (String) -> Void) {
        DispatchQueue.main.async { [weak self] in
            self?.appRemote?.playerAPI?.enqueueTrackUri(uri, callback: self?.wrapCallback(onSuccess, onFailure))
        }
    }

    func pause(onSuccess: @escaping () -> Void, onFailure: @escaping (String) -> Void) {
        DispatchQueue.main.async { [weak self] in
            self?.appRemote?.playerAPI?.pause(self?.wrapCallback(onSuccess, onFailure))
        }
    }

    func pauseImmediate() {
        DispatchQueue.main.async { [weak self] in
            self?.appRemote?.playerAPI?.pause(nil)
        }
    }

    func resume(onSuccess: @escaping () -> Void, onFailure: @escaping (String) -> Void) {
        DispatchQueue.main.async { [weak self] in
            self?.appRemote?.playerAPI?.resume(self?.wrapCallback(onSuccess, onFailure))
        }
    }

    func skipNext(onSuccess: @escaping () -> Void, onFailure: @escaping (String) -> Void) {
        DispatchQueue.main.async { [weak self] in
            self?.appRemote?.playerAPI?.skip(toNext: self?.wrapCallback(onSuccess, onFailure))
        }
    }

    func seekTo(positionMs: Int64, onSuccess: @escaping () -> Void, onFailure: @escaping (String) -> Void) {
        DispatchQueue.main.async { [weak self] in
            self?.appRemote?.playerAPI?.seek(toPosition: Int(positionMs), callback: self?.wrapCallback(onSuccess, onFailure))
        }
    }


    func appRemoteDidEstablishConnection(_ appRemote: SPTAppRemote) {
        appRemote.playerAPI?.delegate = self
        appRemote.playerAPI?.subscribe(toPlayerState: nil)
        pendingConnectSuccess?()
        clearPendingConnect()
    }

    func appRemote(_ appRemote: SPTAppRemote, didFailConnectionAttemptWithError error: Error?) {
        let message = error?.localizedDescription ?? "Не вдалося підключитися до Spotify"
        pendingConnectFailure?(message)
        clearPendingConnect()
        kotlinClient?.clearPlaybackState()
    }

    func appRemote(_ appRemote: SPTAppRemote, didDisconnectWithError error: Error?) {
        kotlinClient?.clearPlaybackState()
    }

    func playerStateDidChange(_ playerState: SPTAppRemotePlayerState) {
        let track = playerState.track
        kotlinClient?.updatePlaybackState(
            trackUri: track.uri,
            title: track.name,
            artist: track.artist.name,
            isPaused: playerState.isPaused,
            positionMs: Int64(playerState.playbackPosition),
            durationMs: Int64(track.duration)
        )
    }

    private func wrapCallback(
        _ onSuccess: @escaping () -> Void,
        _ onFailure: @escaping (String) -> Void
    ) -> SPTAppRemoteCallback {
        return { _, error in
            if let error {
                onFailure(error.localizedDescription)
            } else {
                onSuccess()
            }
        }
    }

    private func clearPendingConnect() {
        pendingConnectSuccess = nil
        pendingConnectFailure = nil
    }
}
