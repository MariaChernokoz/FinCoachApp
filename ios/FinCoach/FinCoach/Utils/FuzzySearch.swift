//
//  FuzzySearch.swift
//  FinCoach
//
//  Created by Chernokoz on 05.02.2026.
//

import Foundation

/// Levenshtein edit distance between two strings (space-optimised, O(m*n) time, O(n) space).
func levenshteinDistance(_ a: String, _ b: String) -> Int {
    let a = Array(a.unicodeScalars)
    let b = Array(b.unicodeScalars)
    let m = a.count, n = b.count

    if m == 0 { return n }
    if n == 0 { return m }

    var prev = Array(0...n)
    var curr = Array(repeating: 0, count: n + 1)

    for i in 1...m {
        curr[0] = i
        for j in 1...n {
            curr[j] = a[i-1] == b[j-1]
                ? prev[j-1]
                : 1 + min(prev[j], curr[j-1], prev[j-1])
        }
        swap(&prev, &curr)
    }
    return prev[n]
}

/// Returns true if `query` fuzzy-matches any word in `text`.
/// Falls back to substring check first (fast path), then checks each word with a tolerance that scales with query length.
func fuzzyMatches(query: String, in text: String) -> Bool {
    let q = query.lowercased()
    let t = text.lowercased()

    if t.contains(q) { return true }

    guard q.count >= 3 else { return false }

    let threshold: Int = q.count <= 4 ? 1 : 2

    return t.components(separatedBy: .whitespaces)
        .lazy
        .filter { !$0.isEmpty }
        .contains { levenshteinDistance(q, $0) <= threshold }
}
