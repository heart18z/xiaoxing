import Foundation
import UIKit
import ActivityKit

@available(iOS 17.2, *)
@MainActor
final class XiaoxingLiveActivities {
    static let shared = XiaoxingLiveActivities()
    private var installed = false
    private var monitors: [String: [Task<Void, Never>]] = [:]
    private var pending: [String: [String: Any]] = [:]
    private var sent: [String: String] = [:]
    private var inFlight = Set<String>()
    private var generation = 0
    private var retry: Task<Void, Never>?
    private let storage = "liveActivityConfig"

    private func object(_ key: String) -> [String: Any] {
        (try? JSONSerialization.jsonObject(with: Data(XiaoxingBridge.read(key).utf8))) as? [String: Any] ?? [:]
    }
    private var extensionAvailable: Bool {
        guard let plugins = Bundle.main.builtInPlugInsURL else { return false }
        return FileManager.default.fileExists(atPath: plugins.appendingPathComponent("XiaoxingLiveActivityExtension.appex").path)
    }
    func install() {
        guard !installed else { return }; installed = true
        Task {
            for await token in Activity<XiaoxingReminderAttributes>.pushToStartTokenUpdates {
                self.enqueue("START", token: token)
            }
        }
        Task {
            for await activity in Activity<XiaoxingReminderAttributes>.activityUpdates { self.observe(activity) }
        }
        for activity in Activity<XiaoxingReminderAttributes>.activities { observe(activity) }
    }
    func configure(_ call: XiaoxingCall) {
        install()
        let input = call.input
        guard let owner = input["owner"] as? String, !owner.isEmpty,
              owner == object("session")["userId"] as? String,
              let base = input["baseUrl"] as? String, URL(string: base)?.scheme == "https",
              let binding = input["bindingId"] as? String, !binding.isEmpty else {
            call.reject("活动设备绑定无效"); return
        }
        let old = object(storage)
        if old["owner"] as? String != owner || old["bindingId"] as? String != binding {
            generation += 1; pending.removeAll(); sent.removeAll(); inFlight.removeAll()
            retry?.cancel(); retry = nil
        }
        guard let data = try? JSONSerialization.data(withJSONObject: input),
              let encoded = String(data: data, encoding: .utf8), XiaoxingBridge.write(storage, encoded) else {
            call.reject("活动配置保存失败"); return
        }
        let enabled = extensionAvailable && ActivityAuthorizationInfo().areActivitiesEnabled
        enqueue("ENABLED", enabled: enabled)
        if enabled, let token = Activity<XiaoxingReminderAttributes>.pushToStartToken { enqueue("START", token: token) }
        for activity in Activity<XiaoxingReminderAttributes>.activities {
            if activity.attributes.owner != owner || !enabled {
                Task { await activity.end(nil, dismissalPolicy: .immediate) }
            } else { observe(activity); if let token=activity.pushToken {enqueue("UPDATE",token:token,activity:activity)} }
        }
        flush()
        call.resolve(["supported": extensionAvailable, "enabled": enabled, "mode": enabled ? "activitykit" : "notification"])
    }
    func clear() {
        generation += 1
        pending.removeAll(); sent.removeAll(); inFlight.removeAll(); retry?.cancel(); retry=nil
        _ = XiaoxingBridge.write(storage, "")
        for activity in Activity<XiaoxingReminderAttributes>.activities {Task {await activity.end(nil,dismissalPolicy:.immediate)}}
    }
    private func observe(_ activity: Activity<XiaoxingReminderAttributes>) {
        guard monitors[activity.id] == nil else { return }
        let owner = object("session")["userId"] as? String ?? ""
        guard !owner.isEmpty, activity.attributes.owner == owner else {
            Task {await activity.end(nil,dismissalPolicy:.immediate)};return
        }
        if let token=activity.pushToken {enqueue("UPDATE",token:token,activity:activity)}
        let tokens = Task {
            for await token in activity.pushTokenUpdates {self.enqueue("UPDATE",token:token,activity:activity)}
        }
        let states = Task {
            for await state in activity.activityStateUpdates {
                if state == .dismissed || state == .ended {
                    self.enqueue("DISMISSED",activity:activity)
                    self.monitors.removeValue(forKey:activity.id)?.forEach {$0.cancel()}
                    return
                }
            }
        }
        monitors[activity.id] = [tokens,states]
    }
    private func enqueue(_ kind:String,token:Data?=nil,activity:Activity<XiaoxingReminderAttributes>?=nil,enabled:Bool=true) {
        let config=object(storage), owner=config["owner"] as? String ?? ""
        guard !owner.isEmpty, object("session")["userId"] as? String == owner else {return}
        if let activity=activity, activity.attributes.owner != owner {return}
        if kind == "START" && (!extensionAvailable || !ActivityAuthorizationInfo().areActivitiesEnabled) {return}
        let key=kind+":"+(activity?.attributes.key ?? "")
        var body:[String:Any]=["owner":owner,"kind":kind,"enabled":enabled]
        for field in ["installationId","installationSecret","bindingId"] {body[field]=config[field]}
        if let token=token {body["token"]=token.map {String(format:"%02x",$0)}.joined()}
        if let activity=activity {body["activityKey"]=activity.attributes.key}
        guard let data=try? JSONSerialization.data(withJSONObject:body,options:.sortedKeys) else {return}
        let fingerprint=String(data:data,encoding:.utf8) ?? ""
        if sent[key]==fingerprint {return}
        pending[key]=body
        flush()
    }
    private func flush() {
        let config=object(storage), epoch=generation
        guard let owner=config["owner"] as? String, object("session")["userId"] as? String == owner,
              let base=config["baseUrl"] as? String, let url=URL(string:base+"/app/push/live/token"),url.scheme=="https" else {return}
        for (key,body) in pending {
            if inFlight.contains(key) {continue}
            guard let data=try? JSONSerialization.data(withJSONObject:body,options:.sortedKeys) else {continue}
            inFlight.insert(key)
            var request=URLRequest(url:url);request.httpMethod="POST";request.httpBody=data;request.timeoutInterval=12
            request.setValue("application/json",forHTTPHeaderField:"Content-Type")
            request.setValue(config["basicAuth"] as? String,forHTTPHeaderField:"Authorization")
            request.setValue(config["tenant"] as? String,forHTTPHeaderField:"Tenant-Id")
            request.setValue("BladeHttpRequest",forHTTPHeaderField:"Blade-Requested-With")
            // The endpoint validates a revocable installation proof, so no OAuth token is sent/stored here.
            URLSession.shared.dataTask(with:request) {data,response,error in
                let http=response as? HTTPURLResponse
                let result=data.flatMap {try? JSONSerialization.jsonObject(with:$0)} as? [String:Any]
                let ok=error==nil && http?.statusCode==200 && (result?["code"] as? Int)==200
                Task {@MainActor in
                    guard self.generation==epoch else {return}
                    self.inFlight.remove(key)
                    if ok {
                        let fingerprint=(try? JSONSerialization.data(withJSONObject:body,options:.sortedKeys)).flatMap {String(data:$0,encoding:.utf8)} ?? ""
                        self.sent[key]=fingerprint
                        if let latest=self.pending[key], NSDictionary(dictionary:latest).isEqual(to:body) {self.pending.removeValue(forKey:key)}
                    }
                    self.scheduleRetry()
                }
            }.resume()
        }
    }
    private func scheduleRetry() {
        guard !pending.isEmpty, retry==nil else {return}
        retry=Task {
            do {try await Task.sleep(nanoseconds:30_000_000_000)} catch {return}
            self.retry=nil;self.flush()
        }
    }
}
