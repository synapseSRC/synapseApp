import Foundation

protocol CrashReportingService {
    func recordError(_ error: Error)
    func setUserId(_ id: String)
    func log(_ message: String)
}

class DefaultCrashReportingService: CrashReportingService {
    func recordError(_ error: Error) {
        // Placeholder for real crash reporting (e.g. Crashlytics)
        print("💥 [Crashlytics] Recorded Error: \(error.localizedDescription)")
    }

    func setUserId(_ id: String) {
        print("💥 [Crashlytics] Set User ID: \(id)")
    }

    func log(_ message: String) {
        print("💥 [Crashlytics] Log: \(message)")
    }
}
