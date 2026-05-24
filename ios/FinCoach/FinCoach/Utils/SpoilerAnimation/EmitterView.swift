import SwiftUI
import UIKit

final class EmitterView: UIView {
    override class var layerClass: AnyClass {
        CAEmitterLayer.self
    }

    override var layer: CAEmitterLayer {
        guard let emitterLayer = super.layer as? CAEmitterLayer else {
            fatalError("Expected CAEmitterLayer")
        }
        return emitterLayer
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        layer.emitterPosition = CGPoint(x: bounds.size.width / 2, y: bounds.size.height / 2)
        layer.emitterSize = bounds.size
    }
}
