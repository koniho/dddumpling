import XCTest

final class SmokeTests: XCTestCase {
    private func launch(_ scene: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["DDD_GAME_CENTER_DISABLED"] = "1"
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
        app.buttons["gameServices"].tap()
        let services = app.alerts["DDD Dev services"]
        XCTAssertTrue(services.waitForExistence(timeout: 5))
        XCTAssertTrue(services.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "Game Center: Disabled")).firstMatch.exists)
        services.buttons["Done"].tap()
        capture(app, "title")
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.85)).tap()
        let starting = NSPredicate(format: "value CONTAINS %@ OR value CONTAINS %@", "starter=true", "state=1")
        expectation(for: starting, evaluatedWith: game)
        waitForExpectations(timeout: 5)
        if (game.value as? String ?? "").contains("starter=true") {
            let ready = NSPredicate(format: "value CONTAINS %@", "starterReady=true")
            expectation(for: ready, evaluatedWith: game)
            waitForExpectations(timeout: 5)
            capture(app, "starter-choice")
            game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.48)).tap()
            let confirm = NSPredicate(format: "value CONTAINS %@", "starterConfirm=true")
            expectation(for: confirm, evaluatedWith: game)
            waitForExpectations(timeout: 5)
            capture(app, "starter-preview")
            game.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.68)).tap()
        }
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
}
