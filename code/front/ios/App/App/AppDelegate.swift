import UIKit
import Capacitor
import Security
import Speech
import AVFoundation
#if canImport(AlarmKit)
import AlarmKit
import SwiftUI
#endif

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        NotificationCenter.default.post(name: .capacitorDidRegisterForRemoteNotifications, object: deviceToken)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NotificationCenter.default.post(name: .capacitorDidFailToRegisterForRemoteNotifications, object: error)
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }

    func application(_ application: UIApplication,
                     configurationForConnecting connectingSceneSession: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let config = UISceneConfiguration(name: "Default Configuration",
                                          sessionRole: connectingSceneSession.role)
        config.delegateClass = SceneDelegate.self
        return config
    }
}

// Kept in an existing Sources build entry so the generated Xcode project includes the bridge.
@objc(XiaoxingViewController)
class XiaoxingViewController: CAPBridgeViewController {
    override func capacitorDidLoad() {
        bridge?.registerPluginInstance(XiaoxingNativePlugin())
        bridge?.registerPluginInstance(XiaoxingSpeechPlugin())
        bridge?.registerPluginInstance(XiaoxingAlarmPlugin())
        // Native keyboard resizing can expose the hosting view/window behind WKWebView.
        let surface = UIColor(red: 244/255.0, green: 247/255.0, blue: 253/255.0, alpha: 1)
        view.backgroundColor = surface
        webView?.isOpaque = false
        webView?.backgroundColor = surface
        webView?.scrollView.backgroundColor = surface
    }
}

