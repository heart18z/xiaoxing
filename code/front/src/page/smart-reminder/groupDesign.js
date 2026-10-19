export const groupAvatars = [
  '/avatars/group/01-campus.png',
  '/avatars/group/02-assessment.png',
  '/avatars/group/03-safety.png',
  '/avatars/group/04-reading.png',
  '/avatars/group/05-sports.png',
  '/avatars/group/06-notice.png',
  '/avatars/group/07-team.png',
  '/avatars/group/08-schedule.png',
  '/avatars/group/09-charity.png',
];
export function groupTagTone(name) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash + name.charCodeAt(i)) % 3;
  return 'tone-' + (hash + 1).toString();
}
