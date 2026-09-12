<template>
  <div>
    <el-dialog
      v-model="aiChatVisible"
      width="720px"
      append-to-body
      :close-on-click-modal="false"
      custom-class="ai-chat-dialog-wrap"
      @closed="onAiChatClosed"
    >
      <div slot="title" class="ai-chat-title-row">
        <span class="ai-chat-title">AI 对话</span>
        <div class="ai-chat-title-actions">
          <el-tooltip effect="dark" content="查看本次弹窗会话日志" placement="top">
            <button
              type="button"
              class="ai-chat-icon-btn ai-chat-icon-btn--title"
              :disabled="!aiChatSessionId || !maitalkBaseUrl"
              @click.stop="openRequestLogDialog"
            >
              <i class="el-icon-document"></i>
            </button>
          </el-tooltip>
        </div>
      </div>
      <div class="ai-chat-body" v-loading="aiChatLoadingDetail">
        <div class="ai-chat-messages" ref="aiChatScroll">
          <div v-if="!aiChatMessages.length" class="ai-chat-empty">
            将根据按钮编码从后台加载该按钮的背景资料，输入问题后点击发送将一并提交。
          </div>
          <div
            v-for="(m, idx) in aiChatMessages"
            :key="idx"
            :class="[
              'ai-chat-bubble',
              m.role === 'user' ? 'is-user' : 'is-assistant',
              { 'has-structured': m.role === 'assistant' && m.structured && !m.loading }
            ]"
          >
            <div class="ai-chat-bubble-meta">{{ m.role === 'user' ? '我' : 'AI' }}</div>
            <template v-if="m.loading">
              <div class="ai-chat-bubble-text">
                <span class="ai-chat-typing-dot d1"></span>
                <span class="ai-chat-typing-dot d2"></span>
                <span class="ai-chat-typing-dot d3"></span>
              </div>
            </template>
            <template v-else-if="m.role === 'user'">
              <div class="ai-chat-bubble-text ai-chat-bubble-text--plain">{{ m.content }}</div>
            </template>
            <template v-else-if="m.structured">
              <div class="ai-chat-bubble-structured">
                <div class="ai-chat-bubble-text ai-chat-bubble-text--md">
                  <div class="ai-chat-md" v-html="m.displayHtml"></div>
                </div>
                <div class="ai-chat-bubble-actions">
                  <button
                    type="button"
                    class="ai-chat-icon-btn"
                    title="查看 jsonData"
                    @click.stop="openJsonDataPreview(m.structured.jsonData)"
                  >
                    <i class="el-icon-view"></i>
                  </button>
                  <button
                    type="button"
                    class="ai-chat-icon-btn"
                    title="表格查看（json 数组）"
                    @click.stop="openStructuredArrayTable(m.structured.jsonData)"
                  >
                    <i class="el-icon-tickets"></i>
                  </button>
                  <button
                    type="button"
                    class="ai-chat-icon-btn"
                    title="应用结构化数据"
                    @click.stop="applyStructuredJson(m.structured.jsonData)"
                  >
                    <i class="el-icon-check"></i>
                  </button>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="ai-chat-bubble-text ai-chat-bubble-text--md">
                <div class="ai-chat-md" v-html="m.displayHtml"></div>
              </div>
            </template>
          </div>
        </div>
        <div class="ai-chat-input-row">
          <el-input
            v-model="aiChatInput"
            type="textarea"
            :rows="3"
            resize="none"
            :placeholder="aiChatInputPlaceholder || defaultInputPlaceholder"
            @keydown.native.ctrl.enter="sendAiChat"
            @keydown.native.meta.enter="sendAiChat"
          />
          <el-button
            type="primary"
            class="ai-chat-send"
            icon="el-icon-position"
            circle
            :loading="aiChatSending"
            :disabled="aiChatSending || aiChatLoadingDetail"
            @click="sendAiChat"
          />
        </div>
      </div>
    </el-dialog>

    <el-dialog
      title="结构化数据（jsonData）"
      v-model="jsonPreviewVisible"
      width="560px"
      append-to-body
      :close-on-click-modal="false"
      custom-class="ai-json-preview-dialog"
    >
      <div class="ai-json-preview-toolbar">
        <el-button size="small" icon="el-icon-document-copy" @click="copyJsonPreview">复制</el-button>
      </div>
      <pre class="ai-json-preview">{{ jsonPreviewBody }}</pre>
      <template #footer><span class="dialog-footer">
        <el-button size="small" type="primary" @click="jsonPreviewVisible = false">关闭</el-button>
      </span></template>
    </el-dialog>

    <el-dialog
      title="本次会话日志"
      v-model="requestLogVisible"
      width="860px"
      append-to-body
      :close-on-click-modal="false"
      custom-class="ai-requestlog-dialog"
    >
      <div v-loading="requestLogLoading">
        <el-table
          :data="requestLogRecords"
          size="small"
          border
          style="width: 100%"
          @row-click="openLogRowDetail"
        >
          <el-table-column prop="dialogTime" label="对话时间" width="170" />
          <el-table-column prop="params" label="参数" min-width="260">
            <template #default="{  row  }">
              <el-tooltip effect="dark" :content="row.params" placement="top" :disabled="!row.params">
                <div class="ai-log-ellipsis">{{ row.params }}</div>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="resp" label="返回结果" min-width="260">
            <template #default="{  row  }">
              <el-tooltip effect="dark" :content="row.resp" placement="top" :disabled="!row.resp">
                <div class="ai-log-ellipsis">{{ row.resp }}</div>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>

        <div class="ai-log-pagination">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :total="requestLogPage.total"
            :page-size="requestLogPage.pageSize"
            :current-page="requestLogPage.currentPage"
            :page-sizes="[10, 20, 30, 50]"
            @current-change="onRequestLogCurrentChange"
            @size-change="onRequestLogSizeChange"
          />
        </div>
      </div>
      <template #footer><span class="dialog-footer">
        <el-button size="small" type="primary" @click="requestLogVisible = false">关闭</el-button>
      </span></template>
    </el-dialog>

    <el-dialog
      title="日志详情"
      v-model="logDetailVisible"
      width="860px"
      append-to-body
      :close-on-click-modal="false"
      custom-class="ai-logdetail-dialog"
    >
      <div class="ai-logdetail-meta">
        <div><b>对话时间</b>：{{ (logDetailRow || {}).dialogTime || '-' }}</div>
