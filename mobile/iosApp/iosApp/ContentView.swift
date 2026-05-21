//
//  ContentView.swift
//  iosApp
//
//  Hosts the Compose Multiplatform UI inside SwiftUI.
//

import SwiftUI
import ComposeApp

/// Bridges the Kotlin/Compose root view controller into SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // MainViewControllerKt.MainViewController() is defined in
        // composeApp/src/iosMain/.../MainViewController.kt — it starts Koin
        // and renders App().
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose handles insets and the keyboard itself.
    }
}
