import Foundation

protocol AnalyticsService {
    func trackEvent(_ name: String, parameters: [String: Any]?)
    func setUserProperty(_ value: String, forName name: String)
}

class DefaultAnalyticsService: AnalyticsService {
    func trackEvent(_ name: String, parameters: [String: Any]? = nil) {
        // Placeholder for real analytics integration (e.g. Firebase, Mixpanel)
        print("📊 [Analytics] Event: \(name), Params: \(parameters ?? [:])")
    }

    func setUserProperty(_ value: String, forName name: String) {
        print("📊 [Analytics] User Property: \(name) = \(value)")
    }
}