<!--        <div><b>URL</b>：{{ (logDetailRow || {}).url || '-' }}</div>-->
        <div><b>sessionId</b>：{{ (logDetailRow || {}).sessionId || '-' }}</div>
      </div>
      <el-tabs value="params" type="border-card">
        <el-tab-pane label="参数（params）" name="params">
          <pre class="ai-log-pre">{{ (logDetailRow || {}).params || '' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="返回结果（resp）" name="resp">
          <pre class="ai-log-pre">{{ (logDetailRow || {}).resp || '' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="错误信息" name="errorInfo">
          <pre class="ai-log-pre">{{ (logDetailRow || {}).errorInfo || '' }}</pre>
        </el-tab-pane>
      </el-tabs>
      <template #footer><span class="dialog-footer">
        <el-button size="small" type="primary" @click="logDetailVisible = false">关闭</el-button>
      </span></template>
    </el-dialog>

    <el-dialog
      title="JSON 数组表格"
      v-model="arrayTableVisible"
      width="860px"
      append-to-body
      :close-on-click-modal="false"
      custom-class="ai-arraytable-dialog"
    >
      <div class="ai-arraytable-tip" v-if="arrayTableTip">{{ arrayTableTip }}</div>
      <el-table
        v-loading="arrayTableLoading"
        :data="arrayTableRows"
        size="small"
        border
        style="width: 100%"
      >
        <el-table-column
          v-for="c in arrayTableColumns"
          :key="c.prop"
          :prop="c.prop"
          :label="c.label"
          min-width="140"
          show-overflow-tooltip
        >
          <template #default="{  row  }">
            <span>{{ formatCellValue(row[c.prop]) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer><span class="dialog-footer">
        <el-button size="small" type="primary" @click="arrayTableVisible = false">关闭</el-button>
      </span></template>
    </el-dialog>

  </div>
</template>

<script>
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { getDetail, aiTestProxy } from '@/views/aiBtn/api/btnConfig';
import request from '@/axios';
import {
  parseValidatedOutputFormatJsonSchema,
  parseJsonArrayData,
  buildArrayTableColumnsFromJsonSchema
} from '@/utils/outputFormatJsonSchema';

marked.setOptions({
  gfm: true,
  breaks: true
});

/**
 * AI 按钮对话弹窗。任意页面引入后调用：
 * this.$refs.xxx.open({ btnNo: '按钮编码' })
 * 或 this.$refs.xxx.open({ id: 配置主键 })（与后台 /btnConfig/detail 一致）
 * 返回内容固定为 {"jsonData":...,"textContent":"..."}；jsonSchema 为配置里完整 JsonSchema 解析后的对象，拆分由被调接口处理。
 */
export default {
  name: 'AiBtnChatDialog',
  data() {
    return {
      defaultInputPlaceholder: '请输入要向 AI 提出的问题…',
      aiChatInputPlaceholder: '',
      aiChatVisible: false,
      aiChatLoadingDetail: false,
      aiChatSending: false,
      aiChatInput: '',
      aiChatMessages: [],
      aiUserMessages: [],
      aiLastResponse: '',
      aiChatFormSnapshot: {
        prompt: '',
        outputFormatRequirement: '',
        outputFormatDesc: ''
      },
      aiChatInterfaceUrl: '',
      aiChatInteractionType: '',
      aiChatDetailList: [],
      jsonPreviewVisible: false,
      jsonPreviewBody: '',
      /** 每次打开弹窗生成，同一会话内各轮对话共用 */
      aiChatSessionId: '',
      maitalkBaseUrl: '',
      requestLogVisible: false,
      requestLogLoading: false,
      requestLogRecords: [],
      requestLogPage: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      logDetailVisible: false,
      logDetailRow: null,
      arrayTableVisible: false,
      arrayTableLoading: false,
      arrayTableColumns: [],
      arrayTableRows: [],
      arrayTableTip: ''
    };
  },
  methods: {
    newSessionId() {
      if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        console.log("Using crypto.randomUUID for session ID generation");
        return crypto.randomUUID();
      }
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
      });
    },
    markdownToSafeHtml(src) {
      const raw = src == null ? '' : String(src);
      const html = marked.parse(raw);
      return DOMPurify.sanitize(html);
    },
    /**
     * 从文本中解析 {"jsonData":...,"textContent":"..."}（仅保留这两字段；提取时优先匹配含 "jsonData" 的平衡括号片段）
     * @param {string} text
     * @returns {{ jsonData: *, textContent: string } | null}
     */
    parseStructuredAiResponse(text) {
      const raw = (text || '').trim();
      if (!raw) {
        return null;
      }
      const tryParse = (s) => {
        try {
          return JSON.parse(s);
        } catch (e) {
          return null;
        }
      };
      const parseBalancedAroundJsonData = (s) => {
        const marker = '"jsonData"';
        const idx = s.indexOf(marker);
        if (idx === -1) {
          return null;
        }
        const start = s.lastIndexOf('{', idx);
        if (start === -1) {
          return null;
        }
        let depth = 0;
        for (let i = start; i < s.length; i++) {
          const c = s[i];
          if (c === '{') {
            depth++;
          } else if (c === '}') {
            depth--;
            if (depth === 0) {
              return tryParse(s.slice(start, i + 1));
            }
          }
        }
        return null;
      };
      let json = tryParse(raw);
      if (!json) {
        const fence = raw.match(/^```(?:json)?\s*\n?([\s\S]*?)```$/i);
        if (fence) {
          json = tryParse(fence[1].trim());
        }
      }
      if (!json) {
        json = parseBalancedAroundJsonData(raw);
      }
      if (!json) {
        const start = raw.indexOf('{');
        const end = raw.lastIndexOf('}');
        if (start !== -1 && end > start) {
          json = tryParse(raw.slice(start, end + 1));
        }
      }
      if (!json || typeof json !== 'object') {
        return null;
      }
      if (!Object.prototype.hasOwnProperty.call(json, 'jsonData')) {
        return null;
      }
      if (!Object.prototype.hasOwnProperty.call(json, 'textContent')) {
        return null;
      }
      const textContent = json.textContent == null ? '' : String(json.textContent);
      return { jsonData: json.jsonData, textContent };
    },
    buildAssistantMessage(rawText) {
      const text = rawText == null ? '' : String(rawText);
      const structured = this.parseStructuredAiResponse(text);
      const mdSource = structured ? structured.textContent : text;
      return {
        role: 'assistant',
        content: text,
        displayHtml: this.markdownToSafeHtml(mdSource),
        structured,
        loading: false
      };
    },
    openJsonDataPreview(jsonData) {
      try {
        this.jsonPreviewBody =
          typeof jsonData === 'string' ? jsonData : JSON.stringify(jsonData, null, 2);
      } catch (e) {
        this.jsonPreviewBody = String(jsonData);
      }
      this.jsonPreviewVisible = true;
    },
    copyJsonPreview() {
      const t = this.jsonPreviewBody || '';
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(t).then(
          () => this.$message.success('已复制到剪贴板'),
          () => this.$message.error('复制失败')
        );
        return;
      }
      const ta = document.createElement('textarea');
      ta.value = t;
      ta.style.position = 'fixed';
      ta.style.left = '-9999px';
      document.body.appendChild(ta);
      ta.select();
      try {
        document.execCommand('copy');
        this.$message.success('已复制到剪贴板');
      } catch (e) {
        this.$message.error('复制失败');
      }
      document.body.removeChild(ta);
    },
    applyStructuredJson(jsonData) {
      try {
        console.log(JSON.stringify(jsonData, null, 2));
      } catch (e) {
        console.log(jsonData);
      }
      this.$emit('apply-structured-json', jsonData);
      this.$message.success('已应用结构化数据');
    },
    /**
     * @param {{ btnNo?: string, id?: string|number }} opts
     */
    open(opts) {
      const { btnNo, id } = opts || {};
      const hasBtnNo = btnNo != null && String(btnNo).trim() !== '';
      const hasId = id != null && String(id).trim() !== '';
      if (!hasBtnNo && !hasId) {
        this.$message.warning('请传入按钮编码 btnNo 或配置 id');
        return;
      }
      this.aiChatVisible = true;
      this.aiChatLoadingDetail = true;
      this.resetAiChatSession();
      this.aiChatSessionId = this.newSessionId();
      const params = hasBtnNo ? { btnNo: String(btnNo).trim() } : { id };
      getDetail(params)
        .then((res) => {
          const d = res.data.data || {};
          this.aiChatFormSnapshot = {
            prompt: d.prompt,
            outputFormatRequirement: d.outputFormatRequirement,
            outputFormatDesc: d.outputFormatDesc
          };
          const gr = d.guideReply != null ? String(d.guideReply).trim() : '';
          this.aiChatInputPlaceholder = gr;
          this.aiChatInterfaceUrl = (d.aiInterfaceUrl && String(d.aiInterfaceUrl).trim()) || '';
          this.aiChatInteractionType = (d.interactionType && String(d.interactionType).trim()) || '';
          this.aiChatDetailList = Array.isArray(d.detailList) ? d.detailList : [];
          this.maitalkBaseUrl = (d.maitalkBaseUrl && String(d.maitalkBaseUrl).trim()) || '';
        })
        .catch(() => {
          this.$message.error('加载配置详情失败');
          this.aiChatVisible = false;
        })
        .finally(() => {
          this.aiChatLoadingDetail = false;
        });
    },
    resetAiChatSession() {
      this.aiChatInput = '';
      this.aiChatMessages = [];
      this.aiUserMessages = [];
      this.aiLastResponse = '';
    },
    onAiChatClosed() {
      this.resetAiChatSession();
      this.aiChatInputPlaceholder = '';
      this.aiChatFormSnapshot = {
        prompt: '',
        outputFormatRequirement: '',
        outputFormatDesc: ''
      };
      this.aiChatInterfaceUrl = '';
      this.aiChatInteractionType = '';
      this.aiChatDetailList = [];
      this.jsonPreviewVisible = false;
      this.jsonPreviewBody = '';
      this.aiChatSessionId = '';
      this.maitalkBaseUrl = '';
      this.requestLogVisible = false;
      this.requestLogLoading = false;
      this.requestLogRecords = [];
      this.requestLogPage = { currentPage: 1, pageSize: 10, total: 0 };
      this.logDetailVisible = false;
      this.logDetailRow = null;
      this.arrayTableVisible = false;
      this.arrayTableLoading = false;
      this.arrayTableColumns = [];
      this.arrayTableRows = [];
      this.arrayTableTip = '';
    },
    formatCellValue(v) {
      if (v === null || v === undefined) return '';
      if (typeof v === 'string') return v;
      if (typeof v === 'number' || typeof v === 'boolean') return String(v);
      try {
        return JSON.stringify(v);
      } catch (e) {
        return String(v);
      }
    },
    openStructuredArrayTable(jsonData) {
      const parsedSchema = parseValidatedOutputFormatJsonSchema(
        this.aiChatFormSnapshot.outputFormatRequirement
      );
      if (!parsedSchema.ok) {
        this.$message.error(parsedSchema.message || 'jsonSchema 解析失败');
        return;
      }
      const colsRes = buildArrayTableColumnsFromJsonSchema(parsedSchema.jsonSchema);
      if (!colsRes.ok) {
        this.$message.warning(colsRes.message || '无法从 JsonSchema 生成表头');
        return;
      }
      const arrRes = parseJsonArrayData(jsonData);
      if (!arrRes.ok) {
        this.$message.warning(arrRes.message || 'jsonData 不是数组');
        return;
      }
      this.arrayTableTip = '';
      this.arrayTableColumns = colsRes.columns;
      this.arrayTableRows = arrRes.data;
      this.arrayTableVisible = true;
    },
    openRequestLogDialog() {
      if (!this.aiChatSessionId) {
        this.$message.warning('本次会话 uuid 为空');
        return;
      }
      if (!this.maitalkBaseUrl) {
        this.$message.warning('未获取到 maitalk baseUrl（请刷新并重新打开弹窗）');
        return;
      }
      this.requestLogVisible = true;
      this.requestLogPage.currentPage = 1;
      this.loadRequestLogs();
    },
    onRequestLogCurrentChange(p) {
      this.requestLogPage.currentPage = p;
      this.loadRequestLogs();
    },
    onRequestLogSizeChange(s) {
      this.requestLogPage.pageSize = s;
      this.requestLogPage.currentPage = 1;
      this.loadRequestLogs();
    },
    openLogRowDetail(row) {
      this.logDetailRow = row || null;
      this.logDetailVisible = true;
    },
    async loadRequestLogs() {
      this.requestLogLoading = true;
      try {
        const res = await request({
          url: '/api/blade-ai/btnConfig/requestLog',
          method: 'post',
          params: {
            uuid: this.aiChatSessionId,
            current: this.requestLogPage.currentPage,
            size: this.requestLogPage.pageSize
          }
        });
        // 兼容你提供的结构：res.data.data.data = { total,current,pages,size,records... }
        const payload =
          (res && res.data && res.data.data && res.data.data.data) ? res.data.data.data :
            ((res && res.data && res.data.data) ? res.data.data : (res && res.data));
        const records = payload && payload.records ? payload.records : (Array.isArray(payload) ? payload : []);
        const total = payload && payload.total != null ? Number(payload.total) : records.length;
        this.requestLogPage.total = Number.isFinite(total) ? total : records.length;
        this.requestLogRecords = (records || []).map((r) => {
          const dialogTime = r.dialogTime || r.createTime || r.requestTime || r.time || '';
          const params = r.params == null ? '' : (typeof r.params === 'string' ? r.params : JSON.stringify(r.params, null, 2));
          const resp = r.resp == null ? '' : (typeof r.resp === 'string' ? r.resp : JSON.stringify(r.resp, null, 2));
          const errorInfo = r.errorInfo == null ? '' : (typeof r.errorInfo === 'string' ? r.errorInfo : JSON.stringify(r.errorInfo, null, 2));
          const url = r.url == null ? '' : String(r.url);
          const sessionId = r.sessionId == null ? '' : String(r.sessionId);
          return { dialogTime, params, resp, errorInfo, url, sessionId };
        });
      } catch (e) {
        const msg = (e && e.message) ? e.message : '加载日志失败';
        this.$message.error(msg);
        this.requestLogRecords = [];
        this.requestLogPage.total = 0;
      } finally {
        this.requestLogLoading = false;
      }
    },
    scrollAiChatToBottom() {
      const el = this.$refs.aiChatScroll;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    },
    /**
     * 接口若返回带多余字段的对象，只序列化 jsonData + textContent，便于下游只认结构化载荷
     */
    normalizeStructuredResponsePayload(inner) {
      if (
        inner != null &&
        typeof inner === 'object' &&
        !Array.isArray(inner) &&
        Object.prototype.hasOwnProperty.call(inner, 'jsonData') &&
        Object.prototype.hasOwnProperty.call(inner, 'textContent')
      ) {
        return JSON.stringify(
          {
            jsonData: inner.jsonData,
            textContent: inner.textContent == null ? '' : String(inner.textContent)
          },
          null,
          2
        );
      }
      return null;
    },
    pickAiResponseText(res) {
      const d = res && res.data;
      if (typeof d === 'string') {
        return d;
      }
      if (d == null) {
        return '';
      }
      if (d.data !== undefined && d.code === 200) {
        const inner = d.data;
        if (typeof inner === 'string') {
          return inner;
        }
        if (inner != null && typeof inner === 'object') {
          const norm = this.normalizeStructuredResponsePayload(inner);
          if (norm != null) {
            return norm;
          }
          return JSON.stringify(inner, null, 2);
        }
      }
      if (typeof d.data === 'string') {
        return d.data;
      }
      if (d.data != null && typeof d.data === 'object') {
        const norm = this.normalizeStructuredResponsePayload(d.data);
        if (norm != null) {
          return norm;
        }
        return JSON.stringify(d.data, null, 2);
      }
      if (d.content != null) {
        return String(d.content);
      }
      if (d.message != null) {
        return String(d.message);
      }
      if (d.msg != null) {
        return String(d.msg);
      }
      return JSON.stringify(d, null, 2);
    },
    async sendAiChat() {
      const q = (this.aiChatInput || '').trim();
      if (!q) {
        this.$message.warning('请输入问题');
        return;
      }
      if (this.aiChatLoadingDetail) {
        return;
      }
      if (!this.aiChatInterfaceUrl) {
        this.$message.warning('当前按钮未配置「AI接口地址」，无法发送');
        return;
      }
      if (!this.aiChatInteractionType) {
        this.$message.warning('当前按钮未配置「中台交互类型」，无法发送');
        return;
      }
      const parsedSchema = parseValidatedOutputFormatJsonSchema(
        this.aiChatFormSnapshot.outputFormatRequirement
      );
      if (!parsedSchema.ok) {
        this.$message.error(parsedSchema.message || '「字段输出格式要求」校验失败');
        return;
      }
      const jsonSchema = parsedSchema.jsonSchema;
      const dataPayload = this.buildPreviewContent(
        this.aiChatDetailList,
        this.aiChatFormSnapshot.prompt,
        this.aiChatFormSnapshot.outputFormatRequirement,
        this.aiChatFormSnapshot.outputFormatDesc
      );

      this.aiUserMessages.push(q);
      this.aiChatMessages.push({ role: 'user', content: q });
      this.aiChatInput = '';
      this.$nextTick(() => this.scrollAiChatToBottom());
      const body = {
        aiInterfaceUrl: this.aiChatInterfaceUrl,
        sessionId: this.aiChatSessionId,
        data: dataPayload,
        interactionType: this.aiChatInteractionType,
        userMessages: this.aiUserMessages.slice(),
        lastResponse: this.aiLastResponse || '',
        responseFormat: 'json_schema',
        jsonSchema
      };

      this.aiChatMessages.push({ role: 'assistant', content: '', loading: true });
      const loadingMsgIdx = this.aiChatMessages.length - 1;
      this.$nextTick(() => this.scrollAiChatToBottom());

      this.aiChatSending = true;
      try {
        const res = await aiTestProxy(body);
        const text = this.pickAiResponseText(res);
        this.aiLastResponse = text;
        this.aiChatMessages[loadingMsgIdx] = this.buildAssistantMessage(text);
        this.$nextTick(() => this.scrollAiChatToBottom());
      } catch (e) {
        this.aiUserMessages.pop();
        this.aiChatMessages.pop(); // loading placeholder
        this.aiChatMessages.pop(); // user message
        this.aiChatInput = q;
        const msg = (e && e.message) ? e.message : '请求失败';
        this.$message.error(msg);
      } finally {
        this.aiChatSending = false;
      }
    },
    buildPreviewContent(list, prompt, outputFormatRequirement, outputFormatDesc) {
      const safe = (v) => (v === null || v === undefined ? '' : String(v));
      const nonEmpty = (v) => {
        const s = safe(v).trim();
        return s ? s : '';
      };
      const blocks = list.map((item) => {
        const md = safe(item.mockData).trim();
        const mdPart = md ? `；具体数据为：${md}` : '';
        return `【名称：${safe(item.dataName)}；说明：${safe(item.dataDesc)}；给出的是：${safe(item.$dataStructure)}：具体描述如下：${safe(item.specificDescription)}${mdPart}】`;
      });

      const background = `背景资料包括：\n${blocks.join('\n')}`;
      const p = nonEmpty(prompt);
      const o = nonEmpty(outputFormatRequirement);
      const op = nonEmpty(outputFormatDesc);

      const extraParts = [];
      if (p) {
        extraParts.push(`结合上述背景资料，你需要按如下要求进行处理：\n【${p}】`);
      }
      if (o) {
        extraParts.push(`你需要按照如下格式进行输出：\n【${o}】`);
      }
      if (op) {
        extraParts.push(`输出格式说明：\n【${op}】`);
      }

      if (!extraParts.length) return background;

      return background + '\n' + extraParts.join('\n');
    }
  }
};
</script>

<style scoped>
.ai-chat-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  /* ElementUI dialog 右上角关闭按钮会占用右侧空间，预留避免重叠 */
  padding-right: 44px;
}

.ai-chat-title-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-chat-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.ai-chat-icon-btn--title {
  width: 30px;
  height: 30px;
}

.ai-chat-icon-btn[disabled] {
  opacity: 0.55;
  cursor: not-allowed;
}

.ai-log-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.45;
  color: #303133;
}

