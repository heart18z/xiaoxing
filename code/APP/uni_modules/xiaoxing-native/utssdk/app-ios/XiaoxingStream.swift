import Foundation

/// Incremental, cancellable SSE observation. Callback is deliberately multi-shot.
public final class XiaoxingStream: NSObject, URLSessionDataDelegate, URLSessionTaskDelegate {
    private static var watches: [String: XiaoxingStream] = [:] // main queue only
    private let id: String
    private static var eventListener: ((String) -> Void)?
    private var session: URLSession?
    private var task: URLSessionDataTask?
    private var pending = Data()
    private var cancelled = false
    private init(_ id: String) { self.id = id }
    public static func listen(_ callback: @escaping (String) -> Void) {
        DispatchQueue.main.async { eventListener = callback }
    }
    public static func call(_ action: String, _ json: String, _ callback: @escaping (String) -> Void) {
        DispatchQueue.main.async {
            let input = (try? JSONSerialization.jsonObject(with: Data(json.utf8))) as? [String: Any] ?? [:]
            let id = input["id"] as? String ?? ""
            if action == "stream.cancel" { watches.removeValue(forKey: id)?.cancel(); callback("{}"); return }
            guard !id.isEmpty, let raw = input["url"] as? String, let url = URL(string: raw), url.scheme == "https" else {
                callback("{\"error\":\"流式地址无效\"}"); return
            }
            watches.removeValue(forKey: id)?.cancel()
            guard eventListener != nil else { callback("{\"error\":\"流式监听尚未就绪\"}"); return }
            let watch = XiaoxingStream(id); watches[id] = watch
            var request = URLRequest(url: url); request.httpMethod = "POST"
            for (name, value) in input["headers"] as? [String: String] ?? [:] { request.setValue(value, forHTTPHeaderField: name) }
            request.setValue("identity", forHTTPHeaderField: "Accept-Encoding")
            request.httpBody = (input["body"] as? String ?? "{}").data(using: .utf8)
            let config = URLSessionConfiguration.ephemeral
            config.timeoutIntervalForRequest = 35; config.timeoutIntervalForResource = 1800
            config.requestCachePolicy = .reloadIgnoringLocalCacheData
            watch.session = URLSession(configuration: config, delegate: watch, delegateQueue: .main)
            watch.task = watch.session?.dataTask(with: request); watch.task?.resume()
            callback("{}") // One-shot UTS method callback; all data uses the persistent listener.
        }
    }
    private func emit(_ value: [String: Any]) {
        var event = value; event["id"] = id
        guard !cancelled, let data = try? JSONSerialization.data(withJSONObject: event), let text = String(data: data, encoding: .utf8) else { return }
        XiaoxingStream.eventListener?(text)
    }
    private func cancel() {
        cancelled = true; pending.removeAll(); task?.cancel(); session?.invalidateAndCancel(); task = nil; session = nil
    }
    public func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive response: URLResponse, completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        guard let response = response as? HTTPURLResponse, (200..<300).contains(response.statusCode), response.mimeType == "text/event-stream" else {
            emit(["error": "流式连接暂不可用，正在查询结果"]); completionHandler(.cancel); return
        }
        completionHandler(.allow)
    }
    public func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        guard !cancelled else { return }
        pending.append(data)
        // Newline is an ASCII boundary, so a partial Chinese/emoji character stays buffered.
        if let last = pending.lastIndex(of: 10) {
            let end = pending.index(after: last)
            let chunk = Data(pending[..<end]); pending = Data(pending[end...])
            if let text = String(data: chunk, encoding: .utf8) { emit(["text": text]) }
            else { emit(["error": "流式文本编码错误"]); cancel() }
        }
        if pending.count > 2 * 1024 * 1024 { emit(["error": "流式响应过大"]); cancel() }
    }
    public func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
    public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if !cancelled { emit(error == nil ? ["ended": true] : ["error": "流式连接中断，正在查询结果"]) }
        if XiaoxingStream.watches[id] === self { XiaoxingStream.watches.removeValue(forKey: id) }
        cancel()
    }
}
