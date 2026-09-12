export function notificationRoute(data, userId) {
  if (!data || !userId || String(data.recipientUserId) !== String(userId)) return null;
  const eventId = String(data.eventId || '');
  return /^[1-9]\d{0,18}$/.test(eventId) ? `/app/event/${eventId}` : null;
}