.ai-log-ellipsis {
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-logdetail-meta {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
  margin-bottom: 10px;
  color: #606266;
  font-size: 12px;
}

.ai-log-pagination {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.ai-arraytable-tip {
  margin-bottom: 10px;
  color: #909399;
  font-size: 12px;
}

.ai-chat-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 520px;
}

.ai-chat-messages {
  flex: 1;
  max-height: 460px;
  min-height: 300px;
  overflow-y: auto;
  padding: 12px;
  background: linear-gradient(180deg, #f6f8fb 0%, #f0f2f5 100%);
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.ai-chat-empty {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 48px 16px;
  line-height: 1.6;
}

.ai-chat-bubble {
  max-width: 85%;
  margin-bottom: 14px;
  animation: ai-chat-fade-in 0.25s ease;
}

.ai-chat-bubble.has-structured {
  max-width: 94%;
}

.ai-chat-bubble.is-user {
  margin-left: auto;
  text-align: right;
}

.ai-chat-bubble.is-assistant {
  margin-right: auto;
  text-align: left;
}

.ai-chat-bubble-meta {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
  padding: 0 4px;
}

.ai-chat-bubble.is-user .ai-chat-bubble-meta {
  text-align: right;
}

.ai-chat-bubble-structured {
  position: relative;
  display: inline-block;
  max-width: 100%;
}

.ai-chat-bubble-actions {
  display: flex;
  flex-direction: row;
  gap: 6px;
  position: absolute;
  top: -10px;
  right: -10px;
  opacity: 0;
  transform: translate(6px, -6px);
  transition: opacity 0.22s ease, transform 0.22s ease;
  pointer-events: none;
}

.ai-chat-bubble-structured:hover .ai-chat-bubble-actions {
  opacity: 1;
  transform: translate(0, 0);
  pointer-events: auto;
}

.ai-chat-icon-btn {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  background: linear-gradient(180deg, #ffffff 0%, #f9fafc 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease,
    background 0.18s ease;
}

.ai-chat-icon-btn:hover {
  color: #409eff;
  border-color: #b3d8ff;
  background: #ecf5ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.18);
}

.ai-chat-icon-btn:active {
  transform: scale(0.96);
}

.ai-chat-bubble-text {
  display: inline-block;
  text-align: left;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.ai-chat-bubble-text--plain {
  white-space: pre-wrap;
}

.ai-chat-bubble-text--md {
  white-space: normal;
  padding: 12px 16px;
}

.ai-chat-bubble.is-user .ai-chat-bubble-text {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-chat-bubble.is-assistant .ai-chat-bubble-text {
  background: #fff;
  color: #303133;
  border: 1px solid #ebeef5;
  border-bottom-left-radius: 4px;
}

.ai-chat-md :deep(h1),
.ai-chat-md :deep(h2),
.ai-chat-md :deep(h3) {
  margin: 0.65em 0 0.4em;
  font-weight: 600;
  line-height: 1.35;
  color: #303133;
}

.ai-chat-md :deep(h1) {
  font-size: 1.25em;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 0.35em;
}

.ai-chat-md :deep(h2) {
  font-size: 1.12em;
}

.ai-chat-md :deep(h3) {
  font-size: 1.05em;
}

.ai-chat-md :deep(p) {
  margin: 0.45em 0;
}

.ai-chat-md :deep(p:first-child) {
  margin-top: 0;
}

.ai-chat-md :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-chat-md :deep(ul),
.ai-chat-md :deep(ol) {
  margin: 0.45em 0;
  padding-left: 1.35em;
}

.ai-chat-md :deep(li) {
  margin: 0.2em 0;
}

.ai-chat-md :deep(blockquote) {
  margin: 0.5em 0;
  padding: 0.4em 0.85em;
  border-left: 3px solid #409eff;
  background: #f5f9ff;
  color: #606266;
  border-radius: 0 6px 6px 0;
}

.ai-chat-md :deep(a) {
  color: #409eff;
  text-decoration: none;
  border-bottom: 1px solid rgba(64, 158, 255, 0.35);
}

.ai-chat-md :deep(a:hover) {
  border-bottom-color: #409eff;
}

.ai-chat-md :deep(code) {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 0.9em;
  padding: 0.12em 0.4em;
  background: #f0f2f5;
  border-radius: 4px;
  color: #c7254e;
}

.ai-chat-md :deep(pre) {
  margin: 0.55em 0;
  padding: 12px 14px;
  background: #f8f9fb;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow-x: auto;
  line-height: 1.45;
}

.ai-chat-md :deep(pre code) {
  padding: 0;
  background: transparent;
  color: #303133;
  font-size: 0.88em;
}

.ai-chat-md :deep(table) {
  border-collapse: collapse;
  margin: 0.55em 0;
  width: 100%;
  font-size: 0.95em;
}

.ai-chat-md :deep(th),
.ai-chat-md :deep(td) {
  border: 1px solid #ebeef5;
  padding: 6px 10px;
}

.ai-chat-md :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.ai-json-preview-toolbar {
  margin-bottom: 10px;
  display: flex;
  justify-content: flex-end;
}

.ai-json-preview {
  margin: 0;
  padding: 14px 16px;
  max-height: 420px;
  overflow: auto;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #303133;
  background: linear-gradient(180deg, #fafbfc 0%, #f4f6f8 100%);
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.04);
}

.ai-chat-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.ai-chat-input-row .el-textarea {
  flex: 1;
}

.ai-chat-send {
  flex-shrink: 0;
  height: 40px;
  width: 40px;
  padding: 0;
  border-radius: 50%;
}

.ai-chat-typing-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: ai-chat-typing 1s infinite ease-in-out;
  vertical-align: middle;
}

.ai-chat-typing-dot.d2 {
  animation-delay: 0.15s;
}

.ai-chat-typing-dot.d3 {
  margin-right: 0;
  animation-delay: 0.3s;
}

@keyframes ai-chat-fade-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes ai-chat-typing {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}
</style>
