import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

func color(_ hex: UInt32, _ alpha: CGFloat = 1) -> CGColor {
    CGColor(srgbRed: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: alpha)
}
let space = CGColorSpace(name: CGColorSpace.sRGB)!

/// XM monogram in a 1000x1000 space (y up): white X, gradient M whose right leg is a note.
func drawMonogram(_ ctx: CGContext) {
    // X
    ctx.setStrokeColor(color(0xF4F3FA))
    ctx.setLineWidth(96)
    ctx.setLineCap(.butt)
    ctx.move(to: CGPoint(x: 150, y: 300)); ctx.addLine(to: CGPoint(x: 420, y: 700))
    ctx.move(to: CGPoint(x: 150, y: 700)); ctx.addLine(to: CGPoint(x: 420, y: 300))
    ctx.strokePath()

    // M + note: separate solid pieces, then the gradient painted over them only.
    ctx.beginTransparencyLayer(auxiliaryInfo: nil)
    ctx.setFillColor(color(0xFFFFFF))
    let m = CGMutablePath()
    m.move(to: CGPoint(x: 505, y: 300))
    m.addLine(to: CGPoint(x: 505, y: 700))
    m.addLine(to: CGPoint(x: 640, y: 500))
    m.addLine(to: CGPoint(x: 775, y: 700))
    // Flat top at the X's height, instead of the miter's spike.
    ctx.saveGState()
    ctx.clip(to: CGRect(x: 0, y: 0, width: 1000, height: 733))
    ctx.addPath(m.copy(strokingWithWidth: 92, lineCap: .butt, lineJoin: .miter, miterLimit: 10))
    ctx.fillPath()
    ctx.restoreGState()
    // Right leg doubles as the note's stem, rising above the letters.
    ctx.fill(CGRect(x: 729, y: 330, width: 92, height: 470))
    // Flag
    let flag = CGMutablePath()
    flag.move(to: CGPoint(x: 805, y: 785))
    flag.addCurve(to: CGPoint(x: 885, y: 600), control1: CGPoint(x: 845, y: 730), control2: CGPoint(x: 935, y: 690))
    ctx.addPath(flag.copy(strokingWithWidth: 56, lineCap: .round, lineJoin: .round, miterLimit: 10))
    ctx.fillPath()
    // Note head at the foot of the stem.
    ctx.saveGState()
    ctx.translateBy(x: 708, y: 322)
    ctx.rotate(by: 0.42)
    ctx.fillEllipse(in: CGRect(x: -100, y: -70, width: 200, height: 140))
    ctx.restoreGState()
    ctx.setBlendMode(.sourceIn)
    let gradient = CGGradient(colorsSpace: space, colors: [color(0xB46CFF), color(0x6D5BFF), color(0x3D7BFF)] as CFArray,
                              locations: [0, 0.5, 1])!
    ctx.drawLinearGradient(gradient, start: CGPoint(x: 480, y: 820), end: CGPoint(x: 900, y: 240),
                           options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
    ctx.endTransparencyLayer()
}

func render(size: Int, opaque: Bool, scale: CGFloat = 1, draw: (CGContext) -> Void) -> CGImage {
    let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0, space: space,
                        bitmapInfo: opaque ? CGImageAlphaInfo.noneSkipLast.rawValue : CGImageAlphaInfo.premultipliedLast.rawValue)!
    ctx.scaleBy(x: CGFloat(size) / 1000, y: CGFloat(size) / 1000)
    draw(ctx)
    return ctx.makeImage()!
}

func write(_ image: CGImage, _ path: String) {
    let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: path) as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(dest, image, nil)
    CGImageDestinationFinalize(dest)
}

let out = CommandLine.arguments[1]
let icon = render(size: 1024, opaque: true) { ctx in
    let bg = CGGradient(colorsSpace: space, colors: [color(0x1A1830), color(0x08070F)] as CFArray, locations: [0, 1])!
    ctx.drawRadialGradient(bg, startCenter: CGPoint(x: 500, y: 620), startRadius: 0,
                           endCenter: CGPoint(x: 500, y: 500), endRadius: 760, options: [.drawsAfterEndLocation])
    // Monogram scaled into the icon's safe area.
    ctx.translateBy(x: 500, y: 500); ctx.scaleBy(x: 0.78, y: 0.78); ctx.translateBy(x: -515, y: -540)
    drawMonogram(ctx)
}
write(icon, "\(out)/AppIcon-1024.png")
for s in 1...3 {
    write(render(size: 160 * s, opaque: false) { ctx in
        ctx.translateBy(x: 500, y: 500); ctx.scaleBy(x: 0.92, y: 0.92); ctx.translateBy(x: -515, y: -540)
        drawMonogram(ctx)
    }, "\(out)/LaunchLogo@\(s)x.png")
}
print("ok")
