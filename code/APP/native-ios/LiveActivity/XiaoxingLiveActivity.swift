import SwiftUI
import WidgetKit
import ActivityKit

@main
struct XiaoxingLiveActivityBundle: WidgetBundle {
    var body: some Widget { XiaoxingLiveActivity() }
}

struct XiaoxingLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for:XiaoxingReminderAttributes.self) { context in
            ReminderCard(state:context.state,stale:context.isStale)
                .activityBackgroundTint(Color(red:0.88,green:0.94,blue:1).opacity(0.92))
                .activitySystemActionForegroundColor(ReminderPalette.ink)
                .widgetURL(link(context.attributes))
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label(context.state.language=="en-us" ? "Xiaoxing" : "小醒提醒",systemImage:"calendar.badge.clock")
                        .font(.caption.weight(.semibold)).foregroundColor(.cyan)
                }
                DynamicIslandExpandedRegion(.trailing) { ReminderTime(state:context.state,stale:context.isStale).font(.headline.monospacedDigit()) }
                DynamicIslandExpandedRegion(.bottom) { Text(context.state.title).font(.subheadline.weight(.medium)).lineLimit(2).padding(.top,4).privacySensitive() }
            } compactLeading: { Image(systemName:"calendar.badge.clock").foregroundColor(.cyan) }
              compactTrailing: { ReminderTime(state:context.state,stale:context.isStale).font(.caption.monospacedDigit()).frame(maxWidth:62) }
              minimal: { Image(systemName:"calendar.badge.clock").foregroundColor(.cyan) }
              .widgetURL(link(context.attributes)).keylineTint(.cyan)
        }
    }
    private func link(_ attributes:XiaoxingReminderAttributes)->URL? {
        var url=URLComponents();url.scheme="xiaoxing";url.host="live"
        url.queryItems=[URLQueryItem(name:"owner",value:attributes.owner),URLQueryItem(name:"eventId",value:attributes.eventId)]
        return url.url
    }
}

private enum ReminderPalette {
    static let ink = Color(red:0.07,green:0.13,blue:0.29)
    static let secondary = Color(red:0.34,green:0.40,blue:0.58)
    static let accent = Color(red:0.29,green:0.27,blue:0.95)
}

private struct ReminderCard: View {
    let state:XiaoxingReminderAttributes.ContentState
    let stale:Bool
    private var english:Bool { state.language == "en-us" }
    private var status:String {
        if stale { return english ? "Open to refresh" : "待更新" }
        if state.phase == "DUE" { return english ? "Due now" : "已到时间" }
        return state.kind == "DEADLINE" ? (english ? "Due soon" : "即将截止") : (english ? "Upcoming" : "即将提醒")
    }
    private var timeDescription:String {
        let date = Date(timeIntervalSince1970:TimeInterval(state.targetAt))
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier:english ? "en_US" : "zh_CN")
        formatter.dateFormat = "HH:mm"
        let time = formatter.string(from:date)
        let day:String
        if Calendar.current.isDateInToday(date) { day = english ? "Today" : "今天" }
        else if Calendar.current.isDateInTomorrow(date) { day = english ? "Tomorrow" : "明天" }
        else {
            formatter.dateFormat = english ? "MMM d" : "M月d日"
            day = formatter.string(from:date)
        }
        // The payload does not include the sender: don't label received tasks as self-created.
        let kind = state.kind == "DEADLINE" ? (english ? "Deadline" : "截止") : (english ? "Reminder" : "提醒")
        return "\(day) \(time) · \(kind)"
    }
    var body:some View {
        VStack(alignment:.leading,spacing:12) {
            HStack(spacing:7) {
                Image("live-activity-avatar",bundle:.main)
                    .resizable().scaledToFit().frame(width:28,height:28)
                    .clipShape(RoundedRectangle(cornerRadius:7,style:.continuous))
                    .accessibilityHidden(true)
                Text(english ? "AI Xiaoxing" : "AI小醒")
                    .font(.system(size:15,weight:.bold)).foregroundColor(ReminderPalette.ink)
                Spacer(minLength:6)
                HStack(spacing:5) {
                    Circle().fill(ReminderPalette.accent.opacity(0.65)).frame(width:7,height:7)
                    Text(status).font(.system(size:11,weight:.semibold)).lineLimit(1)
                }.foregroundColor(ReminderPalette.secondary)
            }
            HStack(alignment:.center,spacing:12) {
                VStack(alignment:.leading,spacing:5) {
                    Text(state.title).font(.system(size:19,weight:.bold))
                        .foregroundColor(ReminderPalette.ink).lineLimit(2)
                        .minimumScaleFactor(0.85).privacySensitive()
                    Text(timeDescription).font(.system(size:13,weight:.medium))
                        .foregroundColor(ReminderPalette.secondary).lineLimit(1).minimumScaleFactor(0.8)
                }.frame(maxWidth:.infinity,alignment:.leading)
                Rectangle().fill(ReminderPalette.accent.opacity(0.18)).frame(width:1,height:52)
                VStack(spacing:1) {
                    Text(stale ? (english ? "Status" : "当前状态") : state.kind == "DEADLINE" ? (english ? "Time until due" : "距离截止") : (english ? "Time remaining" : "距离提醒"))
                        .font(.system(size:10,weight:.semibold)).foregroundColor(ReminderPalette.secondary)
                    ReminderTime(state:state,stale:stale)
                        .font(.system(size:stale || state.phase == "DUE" ? 16 : 35,weight:.bold,design:.rounded).monospacedDigit())
                        .foregroundColor(ReminderPalette.accent).lineLimit(1).minimumScaleFactor(0.7)
                        .frame(height:40)
                    Text(stale ? (english ? "Tap to view" : "点击查看") : state.phase == "DUE" ? (english ? "View task" : "查看事项") : (english ? "min : sec" : "分 : 秒"))
                        .font(.system(size:10,weight:.medium)).foregroundColor(ReminderPalette.secondary)
                }.frame(width:100)
            }
        }.padding(.horizontal,16).padding(.vertical,14)
            .background(LinearGradient(colors:[Color.white.opacity(0.50),Color(red:0.76,green:0.85,blue:1).opacity(0.35),Color(red:0.79,green:0.77,blue:1).opacity(0.55)],startPoint:.topLeading,endPoint:.bottomTrailing))
            .clipShape(RoundedRectangle(cornerRadius:26,style:.continuous))
            .overlay(RoundedRectangle(cornerRadius:26,style:.continuous).strokeBorder(Color.white.opacity(0.70),lineWidth:1))
    }
}

private struct ReminderTime:View {
    let state:XiaoxingReminderAttributes.ContentState
    let stale:Bool
    var body:some View {
        let target=Date(timeIntervalSince1970:TimeInterval(state.targetAt))
        if stale {
            Text(state.language=="en-us" ? "View task" : "查看事项")
        } else if state.phase == "DUE" {
            Text(state.language=="en-us" ? "Due now" : "已到时间")
        } else {
            // System-driven countdown; no per-second server pushes, and it never counts upward after zero.
            Text(timerInterval:Date(timeIntervalSince1970:TimeInterval(state.targetAt-3600))...target,countsDown:true,showsHours:false)
                .multilineTextAlignment(.center)
        }
    }
}
