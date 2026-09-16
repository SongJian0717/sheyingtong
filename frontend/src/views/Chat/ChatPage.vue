<template>
  <div class="chat-page">
    <div class="chat-container">
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h2 class="sidebar-title">💬 AI问答助手</h2>
          <el-button type="primary" size="small" @click="createNewSession">
            <el-icon><Plus /></el-icon>
            新建对话
          </el-button>
        </div>
        
        <div class="session-list">
          <div 
            v-for="session in sessions" 
            :key="session.id"
            class="session-item"
            :class="{ active: currentSessionId === session.id }"
            @click="loadSession(session.id)"
          >
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
            <el-button 
              type="text" 
              class="delete-btn" 
              @click.stop="deleteSession(session.id)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          
          <div v-if="sessions.length === 0" class="empty-sessions">
            <el-icon size="48" class="empty-icon"><Message /></el-icon>
            <p>暂无对话，点击上方按钮开始</p>
          </div>
        </div>
      </div>

      <div class="chat-main" v-if="currentSessionId">
        <div class="chat-header">
          <h3 class="chat-title">{{ currentSession?.title || '新对话' }}</h3>
          <div class="chat-actions">
            <el-button type="text" @click="clearMessages">清空消息</el-button>
          </div>
        </div>

        <div class="chat-messages" ref="messagesContainer">
          <div 
            v-for="message in messages" 
            :key="message.id"
            class="message-item"
            :class="{ 'user-message': message.role === 'user', 'ai-message': message.role === 'assistant' }"
          >
            <div class="message-avatar">
              <el-icon v-if="message.role === 'user'" size="24"><User /></el-icon>
              <el-icon v-else size="24"><Sunny /></el-icon>
            </div>
            <div class="message-content">
              <div v-if="message.image" class="message-image">
                <img :src="message.image" class="message-img" />
              </div>
              <div class="message-text">
                <span v-if="message.role === 'user'">{{ message.content }}</span>
                <VueMarkdown v-else :source="message.content" />
              </div>
              <div v-if="message.toolResult" class="tool-result">
                <el-divider content-position="left">工具调用结果</el-divider>
                <VueMarkdown :source="message.toolResult" />
              </div>
              <div class="message-time">{{ formatTime(message.createdAt) }}</div>
            </div>
          </div>
          
          <div v-if="isLoading" class="loading-indicator">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <span>AI正在思考...</span>
          </div>
        </div>

        <div class="chat-input-area">
          <div class="input-tools">
            <el-button type="text" class="upload-btn" @click="triggerImageUpload">
              <el-icon><Picture /></el-icon>
              上传图片
            </el-button>
            <input 
              type="file" 
              ref="imageInput"
              class="image-input" 
              accept="image/*" 
              @change="handleImageUpload"
            />
          </div>
          <div v-if="uploadedImage" class="image-preview">
            <img :src="uploadedImage.preview" class="preview-image" />
            <el-button type="text" class="remove-image-btn" @click="removeImage">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入你的摄影问题...或上传照片让AI分析"
            @keyup.enter.exact="sendMessage"
            :disabled="isLoading"
          />
          <div class="input-actions">
            <el-button 
              type="primary" 
              @click="sendMessage"
              :disabled="!inputMessage.trim() && !uploadedImage || isLoading"
            >
              <el-icon><ChatLineRound /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </div>

      <div class="chat-welcome" v-else>
        <div class="welcome-content">
          <div class="welcome-badge">
            <el-icon :size="34"><ChatSquare /></el-icon>
          </div>
          <h2 class="welcome-title">嗨，我是摄影 AI 助手 👋</h2>
          <p class="welcome-desc">挑一个问题直接开聊，也可以新建空白对话自由提问</p>

          <div class="welcome-starters">
            <div
              v-for="question in starterQuestions"
              :key="question"
              class="starter-card"
              @click="startWithQuestion(question)"
            >
              <span class="starter-icon">{{ starterIcons[question] }}</span>
              <span class="starter-text">{{ question }}</span>
              <span class="starter-go">→</span>
            </div>
          </div>

          <div class="welcome-foot">
            想自己提问？
            <el-button type="primary" @click="createNewSession">新建对话开始</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>import { ref, onMounted, nextTick } from 'vue';