// Fixed-date AlarmKit alerts need no countdown widget. Older SDKs/devices fail explicitly.
@objc(XiaoxingAlarmPlugin)
public class XiaoxingAlarmPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "XiaoxingAlarmPlugin"
    public let jsName = "XiaoxingAlarm"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "activate", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "list", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "schedule", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "cancel", returnType: CAPPluginReturnPromise)
    ]
    private let recordsKey = "xiaoxing.alarm.records.v1"
    private let ownerKey = "xiaoxing.alarm.owner.v1"
    private var generation = 0
    private var scheduling = false
    private var records: [String: [String: Any]] {
        get { UserDefaults.standard.dictionary(forKey: recordsKey) as? [String: [String: Any]] ?? [:] }
        set { UserDefaults.standard.set(newValue, forKey: recordsKey) }
    }
    private var owner: String { UserDefaults.standard.string(forKey: ownerKey) ?? "" }
    private func valid(_ value: String) -> Bool {
        !value.isEmpty && value.count <= 20 && value.allSatisfy { $0.isASCII && $0.isNumber }
    }
    private func rejectUnsupported(_ call: CAPPluginCall) {
        call.reject("系统闹铃需要 iOS 26 及以上，并安装支持闹铃的新版小醒", "UNSUPPORTED")
    }
    @objc func activate(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let next = call.getString("owner") ?? ""
            guard next.isEmpty || self.valid(next) else { call.reject("账号信息无效"); return }
            #if canImport(AlarmKit)
            if #available(iOS 26.0, *) {
                do {
                    if next != self.owner {
                        self.generation += 1
                        if AlarmManager.shared.authorizationState == .authorized {
                            let alarms = try AlarmManager.shared.alarms
                            for alarm in alarms { try AlarmManager.shared.cancel(id: alarm.id) }
                        }
                        self.records = [:]
                        UserDefaults.standard.set(next, forKey: self.ownerKey)
                    }
                    call.resolve(["supported": true])
                } catch { call.reject("清理前一账号的闹铃失败，请重试", "CLEANUP_FAILED") }
                return
            }
            #endif
            call.resolve(["supported": false])
        }
    }
    @objc func list(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            #if canImport(AlarmKit)
            if #available(iOS 26.0, *) {
                guard self.owner == call.getString("owner"), !self.owner.isEmpty else { call.reject("请重新登录"); return }
                do {
                    let alarms = AlarmManager.shared.authorizationState == .authorized ? try AlarmManager.shared.alarms : []
                    let ids = Set(alarms.map { $0.id.uuidString })
                    // One-shot alarms disappear after dismissal. Keep no stale history indefinitely.
                    self.records = self.records.filter { ids.contains($0.value["id"] as? String ?? "") }
                    let items = self.records.map { key, value -> [String: Any] in
                        var item = value
                        item["eventId"] = key
                        item["active"] = ids.contains(value["id"] as? String ?? "")
                        return item
                    }
                    let permission: String
                    switch AlarmManager.shared.authorizationState {
                    case .authorized: permission = "authorized"
                    case .denied: permission = "denied"
                    case .notDetermined: permission = "prompt"
                    @unknown default: permission = "unknown"
                    }
                    call.resolve(["supported": true, "permission": permission, "alarms": items])
                } catch { call.reject("读取系统闹铃失败，请重试") }
                return
            }
            #endif
            call.resolve(["supported": false, "alarms": []])
        }
    }
    @objc func schedule(_ call: CAPPluginCall) {
        #if canImport(AlarmKit)
        if #available(iOS 26.0, *) {
            Task { @MainActor in
                guard !self.scheduling else { call.reject("正在设置另一条闹铃，请稍候"); return }
                let account = call.getString("owner") ?? ""
                let event = call.getString("eventId") ?? ""
                let title = String((call.getString("title") ?? "").prefix(100))
                guard self.valid(account), account == self.owner, self.valid(event),
                      let time = call.getDouble("timestamp"), time.isFinite,
                      time > Date().timeIntervalSince1970 + 2, !title.isEmpty else {
                    call.reject("闹铃账号、内容或时间无效，请选择未来时间"); return
                }
                self.scheduling = true
                defer { self.scheduling = false }
                let revision = self.generation
                do {
                    let permission = try await AlarmManager.shared.requestAuthorization()
                    guard permission == .authorized else { call.reject("未授权系统闹铃，请在 iOS 设置中允许小醒使用闹钟", "DENIED"); return }
                    guard revision == self.generation, self.owner == account else { call.reject("账号已变化，未设置闹铃"); return }
                    guard time > Date().timeIntervalSince1970 + 2 else { call.reject("授权期间闹铃时间已过，请重新选择未来时间"); return }
                    let current = self.records[event]
                    let existing = try AlarmManager.shared.alarms
                    if let savedID = current?["id"] as? String,
                       current?["timestamp"] as? Double == time, current?["title"] as? String == title,
                       existing.contains(where: { $0.id.uuidString == savedID }) {
                        call.resolve(["id": savedID, "timestamp": time]); return
                    }
                    if let savedID = current?["id"] as? String, let oldID = UUID(uuidString: savedID),
                       existing.contains(where: { $0.id == oldID }) { try AlarmManager.shared.cancel(id: oldID) }
                    let id = UUID()
                    let alert = AlarmPresentation.Alert(title: LocalizedStringResource(stringLiteral: title),
                        stopButton: AlarmButton(text: "停止", textColor: .white, systemImageName: "stop.circle"))
                    let attributes = AlarmAttributes(presentation: AlarmPresentation(alert: alert),
                        metadata: XiaoxingAlarmMetadata(), tintColor: Color.blue)
                    let configuration = AlarmManager.AlarmConfiguration.alarm(
                        schedule: .fixed(Date(timeIntervalSince1970: time)), attributes: attributes, sound: .default)
                    _ = try await AlarmManager.shared.schedule(id: id, configuration: configuration)
                    guard revision == self.generation, self.owner == account else {
                        try AlarmManager.shared.cancel(id: id)
                        call.reject("账号已变化，闹铃已撤销"); return
                    }
                    var records = self.records
                    records[event] = ["id": id.uuidString, "timestamp": time, "title": title]
                    self.records = records
                    call.resolve(["id": id.uuidString, "timestamp": time])
                } catch { call.reject("系统闹铃设置失败，请检查权限或稍后重试", "SCHEDULE_FAILED") }
            }
            return
        }
        #endif
        rejectUnsupported(call)
    }
    @objc func cancel(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            #if canImport(AlarmKit)
            if #available(iOS 26.0, *) {
                guard self.owner == call.getString("owner"), !self.owner.isEmpty,
                      let event = call.getString("eventId"), self.valid(event) else { call.reject("账号或事件无效"); return }
                self.generation += 1 // Invalidates a late scheduling result after explicit cancellation.
                do {
                    if let saved = self.records[event]?["id"] as? String, let id = UUID(uuidString: saved),
                       try AlarmManager.shared.alarms.contains(where: { $0.id == id }) {
                        try AlarmManager.shared.cancel(id: id)
                    }
                    var records = self.records; records.removeValue(forKey: event); self.records = records
                    call.resolve()
                } catch { call.reject("取消系统闹铃失败，请重试") }
                return
            }
            #endif
            self.rejectUnsupported(call)
        }
    }
}
#if canImport(AlarmKit)
@available(iOS 26.0, *)
private struct XiaoxingAlarmMetadata: AlarmMetadata {}
#endif

