import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    /// Stage mode asks for the status bar and home indicator to go away (see HideSystemBars).
    @State private var systemBarsHidden = false

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .statusBarHidden(systemBarsHidden)
            .modifier(HomeIndicatorHidden(isHidden: systemBarsHidden))
            .onAppear {
                HideSystemBars_iosKt.observeSystemBarsHidden { hidden in
                    systemBarsHidden = hidden.boolValue
                }
            }
    }
}

/// The home indicator can only be hidden from iOS 16.
private struct HomeIndicatorHidden: ViewModifier {
    let isHidden: Bool

    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.persistentSystemOverlays(isHidden ? .hidden : .automatic)
        } else {
            content
        }
    }
}
