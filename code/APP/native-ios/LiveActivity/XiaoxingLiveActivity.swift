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
                .activityBackgroundTint(Color(red:0.96,green:0.97,blue:1))
                .activitySystemActionForegroundColor(.blue)
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

private struct ReminderCard: View {
    let state:XiaoxingReminderAttributes.ContentState
    let stale:Bool
    var body:some View {
        VStack(alignment:.leading,spacing:10) {
            HStack {
                Label(state.language=="en-us" ? "Xiaoxing reminder" : "小醒提醒",systemImage:"calendar.badge.clock")
                    .font(.caption.weight(.semibold)).foregroundColor(.blue)
                Spacer()
                Text(state.kind=="DEADLINE" ? (state.language=="en-us" ? "Due soon" : "即将截止") : (state.language=="en-us" ? "Upcoming" : "即将到来"))
                    .font(.caption).foregroundColor(.secondary)
            }
            Text(state.title).font(.headline).foregroundColor(.primary).lineLimit(2).privacySensitive()
            HStack {
                Text(Date(timeIntervalSince1970:TimeInterval(state.targetAt)),style:.time).font(.subheadline).foregroundColor(.secondary)
                Spacer()
                ReminderTime(state:state,stale:stale).font(.title3.weight(.semibold).monospacedDigit()).foregroundColor(.blue)
            }
        }.padding(16)
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
            Text(timerInterval:Date(timeIntervalSince1970:TimeInterval(state.targetAt-3600))...target,countsDown:true)
                .multilineTextAlignment(.trailing)
        }
    }
}
