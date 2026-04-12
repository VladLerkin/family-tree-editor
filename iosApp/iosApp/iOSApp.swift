import SwiftUI
import FamilyTreeApp

@main
struct iOSApp: App {
    init() {
        // We use the top-level setupKoin() function defined in main.kt
        MainKt.setupKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
