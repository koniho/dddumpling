import XCTest

final class LiveAnalyticsTests: XCTestCase {
    // Run explicitly with the owner's configuration; ordinary CI never sends live events.
    func testConfiguredReleaseConsentAndGameplay() throws {
        #if DDD_LIVE_ANALYTICS_TEST
        let app = XCUIApplication()
        app.launchArguments = ["-FIRDebugEnabled", "-ddd.analytics.consent.v1", "NO"]
        app.launch()
        XCTAssertTrue(app.buttons["privacy"].waitForExistence(timeout: 15))
        app.buttons["privacy"].tap()
        XCTAssertTrue(app.buttons["analytics-decline"].waitForExistence(timeout: 5))
        app.buttons["analytics-decline"].tap()
        XCTAssertFalse(app.buttons["analytics-allow"].exists)
        app.buttons["privacy"].tap()
        app.buttons["analytics-allow"].tap()
        let game = app.otherElements["game"]
        XCTAssertTrue(game.waitForExistence(timeout: 5))
        game.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.85)).tap()
        XCTAssertTrue(app.buttons["pause"].waitForExistence(timeout: 5))
        // DebugView uploads are batched; keep the real app foregrounded for one batch.
        let batch = expectation(description: "Firebase debug upload interval")
        DispatchQueue.main.asyncAfter(deadline: .now() + 15) { batch.fulfill() }
        wait(for: [batch], timeout: 20)
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "configured-release-gameplay"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        app.terminate()
        // The launch override keeps the next session off; explicitly persist withdrawal too.
        app.launch()
        XCTAssertTrue(app.buttons["privacy"].waitForExistence(timeout: 15))
        app.buttons["privacy"].tap()
        app.buttons["analytics-decline"].tap()
        app.terminate()
        #else
        throw XCTSkip("Live analytics verification requires an explicit Release test invocation.")
        #endif
    }
}
