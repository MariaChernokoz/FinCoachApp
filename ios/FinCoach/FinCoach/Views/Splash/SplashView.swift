import SwiftUI
import Lottie

struct SplashView: View {
    var onFinished: () -> Void

    var body: some View {
        ZStack {
            Color(red: 181 / 255, green: 231 / 255, blue: 49 / 255)
                .ignoresSafeArea()
            LottieWrapper(animationName: "upload", onFinish: onFinished)
        }
    }
}

private struct LottieWrapper: UIViewRepresentable {
    let animationName: String
    var onFinish: (() -> Void)?

    func makeUIView(context: Context) -> LottieAnimationView {
        let view = LottieAnimationView(name: animationName)
        view.contentMode = .scaleAspectFit
        view.loopMode = .playOnce
        view.backgroundColor = .clear
        view.play { finished in
            if finished { onFinish?() }
        }
        return view
    }

    func updateUIView(_ uiView: LottieAnimationView, context: Context) {}
}