import { Delete, Plus, User, Sunny, ChatLineRound, Message, ChatSquare, Loading, Picture } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';
import VueMarkdown from 'vue-markdown-render';
import { getSessions, createSession, sendMessage as sendMessageApi, sendMessageStream, sendMessageWithImage, getMessages, deleteSession as deleteSessionApi } from '../../api/chat';
const sessions = ref([]);
const messages = ref([]);
const currentSessionId = ref(null);
const currentSession = ref(null);
const inputMessage = ref('');
const isLoading = ref(false);
const messagesContainer = ref(null);
const imageInput = ref(null);
const uploadedImage = ref(null);
const loadSessions = async () => {
 try {
 const response = await getSessions();
 sessions.value = response || [];
 if (sessions.value.length > 0 && !currentSessionId.value) {
 await loadSession(sessions.value[0].id);
 }
 }
 catch (error) {
 console.error('加载会话失败:', error);
 }
};
const createNewSession = async () => {
 try {
 const response = await createSession();
 const newSession = response;
 sessions.value.unshift(newSession);
 await loadSession(newSession.id);
 }
 catch (error) {
 console.error('创建会话失败:', error);
 }
};
const loadSession = async (sessionId) => {
 currentSessionId.value = sessionId;
 currentSession.value = sessions.value.find(s => s.id === sessionId);
 try {
 const response = await getMessages(sessionId);
 messages.value = response || [];
 await scrollToBottom();
 }
 catch (error) {
 console.error('加载消息失败:', error);
 }
};
const deleteSessionHandler = async (sessionId) => {
 try {
 await deleteSessionApi(sessionId);
 sessions.value = sessions.value.filter(s => s.id !== sessionId);
 if (currentSessionId.value === sessionId) {
 currentSessionId.value = null;
 currentSession.value = null;
 messages.value = [];
 }
 }
 catch (error) {
 console.error('删除会话失败:', error);
 }
};
const deleteSession = (sessionId) => {
 ElMessageBox.confirm('确定要删除这个对话吗？', '提示', {
 confirmButtonText: '确定',
 cancelButtonText: '取消',
 type: 'warning'
 }).then(() => {
 deleteSessionHandler(sessionId);
 }).catch(() => { });
};
const clearMessages = () => {
 ElMessageBox.confirm('确定要清空当前对话的所有消息吗？', '提示', {
 confirmButtonText: '确定',
 cancelButtonText: '取消',
 type: 'warning'
 }).then(() => {
 messages.value = [];
 }).catch(() => { });
};
/**
 * 【聊天主流程】发送按钮/回车触发的核心消息处理函数
 * ----------------------------------------------------------------
 * 执行流程：
 *  Step 1：输入校验 → 空内容且无图 或 isLoading=true 直接 return
 *  Step 2：构造用户消息气泡，先显示在 UI（📷 图片预览 + 文字拼接），
 *          uploadedImage.value=null 清理上传框
 *  Step 3：推入一条 role="assistant" content="" 的"空"占位消息气泡，
 *          用 assistantMessageId 记录位置，后续 onChunk 回调中直接 += chunk
 *  Step 4：根据是否有图片分流：
 *           - 有图   → sendMessageWithImage()（multipart 多模态分析）
 *           - 无图   → sendMessageStream()      （SSE 文字流式）
 *          每次回调 chunk → 找到占位消息 → 响应式 += chunk → nextTick 滚到底部
 *  Step 5：finally → isLoading=false，解除按钮禁用 + 允许下一条输入
 *  异常降级：catch 里把占位消息内容替换成"抱歉，我现在无法回答..."
 *
 *  【设计要点】为什么先 push 空气泡再流式 append？
 *    → 要实现像 ChatGPT 一样的"打字机逐字插入"效果，
 *      需要先在 messages 数组中占一个位置，通过 Vue 响应式让 UI 先渲染 DOM，
 *      后续 onChunk 直接修改 content 字符串即可触发增量 patch，不用重绘整条消息。
 */
