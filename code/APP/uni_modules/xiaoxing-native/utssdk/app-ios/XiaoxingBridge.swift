import Foundation
import AVFoundation
import Speech
import UIKit
import Security
import UserNotifications
import UniformTypeIdentifiers
import ObjectiveC
import SwiftUI
#if canImport(AlarmKit)
import AlarmKit
#endif

public final class XiaoxingCall: NSObject {
    let input: [String: Any]
    let callback: (String) -> Void
    var completed = false
    init(_ input: [String: Any], _ callback: @escaping (String) -> Void) { self.input=input; self.callback=callback }
    func getString(_ key: String) -> String? { input[key] as? String }
    func getDouble(_ key: String) -> Double? { (input[key] as? NSNumber)?.doubleValue }
    func resolve(_ value: [String: Any] = [:]) { finish(value) }
    func reject(_ message: String, _ code: String = "NATIVE_ERROR") { finish(["error":message,"code":code]) }
    private func finish(_ value: [String: Any]) {
        DispatchQueue.main.async { guard !self.completed else { return }; self.completed=true
            let data=(try? JSONSerialization.data(withJSONObject:value)) ?? Data("{}".utf8)
            self.callback(String(data:data,encoding:.utf8) ?? "{}")
        }
    }
}

public final class XiaoxingBridge: NSObject, UNUserNotificationCenterDelegate, UIDocumentPickerDelegate {
    static let shared = XiaoxingBridge()
    static let alarms = XiaoxingAlarms()
    static let speech = XiaoxingSpeech()
    private var pickerCall: XiaoxingCall?
    private var token = ""
    private var registrationError = ""
    private var tapped: [String: Any] = [:]
    private weak var previousNotificationDelegate: UNUserNotificationCenterDelegate?
    private var installed = false
    private static func keyQuery(_ key: String) -> [String:Any] {
        [kSecClass as String:kSecClassGenericPassword,kSecAttrService as String:"com.dfyj.xiaoxing.uniappx",kSecAttrAccount as String:key]
    }
    public static func read(_ key: String) -> String {
        var query=keyQuery(key);query[kSecReturnData as String]=true;query[kSecMatchLimit as String]=kSecMatchLimitOne
        var item:CFTypeRef?;let status=SecItemCopyMatching(query as CFDictionary,&item)
        guard status==errSecSuccess,let data=item as? Data else { return legacyValue(key) }
        return String(data:data,encoding:.utf8) ?? ""
    }
    // Keep the old installation proof across an in-place Capacitor upgrade.
    // Do not silently import the old login; the first new launch revokes its binding.
    private static func legacyValue(_ key: String) -> String {
        guard ["installation", "pushBinding", "revocations"].contains(key) else { return "" }
        let query: [String:Any] = [kSecClass as String:kSecClassGenericPassword,
            kSecAttrService as String:"com.dfyj.xiaoxing.session", kSecAttrAccount as String:"native-state-v1",
            kSecReturnData as String:true, kSecMatchLimit as String:kSecMatchLimitOne]
        var item:CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary,&item)==errSecSuccess,
            let data=item as? Data,
            let old=(try? JSONSerialization.jsonObject(with:data)) as? [String:Any] else { return "" }
        let value:Any
        if key == "revocations" { value=["items":old["pendingRevocations"] as? [[String:Any]] ?? []] }
        else { guard let saved=old[key] else { return "" }; value=saved }
        guard JSONSerialization.isValidJSONObject(value),
            let encoded=try? JSONSerialization.data(withJSONObject:value),
            let text=String(data:encoded,encoding:.utf8), write(key,text) else { return "" }
        return text
    }
    public static func write(_ key: String,_ value: String) -> Bool {
        guard let data=value.data(using:.utf8), data.count<=1048576 else { return false }
        let attributes:[String:Any]=[kSecValueData as String:data,kSecAttrAccessible as String:kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly]
        var status=SecItemUpdate(keyQuery(key) as CFDictionary,attributes as CFDictionary)
        if status==errSecItemNotFound {var query=keyQuery(key);attributes.forEach{query[$0.key]=$0.value};status=SecItemAdd(query as CFDictionary,nil)}
        return status==errSecSuccess
    }
    public static func random(_ count: Int) -> String {
        guard count>0 && count<=1024 else { return "" }
        var bytes=[UInt8](repeating:0,count:count)
        guard SecRandomCopyBytes(kSecRandomDefault,count,&bytes)==errSecSuccess else {return ""}
        return bytes.map{String(format:"%02x",$0)}.joined()
    }
    public static func call(_ action: String,_ json: String,_ callback: @escaping (String)->Void) {
        let input=(try? JSONSerialization.jsonObject(with:Data(json.utf8))) as? [String:Any] ?? [:]
        let call=XiaoxingCall(input,callback)
        DispatchQueue.main.async {
            switch action {
            case "initialize": shared.install(); call.resolve(["supported":true])
            case "push.status", "push.request": shared.push(call,request:action=="push.request")
            case "push.tap": let value=shared.tapped;shared.tapped=[:];call.resolve(value)
            case "push.clear": shared.tapped=[:];UNUserNotificationCenter.current().removeAllDeliveredNotifications();call.resolve()
            case "settings": guard let url=URL(string:UIApplication.openSettingsURLString) else {call.reject("无法打开设置");return};UIApplication.shared.open(url){ok in ok ? call.resolve() : call.reject("无法打开设置")}
            case "speech.start": speech.start(call)
            case "speech.stop": speech.stop(call)
            case "speech.poll": speech.poll(call)
            case "files.pick": shared.pick(call)
            case "alarm.activate": alarms.activate(call)
            case "alarm.list": alarms.list(call)
            case "alarm.schedule": alarms.schedule(call)
            case "alarm.cancel": alarms.cancel(call)
            default: call.reject("不支持的原生操作")
            }
        }
    }
    private func install() {
        guard !installed else{return};installed=true
        let center=UNUserNotificationCenter.current()
        previousNotificationDelegate=center.delegate;center.delegate=self
        // uni-app x does not expose UTSiOSHookProxy in all runtimes. Add delegate
        // handlers while preserving the runtime's original APNs callbacks.
        guard let delegate=UIApplication.shared.delegate,let cls:AnyClass=object_getClass(delegate) else{return}
        let success=NSSelectorFromString("application:didRegisterForRemoteNotificationsWithDeviceToken:")
        let oldSuccess=class_getInstanceMethod(cls,success).map{method_getImplementation($0)}
        typealias SuccessFn = @convention(c) (AnyObject,Selector,UIApplication,NSData)->Void
        let block:@convention(block)(AnyObject,UIApplication,NSData)->Void={target,app,data in
            self.token=(data as Data).map{String(format:"%02x",$0)}.joined();self.registrationError=""
            if let old=oldSuccess {unsafeBitCast(old,to:SuccessFn.self)(target,success,app,data)}
        }
        class_replaceMethod(cls,success,imp_implementationWithBlock(block),"v@:@@")
        let failure=NSSelectorFromString("application:didFailToRegisterForRemoteNotificationsWithError:")
        let oldFailure=class_getInstanceMethod(cls,failure).map{method_getImplementation($0)}
        typealias FailureFn = @convention(c) (AnyObject,Selector,UIApplication,NSError)->Void
        let failBlock:@convention(block)(AnyObject,UIApplication,NSError)->Void={target,app,error in
            self.registrationError="设备推送注册失败，请检查签名与网络"
            if let old=oldFailure {unsafeBitCast(old,to:FailureFn.self)(target,failure,app,error)}
        }
        class_replaceMethod(cls,failure,imp_implementationWithBlock(failBlock),"v@:@@")
    }
    private func push(_ call:XiaoxingCall,request:Bool) {
        install()
        let center=UNUserNotificationCenter.current()
        let report = {
            center.getNotificationSettings { settings in
                DispatchQueue.main.async {
                    let granted=settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
                    if granted {UIApplication.shared.registerForRemoteNotifications()}
                    call.resolve(["supported":true,"permission":granted ? "authorized" : settings.authorizationStatus == .denied ? "denied" : "prompt","token":self.token,"error":self.registrationError,"bundleId":Bundle.main.bundleIdentifier ?? "","environment":Bundle.main.object(forInfoDictionaryKey:"APNsEnvironment") as? String ?? "","appVersion":Bundle.main.object(forInfoDictionaryKey:"CFBundleShortVersionString") as? String ?? ""])
                }
            }
        }
        if request {center.requestAuthorization(options:[.alert,.sound,.badge]){_,_ in report()}}else{report()}
    }
    public func userNotificationCenter(_ center:UNUserNotificationCenter,willPresent notification:UNNotification,withCompletionHandler completionHandler:@escaping(UNNotificationPresentationOptions)->Void){
        if let old=previousNotificationDelegate,old.responds(to:#selector(userNotificationCenter(_:willPresent:withCompletionHandler:))) {old.userNotificationCenter?(center,willPresent:notification,withCompletionHandler:completionHandler)}else{completionHandler([.banner,.list,.sound])}
    }
    public func userNotificationCenter(_ center:UNUserNotificationCenter,didReceive response:UNNotificationResponse,withCompletionHandler completionHandler:@escaping()->Void){
        let info=response.notification.request.content.userInfo
        for key in ["eventId","messageId","recipientUserId"] { if let value=info[key] {tapped[key]="\(value)"} }
        if let old=previousNotificationDelegate,old.responds(to:#selector(userNotificationCenter(_:didReceive:withCompletionHandler:))) {old.userNotificationCenter?(center,didReceive:response,withCompletionHandler:completionHandler)}else{completionHandler()}
    }
    private func pick(_ call:XiaoxingCall){
        guard pickerCall==nil else{call.reject("文件选择正在进行");return}
        let picker=UIDocumentPickerViewController(forOpeningContentTypes:[.pdf,.plainText,.data],asCopy:true)
        picker.delegate=self;picker.allowsMultipleSelection=false
        var top=UIApplication.shared.connectedScenes.compactMap{$0 as? UIWindowScene}.flatMap{$0.windows}.first{$0.isKeyWindow}?.rootViewController
        while let presented=top?.presentedViewController{top=presented}
        guard let controller=top else{call.reject("无法打开文件选择器");return}
        pickerCall=call;controller.present(picker,animated:true)
    }
    public func documentPicker(_ controller:UIDocumentPickerViewController,didPickDocumentsAt urls:[URL]){
        defer{pickerCall=nil};guard let url=urls.first else{pickerCall?.resolve(["cancelled":true]);return}
        let access=url.startAccessingSecurityScopedResource();defer{if access{url.stopAccessingSecurityScopedResource()}}
        do{let size=(try url.resourceValues(forKeys:[.fileSizeKey])).fileSize ?? 0;guard size<=20*1024*1024 else{pickerCall?.reject("文件不能超过20MB");return};let target=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString+"-"+url.lastPathComponent);try FileManager.default.copyItem(at:url,to:target);pickerCall?.resolve(["path":target.path,"name":url.lastPathComponent,"size":size])}catch{pickerCall?.reject("文件读取失败")}
    }
    public func documentPickerWasCancelled(_ controller:UIDocumentPickerViewController){pickerCall?.resolve(["cancelled":true]);pickerCall=nil}
}

