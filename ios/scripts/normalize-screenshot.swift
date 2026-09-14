import Foundation
import ImageIO
import UniformTypeIdentifiers

// XCTest landscape PNGs can store portrait pixels with an EXIF rotation.
guard CommandLine.arguments.count == 3 else {
    fatalError("Usage: swift normalize-screenshot.swift input.png output.png")
}
let input = URL(fileURLWithPath: CommandLine.arguments[1])
let output = URL(fileURLWithPath: CommandLine.arguments[2])
guard input.standardizedFileURL != output.standardizedFileURL,
      let source = CGImageSourceCreateWithURL(input as CFURL, nil),
      let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
      let width = properties[kCGImagePropertyPixelWidth] as? Int,
      let height = properties[kCGImagePropertyPixelHeight] as? Int,
      let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
        kCGImageSourceCreateThumbnailFromImageAlways: true,
        kCGImageSourceCreateThumbnailWithTransform: true,
        kCGImageSourceThumbnailMaxPixelSize: max(width, height)
      ] as CFDictionary),
      let destination = CGImageDestinationCreateWithURL(output as CFURL, UTType.png.identifier as CFString, 1, nil)
else { fatalError("Cannot read screenshot or create a distinct output file") }
CGImageDestinationAddImage(destination, image, nil)
guard CGImageDestinationFinalize(destination) else { fatalError("Cannot write screenshot") }
print("Saved \(image.width) × \(image.height): \(output.path)")