const sendMessageHandler = async () => {
 if ((!inputMessage.value.trim() && !uploadedImage.value) || isLoading.value)
 return;
 const content = inputMessage.value.trim();
 const hasImage = !!uploadedImage.value;
 const imageFile = uploadedImage.value?.file;
 inputMessage.value = '';
 const userContent = hasImage ? `📷 [图片: ${uploadedImage.value.name}]\n${content || '请分析这张照片'}` : content;
 messages.value.push({
 id: Date.now(),
 role: 'user',
 content: userContent,
 image: hasImage ? uploadedImage.value.preview : null,
 createdAt: new Date().toISOString()
 });
 uploadedImage.value = null;
 await scrollToBottom();
 isLoading.value = true;
 const assistantMessageId = Date.now() + 1;
 messages.value.push({
 id: assistantMessageId,
 role: 'assistant',
 content: '',
 createdAt: new Date().toISOString()
 });
 await scrollToBottom();
 try {
 if (hasImage) {
 await sendMessageWithImage(currentSessionId.value, content || '请分析这张照片', imageFile, (chunk) => {
 const msgIndex = messages.value.findIndex(m => m.id === assistantMessageId);
 if (msgIndex !== -1) {
 messages.value[msgIndex].content += chunk;
 nextTick(() => {
 scrollToBottom();
 });
 }
 });
 }
 else {
 await sendMessageStream(currentSessionId.value, content, (chunk) => {
 const msgIndex = messages.value.findIndex(m => m.id === assistantMessageId);
 if (msgIndex !== -1) {
 messages.value[msgIndex].content += chunk;
 nextTick(() => {
 scrollToBottom();
 });
 }
 }, (error) => {
 console.error('流式响应错误:', error);
 });
 }
 }
 catch (error) {
 const msgIndex = messages.value.findIndex(m => m.id === assistantMessageId);
 if (msgIndex !== -1) {
 messages.value[msgIndex].content = '抱歉，我现在无法回答您的问题，请稍后再试。';
 }
 }
 finally {
 isLoading.value = false;
 }
};
const sendMessage = () => {
  sendMessageHandler();
};

// 欢迎页快捷提问卡片：引导问题 → 图标
const starterIcons = {
  '给我讲讲光圈、快门、ISO 怎么配合': '📖',
  '推荐一款适合新手的入门相机': '🎥',
  '这张照片该怎么调成日系色调': '🎨',
  '拍夜景人像要注意哪些设置': '🌃'
};
const starterQuestions = Object.keys(starterIcons);

// 点击引导卡片：先建会话，再自动发送该问题，进入正常的流式回答
const startWithQuestion = async (question) => {
  try {
    await createNewSession();
    inputMessage.value = question;
    sendMessage();
  } catch (error) {
    console.error('开始对话失败:', error);
  }
};
const triggerImageUpload = () => {
 if (imageInput.value) {
 imageInput.value.click();
 }
};
const handleImageUpload = async (event) => {
 const file = event.target.files?.[0];
 if (!file) return;
 const reader = new FileReader();
 reader.onload = (e) => {
 uploadedImage.value = {
 file: file,
 preview: e.target.result,
 name: file.name
 };
 };
 reader.readAsDataURL(file);
 event.target.value = '';
};
const removeImage = () => {
 uploadedImage.value = null;
};
const scrollToBottom = async () => {
 await nextTick();
 if (messagesContainer.value) {
 messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
 }
};
const formatTime = (dateString) => {
 if (!dateString)
 return '';
 const date = new Date(dateString);
 const now = new Date();
 const diff = now - date;
 if (diff < 60000) {
 return '刚刚';
 }
 else if (diff < 3600000) {
 return `${Math.floor(diff / 60000)}分钟前`;
 }
 else if (diff < 86400000) {
 return `${Math.floor(diff / 3600000)}小时前`;
 }
 else {
 return `${date.getMonth() + 1}/${date.getDate()}`;
 }
};
onMounted(() => {
 loadSessions();
});
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.chat-container {
  width: 90%;
  max-width: 1200px;
  height: 90%;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  overflow: hidden;
}

.chat-sidebar {
  width: 320px;
  background: #f8f9fa;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  padding: 15px;
  margin-bottom: 8px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.session-item:hover {
  background: #e8f0fe;
}

.session-item.active {
  background: #667eea;
  color: white;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 12px;
  color: #999;
}

.session-item.active .session-time {
  color: rgba(255, 255, 255, 0.7);
}

.delete-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.empty-sessions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #999;
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 12px;
}

.user-message .message-avatar {
  background: #667eea;
  color: white;
}

.ai-message .message-avatar {
  background: #f0f0f0;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
}