// Uses Apple's streaming recognition; audio is never sent to our chat or stored on disk.
@objc(XiaoxingSpeechPlugin)
public class XiaoxingSpeechPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "XiaoxingSpeechPlugin"
    public let jsName = "XiaoxingSpeech"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "start", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stop", returnType: CAPPluginReturnPromise)
    ]
    private var engine: AVAudioEngine?
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private var recognizer: SFSpeechRecognizer?
    private var session: String?
    private var transcript = ""
    private var tapped = false
    private var capturing = false
    private var deadline: Timer?
    private var observers: [NSObjectProtocol] = []

    public override func load() {
        for name in [UIApplication.didEnterBackgroundNotification, AVAudioSession.interruptionNotification] {
            observers.append(NotificationCenter.default.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                guard let self = self, self.session != nil else { return }
                self.finish(error: "语音输入已中断，已识别的文字仍保留在输入框中")
            })
        }
    }

    @objc func start(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard self.session == nil, let id = call.getString("session"), !id.isEmpty else {
                call.reject("录音正在进行，请先结束当前录音"); return
            }
            guard Bundle.main.object(forInfoDictionaryKey: "NSSpeechRecognitionUsageDescription") != nil,
                  Bundle.main.object(forInfoDictionaryKey: "NSMicrophoneUsageDescription") != nil else {
                call.reject("此安装包缺少语音权限说明，请更新App"); return
            }
            self.session = id
            self.transcript = ""
            SFSpeechRecognizer.requestAuthorization { status in
                DispatchQueue.main.async {
                    guard self.session == id else { call.reject("语音输入已取消"); return }
                    guard status == .authorized else {
                        self.finish(error: "请在系统设置中允许小醒使用语音识别"); call.reject("语音识别权限未开启"); return
                    }
                    AVAudioSession.sharedInstance().requestRecordPermission { granted in
                        DispatchQueue.main.async {
                            guard self.session == id else { call.reject("语音输入已取消"); return }
                            guard granted else {
                                self.finish(error: "请在系统设置中允许小醒使用麦克风"); call.reject("麦克风权限未开启"); return
                            }
                            do { try self.record(id: id); call.resolve() }
                            catch { self.finish(error: "无法开始语音识别，请检查麦克风、网络或稍后重试"); call.reject("无法开始语音识别") }
                        }
                    }
                }
            }
        }
    }

    private func record(id: String) throws {
        let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "zh-CN"))
        guard let recognizer = recognizer, recognizer.isAvailable else {
            throw NSError(domain: "XiaoxingSpeech", code: 1)
        }
        self.recognizer = recognizer
        let audio = AVAudioSession.sharedInstance()
        try audio.setCategory(.record, mode: .measurement, options: .duckOthers)
        try audio.setActive(true, options: .notifyOthersOnDeactivation)
        let engine = AVAudioEngine()
        self.engine = engine
        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        self.request = request
        let node = engine.inputNode
        let format = node.outputFormat(forBus: 0)
        guard format.sampleRate > 0, format.channelCount > 0 else { throw NSError(domain: "XiaoxingSpeech", code: 2) }
        var lastMeterTime: TimeInterval = 0
        node.installTap(onBus: 0, bufferSize: 1024, format: format) { [weak self] buffer, _ in
            request.append(buffer)
            let now = Date.timeIntervalSinceReferenceDate
            guard now - lastMeterTime >= 0.08, let samples = buffer.floatChannelData?[0], buffer.frameLength > 0 else { return }
            lastMeterTime = now
            var sum: Float = 0
            for index in 0..<Int(buffer.frameLength) { sum += samples[index] * samples[index] }
            let rms = sqrt(sum / Float(buffer.frameLength))
            let level = max(0, min(100, (20 * log10(max(rms, 0.00001)) + 60) / 60 * 100))
            DispatchQueue.main.async {
                guard let self = self, self.session == id, self.capturing else { return }
                self.notifyListeners("level", data: ["session": id, "level": Double(level)])
            }
        }
        tapped = true
        task = recognizer.recognitionTask(with: request) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self = self, self.session == id else { return }
                if let result = result {
                    self.transcript = result.bestTranscription.formattedString
                    self.notifyListeners("result", data: ["session": id, "text": self.transcript])
                }
                if result?.isFinal == true { self.finish() }
                else if error != nil { self.finish(error: self.transcript.isEmpty ? "未识别到语音，请检查网络、权限后重试" : "语音识别已结束，已识别文字已保留") }
            }
        }
        engine.prepare()
        try engine.start()
        capturing = true
        deadline = Timer.scheduledTimer(withTimeInterval: 60, repeats: false) { [weak self] _ in self?.endAudio() }
    }

    @objc func stop(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard self.session != nil, self.session == call.getString("session") else { call.resolve(); return }
            if call.getBool("cancel") == true { self.finish(cancelled: true) }
            else { self.endAudio() }
            call.resolve()
        }
    }
    private func stopCapture() {
        capturing = false
        engine?.stop()
        if tapped { engine?.inputNode.removeTap(onBus: 0); tapped = false }
        request?.endAudio()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
    private func endAudio() {
        stopCapture()
        deadline?.invalidate()
        deadline = Timer.scheduledTimer(withTimeInterval: 5, repeats: false) { [weak self] _ in self?.finish() }
    }
    private func finish(error: String? = nil, cancelled: Bool = false) {
        guard let id = session else { return }
        session = nil // Invalidate callbacks before cancelling the task.
        deadline?.invalidate(); deadline = nil
        stopCapture(); task?.cancel(); task = nil; engine = nil; request = nil; recognizer = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        notifyListeners("finished", data: ["session": id, "text": transcript,
            "error": error ?? (transcript.isEmpty && !cancelled ? "未识别到语音，请重试" : ""), "cancelled": cancelled])
    }
    deinit {
        deadline?.invalidate()
        engine?.stop()
        if tapped { engine?.inputNode.removeTap(onBus: 0) }
        task?.cancel()
        for observer in observers { NotificationCenter.default.removeObserver(observer) }
    }
}

