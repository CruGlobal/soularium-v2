//
//  iosAppApp.swift
//  iosApp
//
//  Created by Daniel Bisgrove on 5/20/26.
//

import SwiftUI

@main
struct iosAppApp: App {
    @UIApplicationDelegateAdaptor(FirebaseAppDelegate.self) var firebaseDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
