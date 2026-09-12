/* 判断是否是内网IP */
export const isInnerIPFn = curPageUrl => {
  var reg1 = /(http|ftp|https|www):\/\//g;
  curPageUrl = curPageUrl.replace(reg1, '');
  var reg2 = /\:+/g;
  curPageUrl = curPageUrl.replace(reg2, '.');
  curPageUrl = curPageUrl.split('.');
  var ipAddress = curPageUrl[0] + '.' + curPageUrl[1] + '.' + curPageUrl[2] + '.' + curPageUrl[3];
  var isInnerIp = false;
  var ipNum = getIpNum(ipAddress);
  var aBegin = getIpNum('10.0.0.0');
  var aEnd = getIpNum('10.255.255.255');
  var bBegin = getIpNum('172.16.0.0');
  var bEnd = getIpNum('172.31.255.255');
  var cBegin = getIpNum('192.168.0.0');
  var cEnd = getIpNum('192.168.255.255');
  var dBegin = getIpNum('127.0.0.0');
  var dEnd = getIpNum('127.255.255.255');
  isInnerIp =
    isInner(ipNum, aBegin, aEnd) ||
    isInner(ipNum, bBegin, bEnd) ||
    isInner(ipNum, cBegin, cEnd) ||
    isInner(ipNum, dBegin, dEnd);
  return isInnerIp;
};

function getIpNum(ipAddress) {
  var ip = ipAddress.split('.');
  var a = parseInt(ip[0]);
  var b = parseInt(ip[1]);
  var c = parseInt(ip[2]);
  var d = parseInt(ip[3]);
  return a * 256 * 256 * 256 + b * 256 * 256 + c * 256 + d;
}

function isInner(userIp, begin, end) {
  return userIp >= begin && userIp <= end;
}
