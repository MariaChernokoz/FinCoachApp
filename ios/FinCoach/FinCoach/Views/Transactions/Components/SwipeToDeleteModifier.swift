//
//  SwipeToDeleteModifier.swift
//  FinCoach
//
//  Created by Chernokoz on 08.05.2026.
//

import SwiftUI

struct SwipeToDeleteModifier: ViewModifier {
    let onDelete: () -> Void
    @State private var offset: CGFloat = 0
    private let threshold: CGFloat = 75

    func body(content: Content) -> some View {
        ZStack(alignment: .trailing) {
            Color.red
                .frame(width: max(0, -offset))
                .overlay(
                    Image(systemName: "trash")
                        .foregroundColor(.white)
                        .font(.system(size: 18, weight: .semibold))
                        .opacity(min(1, -offset / 30))
                        .padding(.trailing, 22),
                    alignment: .trailing
                )

            content
                .offset(x: offset)
                .gesture(
                    DragGesture(minimumDistance: 15, coordinateSpace: .local)
                        .onChanged { value in
                            guard value.translation.width < 0 else { return }
                            offset = max(value.translation.width, -threshold)
                        }
                        .onEnded { value in
                            if value.translation.width < -(threshold * 0.55) {
                                withAnimation(.easeIn(duration: 0.22)) { offset = -400 }
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) { onDelete() }
                            } else {
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) { offset = 0 }
                            }
                        }
                )
        }
        .clipped()
    }
}

extension View {
    func swipeToDelete(onDelete: @escaping () -> Void) -> some View {
        modifier(SwipeToDeleteModifier(onDelete: onDelete))
    }
}
