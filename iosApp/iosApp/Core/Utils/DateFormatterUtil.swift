import Foundation

enum DateFormatterUtil {
    private static let isoFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    private static let fallbackIsoFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    private static let dateOnlyFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        return formatter
    }()

    private static let timeOnlyFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        return formatter
    }()

    static func parse(dateString: String) -> Date? {
        return isoFormatter.date(from: dateString) ?? fallbackIsoFormatter.date(from: dateString)
    }

    static func formatDate(dateString: String) -> String {
        guard let date = parse(dateString: dateString) else {
            return dateString
        }
        return dateOnlyFormatter.string(from: date)
    }

    static func formatTime(dateString: String) -> String {
        guard let date = parse(dateString: dateString) else {
            return dateString
        }
        return timeOnlyFormatter.string(from: date)
    }
}
