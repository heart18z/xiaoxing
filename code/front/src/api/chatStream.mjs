// Result is the committed terminal frame. Do not wait for a proxy to close the socket.
export async function consumeChatStream(response, handlers={}) {
  if(!response.ok || !response.body) throw new Error(response.status===401?'登录已过期，请重新登录':`暂时无法发送消息（${response.status}）`);
  const reader=response.body.getReader(), decoder=new TextDecoder('utf-8');
  let buffer='',result,completed=false;
  const consume=block=>{
    if(completed)return;
    const json=block.split(/\r?\n/).filter(line=>line.startsWith('data:')).map(line=>line.slice(5)).join('');
    if(!json)return;
    const event=JSON.parse(json);
    if(event.type==='delta')handlers.onDelta?.(event.content||'');
    if(event.type==='reasoning')handlers.onReasoning?.(event.content||'');
    if(event.type==='snapshot')handlers.onSnapshot?.(event);
    if(event.type==='error')throw new Error(event.message||'AI服务暂时不可用');
    if(event.type==='result'){result=event.data;completed=true;handlers.onResult?.(result);}
  };
  try {
    while(!completed){
      const {value,done}=await reader.read();buffer+=decoder.decode(value||new Uint8Array(),{stream:!done});
      const blocks=buffer.split(/\r?\n\r?\n/);buffer=blocks.pop()||'';blocks.forEach(consume);
      if(done){if(buffer.trim())consume(buffer);break;}
    }
    if(!completed)throw new Error('连接已中断，操作结果待确认，请先查看事件或聊天记录，避免重复提交');
    return result;
  } finally {
    // Cancellation is best-effort; it must not delay the committed result.
    void reader.cancel().catch(()=>{});
    reader.releaseLock();
  }
}

export async function requestChatStream(send, refresh, signal) {
  let response=await send();
  // Retry only an HTTP 401 before any stream/actions began, never a failed mutation.
  if(response.status===401){
    void response.body?.cancel().catch(()=>{});
    try { await refresh(); } catch { throw new Error('登录已过期，请重新登录'); }
    if(signal?.aborted)throw new DOMException('Aborted','AbortError');
    response=await send();
  }
  return response;
}