public final class XiaoxingAlarms: NSObject {
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
    private func rejectUnsupported(_ call: XiaoxingCall) {
        call.reject("系统闹铃需要 iOS 26 及以上，并安装支持闹铃的新版小醒", "UNSUPPORTED")
    }
    @objc func activate(_ call: XiaoxingCall) {
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
    @objc func list(_ call: XiaoxingCall) {
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
    @objc func schedule(_ call: XiaoxingCall) {
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
    @objc func cancel(_ call: XiaoxingCall) {
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

// Ported from the existing Capacitor speech implementation.
final class XiaoxingSpeech: NSObject {
    private var latest: [String: Any] = [:]
    func poll(_ call: XiaoxingCall) {
        guard call.getString("session") == latest["session"] as? String else { call.resolve(["finished":true]); return }
        var value = latest
        value["recording"] = capturing
        call.resolve(value)
    }
    private func notifyListeners(_ name: String, data: [String: Any]) {
        for (key, value) in data { latest[key == "error" ? "message" : key] = value }
        if name == "finished" { latest["finished"] = true }
    }
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

    override init() {
        super.init()
        for name in [UIApplication.didEnterBackgroundNotification, AVAudioSession.interruptionNotification] {
            observers.append(NotificationCenter.default.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                guard let self = self, self.session != nil else { return }
                self.finish(error: "语音输入已中断，已识别的文字仍保留在输入框中")
            })
        }
    }

    func start(_ call: XiaoxingCall) {
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
            self.latest = ["session": id, "text": "", "finished": false]
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

    func stop(_ call: XiaoxingCall) {
        DispatchQueue.main.async {
            guard self.session != nil, self.session == call.getString("session") else { call.resolve(); return }
            if (call.input["cancel"] as? Bool) == true { self.finish(cancelled: true) }
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