@objc(XiaoxingNativePlugin)
public class XiaoxingNativePlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "XiaoxingNativePlugin"
    public let jsName = "XiaoxingNative"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "readState", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "writeState", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "context", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openSettings", returnType: CAPPluginReturnPromise)
    ]

    private var keychainQuery: [String: Any] {
        return [kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: "com.dfyj.xiaoxing.session",
                kSecAttrAccount as String: "native-state-v1"]
    }

    @objc func readState(_ call: CAPPluginCall) {
        var query = keychainQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        if status == errSecItemNotFound { call.resolve(["value": ""]); return }
        guard status == errSecSuccess, let data = item as? Data, let value = String(data: data, encoding: .utf8) else {
            call.reject("Secure storage is unavailable"); return
        }
        call.resolve(["value": value])
    }

    @objc func writeState(_ call: CAPPluginCall) {
        guard let value = call.getString("value"), let data = value.data(using: .utf8), data.count <= 524288,
              (try? JSONSerialization.jsonObject(with: data)) is [String: Any] else {
            call.reject("Invalid secure state"); return
        }
        let attributes: [String: Any] = [kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly]
        var status = SecItemUpdate(keychainQuery as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var query = keychainQuery
            for (key, value) in attributes { query[key] = value }
            status = SecItemAdd(query as CFDictionary, nil)
        }
        guard status == errSecSuccess else { call.reject("Secure storage write failed"); return }
        call.resolve()
    }

    @objc func context(_ call: CAPPluginCall) {
        guard let environment = Bundle.main.object(forInfoDictionaryKey: "APNsEnvironment") as? String,
              ["production", "sandbox"].contains(environment) else {
            call.reject("APNs build environment is missing"); return
        }
        call.resolve(["bundleId": Bundle.main.bundleIdentifier ?? "",
                      "environment": environment,
                      "appVersion": Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""])
    }

    @objc func openSettings(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard let url = URL(string: UIApplication.openSettingsURLString) else { call.reject("Settings unavailable"); return }
            UIApplication.shared.open(url, options: [:]) { opened in
                if opened { call.resolve() } else { call.reject("Settings unavailable") }
            }
        }
    }
}
