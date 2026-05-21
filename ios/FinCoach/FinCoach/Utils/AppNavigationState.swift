//
//  AppNavigationState.swift
//  FinCoach
//

import SwiftUI
import Combine

final class AppNavigationState: ObservableObject {
    @Published var selectedTab: Int = 0
    @Published var pendingAIMessage: String?

    func navigateToAICoach(with message: String? = nil) {
        pendingAIMessage = message
        selectedTab = 2
    }
}
