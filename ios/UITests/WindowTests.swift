import XCTest

final class WindowTests: XCTestCase {
    func testIPadWindowResizeKeepsControlsAligned() throws {
        guard UIDevice.current.userInterfaceIdiom == .pad else { throw XCTSkip("iPad only") }
        guard #available(iOS 26, *) else { throw XCTSkip("Requires iPadOS 26 windowing") }
        XCUIDevice.shared.orientation = .portrait
        let app = XCUIApplication()
        app.launchEnvironment["DDD_SCENE"] = "title"
        app.launch()
        XCTAssertTrue(app.otherElements["game"].waitForExistence(timeout: 15))
        let game = app.otherElements["game"]
        let window = app.windows.firstMatch
        if window.frame.width < UIScreen.main.bounds.width - 50 {
            window.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.02)).doubleTap()
            expectation(for: NSPredicate { _, _ in window.frame.width > UIScreen.main.bounds.width - 50 }, evaluatedWith: window)
            waitForExpectations(timeout: 10)
        }
        let originalWidth = window.frame.width
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.65, dy: 0.7))
        window.coordinate(withNormalizedOffset: CGVector(dx: 0.997, dy: 0.997)).press(forDuration: 0.2, thenDragTo: end)
        expectation(for: NSPredicate { _, _ in window.frame.width < originalWidth - 50 }, evaluatedWith: window)
        waitForExpectations(timeout: 10)
        XCTAssertEqual(game.frame.width / game.frame.height, 390 / 800, accuracy: 0.01)
        XCTAssertEqual(game.frame.midX, window.frame.midX, accuracy: 2)
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.85)).tap()
        expectation(for: NSPredicate(format: "value CONTAINS %@", "state=1"), evaluatedWith: game)
        waitForExpectations(timeout: 5)
        app.buttons["pause"].tap()
        XCTAssertTrue((game.value as? String ?? "").contains("paused=true"))
        XCTAssertGreaterThanOrEqual(app.buttons["pause"].frame.width, 44)
        XCTAssertLessThanOrEqual(app.buttons["pause"].frame.maxY, game.frame.minY + 1)
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = "ipad-window-resize"
        shot.lifetime = .keepAlways
        add(shot)
        window.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.02)).doubleTap()
        expectation(for: NSPredicate { _, _ in window.frame.width > originalWidth - 2 }, evaluatedWith: window)
        waitForExpectations(timeout: 10)
    }
}
