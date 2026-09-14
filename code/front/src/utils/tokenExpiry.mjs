// Cookie persistence follows the server's JWT expiry; it never extends authentication.
export function tokenCookieOptions(token) {
  try {
    const part=String(token).split('.')[1];
    const payload=JSON.parse(atob(part.replace(/-/g,'+').replace(/_/g,'/')));
    const seconds=payload.exp;
    if(typeof seconds==='number'&&Number.isFinite(seconds)&&seconds>0&&seconds<8640000000000)
      return {expires:new Date(seconds*1000),sameSite:'Lax'};
  } catch {}
  return {sameSite:'Lax'};
}
