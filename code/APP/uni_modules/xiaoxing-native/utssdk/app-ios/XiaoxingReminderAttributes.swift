import Foundation
import ActivityKit

// This SAME source file is compiled into the host plugin and the Widget Extension.
@available(iOS 16.2, *)
struct XiaoxingReminderAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        let key: String
        let owner: String
        let eventId: String
        let branchId: String
        let title: String
        let kind: String
        let phase: String
        let targetAt: Int64
        let endAt: Int64
        let language: String
    }
    let owner: String
    let key: String
    let eventId: String
}
