import Foundation
import OSLog

protocol CrashReportingService {
    func start()
    func recordError(_ error: Error)
    func setUserId(_ id: String)
    func log(_ message: String)
}

class DefaultCrashReportingService: CrashReportingService {
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "com.synapse.social", category: "CrashReporting")
    private let crashFileURL: URL
    private let queue = DispatchQueue(label: "com.synapse.social.crashreporting", qos: .background)
    private var userId: String?

    init() {
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        crashFileURL = documentsDirectory.appendingPathComponent("crash_reports.json")

        if !FileManager.default.fileExists(atPath: crashFileURL.path) {
            try? "[]".write(to: crashFileURL, atomically: true, encoding: .utf8)
        }
    }

    func start() {
        setupCrashHandler()
        logger.debug("💥 [CrashReporting] Started local crash reporter")
    }

    private func setupCrashHandler() {
        NSSetUncaughtExceptionHandler { exception in
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let crashFileURL = documentsDirectory.appendingPathComponent("crash_reports.json")

            let crashReport: [String: Any] = [
                "timestamp": ISO8601DateFormatter().string(from: Date()),
                "name": exception.name.rawValue,
                "reason": exception.reason ?? "Unknown",
                "callStackSymbols": exception.callStackSymbols
            ]

            // Note: Since this is happening during a crash, we must write synchronously and safely
            do {
                var events: [[String: Any]] = []
                if let data = try? Data(contentsOf: crashFileURL),
                   let existingEvents = try? JSONSerialization.jsonObject(with: data, options: []) as? [[String: Any]] {
                    events = existingEvents
                }

                events.append(crashReport)

                if let newData = try? JSONSerialization.data(withJSONObject: events, options: [.prettyPrinted]) {
                    try? newData.write(to: crashFileURL, options: .atomic)
                }
            }
        }
    }

    func recordError(_ error: Error) {
        logger.error("💥 [CrashReporting] Recorded Error: \(error.localizedDescription)")

        queue.async {
            let errorReport: [String: Any] = [
                "timestamp": ISO8601DateFormatter().string(from: Date()),
                "type": "error",
                "description": error.localizedDescription,
                "userId": self.userId ?? "unknown"
            ]
            self.appendReportToFile(errorReport)
        }
    }

    func setUserId(_ id: String) {
        logger.debug("💥 [CrashReporting] Set User ID: \(id)")
        queue.async {
            self.userId = id
        }
    }

    func log(_ message: String) {
        logger.debug("💥 [CrashReporting] Log: \(message)")
        queue.async {
            let logEntry: [String: Any] = [
                "timestamp": ISO8601DateFormatter().string(from: Date()),
                "type": "log",
                "message": message
            ]
            self.appendReportToFile(logEntry)
        }
    }

    private func appendReportToFile(_ report: [String: Any]) {
        do {
            let data = try Data(contentsOf: crashFileURL)
            var reports = try JSONSerialization.jsonObject(with: data, options: []) as? [[String: Any]] ?? []
            reports.append(report)

            let newData = try JSONSerialization.data(withJSONObject: reports, options: [.prettyPrinted])
            try newData.write(to: crashFileURL, options: .atomic)
        } catch {
            logger.error("Failed to write crash/error report: \(error.localizedDescription)")
        }
    }
}