.user-message .message-text {
  background: #667eea;
  color: white;
  border-radius: 12px 12px 0 12px;
}

.ai-message .message-text {
  background: white;
  color: #333;
  border-radius: 12px 12px 12px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.tool-result {
  margin-top: 10px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: 13px;
  color: #666;
}

.tool-result pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3),
.message-text :deep(h4),
.message-text :deep(h5),
.message-text :deep(h6) {
  font-weight: 600;
  margin: 12px 0 8px;
  line-height: 1.4;
}

.message-text :deep(h1) { font-size: 18px; }
.message-text :deep(h2) { font-size: 16px; }
.message-text :deep(h3) { font-size: 15px; }
.message-text :deep(h4),
.message-text :deep(h5),
.message-text :deep(h6) { font-size: 14px; }

.message-text :deep(p) {
  margin: 8px 0;
  line-height: 1.7;
}

.message-text :deep(strong),
.message-text :deep(b) {
  font-weight: 600;
  color: #333;
}

.message-text :deep(em),
.message-text :deep(i) {
  font-style: italic;
}

.message-text :deep(u) {
  text-decoration: underline;
}

.message-text :deep(s),
.message-text :deep(strike) {
  text-decoration: line-through;
}

.message-text :deep(code) {
  background: #f4f4f4;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #c7254e;
}

.message-text :deep(pre) {
  background: #2d2d2d;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.message-text :deep(pre code) {
  background: none;
  padding: 0;
  color: #ccc;
  font-size: 13px;
  line-height: 1.6;
}

.message-text :deep(blockquote) {
  border-left: 4px solid #667eea;
  padding: 8px 16px;
  margin: 12px 0;
  background: #f8f9fa;
  border-radius: 0 4px 4px 0;
  color: #666;
  font-style: italic;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.message-text :deep(li) {
  margin: 4px 0;
  line-height: 1.6;
}

.message-text :deep(li > ul),
.message-text :deep(li > ol) {
  margin: 4px 0;
}

.message-text :deep(a) {
  color: #667eea;
  text-decoration: none;
}

.message-text :deep(a:hover) {
  text-decoration: underline;
}

.message-text :deep(hr) {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 16px 0;
}

.message-text :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13px;
}

.message-text :deep(th),
.message-text :deep(td) {
  border: 1px solid #e0e0e0;
  padding: 8px 12px;
  text-align: left;
}

.message-text :deep(th) {
  background: #f8f9fa;
  font-weight: 600;
}

.message-text :deep(tr:nth-child(even)) {
  background: #fafafa;
}

.message-text :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  color: #666;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.chat-input-area {
  padding: 20px;
  border-top: 1px solid #e0e0e0;
  background: white;
}

.input-tools {
  margin-bottom: 10px;
}

.upload-btn {
  color: #667eea;
  padding: 6px 12px;
  font-size: 14px;
}

.upload-btn:hover {
  background: #e8f0fe;
}

.image-input {
  display: none;
}

.image-preview {
  position: relative;
  margin-bottom: 10px;
  max-width: 200px;
}

.preview-image {
  width: 100%;
  height: auto;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.remove-image-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.remove-image-btn:hover {
  background: #ff4d4f;
  color: #fff;
}

.message-image {
  margin-bottom: 8px;
}

.message-img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  object-fit: contain;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.chat-welcome {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  overflow-y: auto;
}

.welcome-content {
  width: 100%;
  max-width: 660px;
  padding: 40px;
  text-align: center;
}

.welcome-badge {
  width: 76px;
  height: 76px;
  margin: 0 auto 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 26px rgba(102, 126, 234, 0.35);
}

.welcome-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #222;
}

.welcome-desc {
  font-size: 15px;
  color: #888;
  margin: 0 0 28px;
}

.welcome-starters {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 26px;
  text-align: left;
}

.starter-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #eceef5;
  border-radius: 12px;
  padding: 15px 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  color: #444;
}

.starter-card:hover {
  border-color: #c9d2fb;
  background: #f7f9ff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.12);
}

.starter-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.starter-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.5;
}

.starter-go {
  color: #c3c9e8;
  font-size: 16px;
  flex-shrink: 0;
}

.welcome-foot {
  font-size: 14px;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

@media (max-width: 720px) {
  .welcome-starters {
    grid-template-columns: 1fr;
  }
}

</style>