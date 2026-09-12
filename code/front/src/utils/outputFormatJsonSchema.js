/**
 * 「字段输出格式要求」：存完整 JsonSchema（JSON 文本）。
 * 转发时原样解析为对象放入 jsonSchema；如何拆分/组装由被调接口处理。
 */

export function parseValidatedOutputFormatJsonSchema(raw) {
  const s = (raw || '').trim();
  if (!s) {
    return { ok: false, message: '请输入「字段输出格式要求」（JsonSchema JSON 文本）' };
  }
  let parsed;
  try {
    parsed = JSON.parse(s);
  } catch (e) {
    return { ok: false, message: '「字段输出格式要求」须为合法 JSON' };
  }
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return { ok: false, message: '「字段输出格式要求」须为 JSON 对象' };
  }
  if (Object.keys(parsed).length === 0) {
    return { ok: false, message: '「字段输出格式要求」不能为空对象' };
  }
  return { ok: true, jsonSchema: parsed };
}

export function parseJsonArrayData(jsonData) {
  if (Array.isArray(jsonData)) {
    return { ok: true, data: jsonData };
  }
  if (typeof jsonData === 'string') {
    const s = jsonData.trim();
    if (!s) return { ok: false, message: 'jsonData 为空' };
    try {
      const parsed = JSON.parse(s);
      if (Array.isArray(parsed)) return { ok: true, data: parsed };
      return { ok: false, message: 'jsonData 不是数组 JSON' };
    } catch (e) {
      return { ok: false, message: 'jsonData 不是合法 JSON' };
    }
  }
  return { ok: false, message: 'jsonData 不是数组' };
}

function toPlainObject(v) {
  return v != null && typeof v === 'object' && !Array.isArray(v);
}

export function buildArrayTableColumnsFromJsonSchema(jsonSchema) {
  if (!toPlainObject(jsonSchema)) {
    return { ok: false, message: 'jsonSchema 不是对象' };
  }

  const pickFromItems = items => {
    if (!toPlainObject(items)) return null;
    const props = items.properties;
    if (!toPlainObject(props)) return null;
    const keys = Object.keys(props);
    if (!keys.length) return null;
    return keys.map(k => {
      const def = props[k];
      const desc =
        toPlainObject(def) && (def.description || def.title)
          ? String(def.description || def.title)
          : '';
      const label = desc ? `${k}（${desc}）` : k;
      return { prop: k, label };
    });
  };

  if (jsonSchema.type === 'array') {
    const cols = pickFromItems(jsonSchema.items);
    if (cols) return { ok: true, columns: cols };
  }

  if (toPlainObject(jsonSchema.properties)) {
    for (const k of Object.keys(jsonSchema.properties)) {
      const def = jsonSchema.properties[k];
      if (toPlainObject(def) && def.type === 'array') {
        const cols = pickFromItems(def.items);
        if (cols) return { ok: true, columns: cols };
      }
    }
  }

  return { ok: false, message: 'jsonSchema 未找到数组 items.properties，无法生成表头' };
}
