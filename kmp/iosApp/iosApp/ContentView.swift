import UIKit
import SwiftUI
import FamilyTreeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Kotlin top-level function defined in kmp/app-ios/src/iosMain/.../main.kt
        // File name: main.kt → Swift wrapper class: MainKt
        return MainKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
