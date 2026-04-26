import Foundation
import OSLog

protocol AnalyticsService {
    func trackEvent(_ name: String, parameters: [String: Any]?)
    func setUserProperty(_ value: String, forName name: String)
}

class DefaultAnalyticsService: AnalyticsService {
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "com.synapse.social", category: "Analytics")
    private let fileURL: URL
    private let queue = DispatchQueue(label: "com.synapse.social.analytics", qos: .background)

    init() {
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        fileURL = documentsDirectory.appendingPathComponent("analytics_events.json")

        if !FileManager.default.fileExists(atPath: fileURL.path) {
            try? "[]".write(to: fileURL, atomically: true, encoding: .utf8)
        }
    }

    func trackEvent(_ name: String, parameters: [String: Any]? = nil) {
        logger.debug("📊 [Analytics] Event: \(name), Params: \(parameters ?? [:], privacy: .private)")

        queue.async {
            let event: [String: Any] = [
                "timestamp": ISO8601DateFormatter().string(from: Date()),
                "name": name,
                "parameters": parameters ?? [:]
            ]

            self.appendEventToFile(event)
        }
    }

    func setUserProperty(_ value: String, forName name: String) {
        logger.debug("📊 [Analytics] User Property: \(name) = \(value, privacy: .private)")

        queue.async {
            let event: [String: Any] = [
                "timestamp": ISO8601DateFormatter().string(from: Date()),
                "type": "user_property",
                "name": name,
                "value": value
            ]

            self.appendEventToFile(event)
        }
    }

    private func appendEventToFile(_ event: [String: Any]) {
        do {
            let data = try Data(contentsOf: fileURL)
            var events = try JSONSerialization.jsonObject(with: data, options: []) as? [[String: Any]] ?? []
            events.append(event)

            let newData = try JSONSerialization.data(withJSONObject: events, options: [.prettyPrinted])
            try newData.write(to: fileURL, options: .atomic)
        } catch {
            logger.error("Failed to write analytics event: \(error.localizedDescription)")
        }
    }
}
