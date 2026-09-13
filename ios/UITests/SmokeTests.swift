import XCTest

final class SmokeTests: XCTestCase {
    override func tearDown() {
        XCUIDevice.shared.orientation = .portrait
        super.tearDown()
    }

    private func launch(_ scene: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        if let scene = scene { app.launchEnvironment["DDD_SCENE"] = scene }
        app.launch()
        XCTAssertTrue(app.otherElements["game"].waitForExistence(timeout: 15))
        return app
    }

    private func capture(_ app: XCUIApplication, _ name: String) {
        // The screen capture includes the whole rotated window and its letterboxing.
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }

    func testTitleStartsGameAndBackgroundPauses() {
        let app = launch()
        let game = app.otherElements["game"]
        XCTAssertTrue((game.value as? String ?? "").contains("state=0"))
        capture(app, "title")
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.85)).tap()
        let playing = NSPredicate(format: "value CONTAINS %@", "state=1")
        expectation(for: playing, evaluatedWith: game)
        waitForExpectations(timeout: 5)
        capture(app, "play")
        app.buttons["pause"].tap()
        XCTAssertTrue((game.value as? String ?? "").contains("paused=true"))
        app.buttons["pause"].tap()
        XCTAssertTrue((game.value as? String ?? "").contains("paused=false"))
        XCUIDevice.shared.press(.home)
        app.activate()
        XCTAssertTrue((game.value as? String ?? "").contains("paused=true"))
        capture(app, "background-paused")
    }

    func testProductionScenesRender() {
        for scene in ["case", "stage:5", "stage:10", "stage:15", "stage:20", "stars", "steamer"] {
            let app = launch(scene)
            XCTAssertEqual(app.state, .runningForeground)
            capture(app, scene.replacingOccurrences(of: ":", with: "-"))
            app.terminate()
        }
    }

    func testBossAcceptsPinchAndSwipeWithoutCrashing() {
        let app = launch("stage:10")
        let game = app.otherElements["game"]
        game.pinch(withScale: 1.7, velocity: 1)
        game.swipeLeft()
        XCTAssertEqual(app.state, .runningForeground)
        capture(app, "divide-after-gestures")
    }

    func testIPadRotationKeepsPortraitCanvasAndAlignedControls() throws {
        guard UIDevice.current.userInterfaceIdiom == .pad else { throw XCTSkip("iPad window test") }
        XCUIDevice.shared.orientation = .portrait
        let app = launch("title")
        let game = app.otherElements["game"]
        let portrait = game.frame
        XCTAssertEqual(portrait.width / portrait.height, 390 / 800, accuracy: 0.01)
        capture(app, "ipad-title-portrait")
        XCUIDevice.shared.orientation = .landscapeLeft
        let rotated = NSPredicate { _, _ in game.frame.height < portrait.height - 50 }
        expectation(for: rotated, evaluatedWith: game)
        waitForExpectations(timeout: 10)
        XCTAssertEqual(game.frame.width / game.frame.height, 390 / 800, accuracy: 0.01)
        capture(app, "ipad-title-landscape")
        // A letterbox tap must not act on the game's title or modal menus.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: 0.85)).tap()
        XCTAssertTrue((game.value as? String ?? "").contains("state=0"))
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.85)).tap()
        expectation(for: NSPredicate(format: "value CONTAINS %@", "state=1"), evaluatedWith: game)
        waitForExpectations(timeout: 5)
        let pause = app.buttons["pause"]
        XCTAssertGreaterThanOrEqual(pause.frame.width, 44)
        XCTAssertLessThanOrEqual(pause.frame.maxY, game.frame.minY + 1)
        pause.tap()
        XCTAssertTrue((game.value as? String ?? "").contains("paused=true"))
        XCUIDevice.shared.orientation = .portraitUpsideDown
        expectation(for: NSPredicate { _, _ in game.frame.height > portrait.height - 2 }, evaluatedWith: game)
        waitForExpectations(timeout: 10)
        XCTAssertTrue((game.value as? String ?? "").contains("paused=true"))
        pause.tap()
        XCTAssertTrue((game.value as? String ?? "").contains("paused=false"))
        capture(app, "ipad-play-upside-down")
    }

    func testIPadLandscapeScenesRenderAndAcceptGestures() throws {
        guard UIDevice.current.userInterfaceIdiom == .pad else { throw XCTSkip("iPad scene test") }
        XCUIDevice.shared.orientation = .landscapeRight
        for scene in ["case", "stage:5", "stage:10", "stage:15", "stage:20", "stars", "steamer", "fling"] {
            let app = launch(scene)
            let game = app.otherElements["game"]
            XCTAssertEqual(game.frame.width / game.frame.height, 390 / 800, accuracy: 0.01)
            capture(app, "ipad-landscape-" + scene.replacingOccurrences(of: ":", with: "-"))
            if scene == "stage:10" { game.pinch(withScale: 1.7, velocity: 1) }
            game.coordinate(withNormalizedOffset: CGVector(dx: 0.4, dy: 0.5))
                .press(forDuration: 0.05, thenDragTo: game.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5)))
            XCTAssertEqual(app.state, .runningForeground)
            app.terminate()
        }
    }
}
