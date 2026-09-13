import XCTest

final class SmokeTests: XCTestCase {
    private func launch(_ scene: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        if let scene = scene { app.launchEnvironment["DDD_SCENE"] = scene }
        app.launch()
        XCTAssertTrue(app.otherElements["game"].waitForExistence(timeout: 15))
        return app
    }

    private func capture(_ app: XCUIApplication, _ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
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

    func testAnalyticsChoiceDoesNotBlockPlayAndPersists() {
        for choice in ["analytics-decline", "analytics-allow"] {
            let app = XCUIApplication()
            app.launchEnvironment["DDD_ANALYTICS_PREVIEW"] = "1"
            app.launchEnvironment["DDD_ANALYTICS_RESET"] = "1"
            app.launch()
            let prompt = app.buttons["analytics-allow"]
            XCTAssertTrue(prompt.waitForExistence(timeout: 15))
            capture(app, "analytics-choice")
            XCTAssertTrue(app.buttons["analytics-policy"].exists)
            app.buttons[choice].tap()
            let game = app.otherElements["game"]
            XCTAssertTrue(game.waitForExistence(timeout: 5))
            game.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.85)).tap()
            expectation(for: NSPredicate(format: "value CONTAINS %@", "state=1"), evaluatedWith: game)
            waitForExpectations(timeout: 5)
            app.terminate()
            app.launchEnvironment.removeValue(forKey: "DDD_ANALYTICS_RESET")
            app.launch()
            XCTAssertTrue(app.otherElements["game"].waitForExistence(timeout: 15))
            XCTAssertFalse(app.buttons["analytics-allow"].exists)
            app.buttons["privacy"].tap()
            XCTAssertTrue(app.buttons["analytics-decline"].waitForExistence(timeout: 5))
            if choice == "analytics-allow" {
                XCTAssertEqual(app.buttons["analytics-decline"].label, "TURN OFF")
                app.buttons["analytics-decline"].tap()
                app.buttons["privacy"].tap()
                XCTAssertEqual(app.buttons["analytics-allow"].label, "ALLOW ANALYTICS")
            }
            app.buttons["analytics-close"].tap()
            app.terminate()
        }
    }
}
