//
//  FinCoachApp.swift
//  FinCoach
//
//  Created by Chernokoz on 05.02.2026.
//

import SwiftUI
import FirebaseCore
import FirebaseFirestore

class AppDelegate: NSObject, UIApplicationDelegate {
  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
    FirebaseApp.configure()
    let settings = FirestoreSettings()
    settings.cacheSettings = PersistentCacheSettings()
    Firestore.firestore().settings = settings
    return true
  }
}

@main
struct FinCoachApp: App {
    // register app delegate for Firebase setup
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    var body: some Scene {
        WindowGroup {
            NavigationView {
                ContentView()
            }
        }
    }
}
