import XCTest

// Run explicitly with CONFIGURATION=Release; no scene hooks or seeded progress.
final class StoreCaptureTests: XCTestCase {
    func testIPadReleaseScreenshots() throws {
        guard UIDevice.current.userInterfaceIdiom == .pad else { throw XCTSkip("iPad captures") }
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        defer { XCUIDevice.shared.orientation = .portrait }
        let app = XCUIApplication()
        app.launch()
        let game = app.otherElements["game"]
        XCTAssertTrue(game.waitForExistence(timeout: 15))
        if (game.value as? String ?? "").contains("state=") { throw XCTSkip("Requires Release app") }
        let window = app.windows.firstMatch
        if window.frame.width < UIScreen.main.bounds.width - 50 {
            window.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.02)).doubleTap()
            expectation(for: NSPredicate { _, _ in window.frame.width > UIScreen.main.bounds.width - 2 }, evaluatedWith: window)
            waitForExpectations(timeout: 10)
        }
        XCTAssertEqual(game.frame.width / game.frame.height, 390 / 800, accuracy: 0.01)
        XCTAssertFalse(app.buttons["pause"].exists)
        capture("store-ipad-01-title")
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.85)).tap()
        XCTAssertTrue(app.buttons["pause"].waitForExistence(timeout: 5))
        // Let the start transition finish and the first words enter the playfield.
        Thread.sleep(forTimeInterval: 4)
        capture("store-ipad-02-gameplay")
        let portraitHeight = game.frame.height
        XCUIDevice.shared.orientation = .landscapeLeft
        expectation(for: NSPredicate { _, _ in game.frame.height < portraitHeight - 50 }, evaluatedWith: game)
        waitForExpectations(timeout: 10)
        XCTAssertEqual(game.frame.width / game.frame.height, 390 / 800, accuracy: 0.01)
        capture("store-ipad-03-landscape")
        app.terminate()
    }

    private func capture(_ name: String) {
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}
