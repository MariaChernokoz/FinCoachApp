//
//  Bundle+Localization.swift
//  FinCoach
//

import Foundation

extension Bundle {
    // Returns the .lproj bundle matching the user's chosen app language.
    // Falls back to the main bundle (Russian text as the key is the natural fallback).
    static var appLocalized: Bundle {
        let language = UserDefaults.standard.string(forKey: "appLanguage") ?? "ru"
        if let path = Bundle.main.path(forResource: language, ofType: "lproj"),
           let bundle = Bundle(path: path) {
            return bundle
        }
        return Bundle.main
    }

    // Convenience: look up a key in the app-language bundle, with optional format args.
    static func L(_ key: String, _ args: CVarArg...) -> String {
        let format = NSLocalizedString(key, bundle: .appLocalized, comment: "")
        return args.isEmpty ? format : String(format: format, arguments: args)
    }
}
