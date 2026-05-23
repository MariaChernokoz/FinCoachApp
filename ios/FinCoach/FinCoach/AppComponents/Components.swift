//
//  Components.swift
//  FinCoach
//
//  Created by Chernokoz on 11.02.2026.
//

import Foundation
import SwiftUI

public struct AppIcons {
    //Logo
    public static let logoLightGreen = Image("FinCoachLogoLightGreen")
    public static let logoWhite = Image("FinCoachLogoWhite")
    
    //TabBar
    public static let analyticsIcon = Image(systemName: "chart.bar.xaxis")
    public static let transactionsIcon = Image(systemName: "creditcard")
    public static let AIAssistantIcon = Image(systemName: "ellipsis.message")
    public static let goalsIcon = Image(systemName: "target")
    public static let settingsIcon = Image(systemName: "gearshape")
}

public struct AppColors {
    //Frames
    public static let whiteFrameColor = Color("whiteFrameColor")
    public static let lightGrayFrameColor = Color("lightGrayFrameColor")
    public static let darkGrayFrameColor = Color("darkGrayFrameColor")
    public static let blackFrameColor = Color("blackFrameColor")
    
    public static let lightGreenFrameColor = Color("lightGreenFrameColor")
    public static let darkGreenFrameColor = Color("darkGreenFrameColor")
    public static let purpleFrameColor = Color("purpleFrameColor")
    
    public static let backgroundGray = Color(red: 246/255, green: 246/255, blue: 246/255)

    public static let linearGreenGradient: LinearGradient = {
        let gradient = LinearGradient(
            colors: [AppColors.lightGreenFrameColor, AppColors.darkGreenFrameColor],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        return gradient
    }()
    
    //Fonts
    public static let blackTextColor = Color("blackTextColor")
    public static let darkGrayTextColor = Color("darkGrayTextColor")
    public static let grayTextColor = Color("grayTextColor")
    public static let whiteTextColor = Color("whiteTextColor")
}

public struct AppFonts {
    //Fonts styles
}
