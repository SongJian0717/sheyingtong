<template>
  <div class="photo-score-page">
    <div class="page-header">
      <h2>照片打分</h2>
      <p class="page-subtitle">上传照片，AI智能分析构图、光线、色彩，给出专业评分和改进建议</p>
    </div>
    
    <div class="upload-section">
      <el-card class="upload-card">
        <div class="upload-area">
          <el-upload
            class="upload-demo"
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :show-file-list="false"
            accept="image/*"
          >
            <div v-if="!uploadedFile" class="upload-placeholder">
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="upload-text">点击或拖拽上传照片</div>
              <div class="upload-hint">支持 JPG、PNG 格式，最大 50MB</div>
            </div>
            <div v-else class="upload-preview">
              <img :src="previewUrl" class="preview-image" />
              <div class="preview-info">
                <span class="file-name">{{ uploadedFile.name }}</span>
                <span class="file-size">{{ formatFileSize(uploadedFile.size) }}</span>
              </div>
              <el-button type="text" class="remove-btn" @click="clearFile">移除</el-button>
            </div>
          </el-upload>
        </div>
        
        <div class="upload-form">
          <el-input
            v-model="photoDescription"
            type="textarea"
            :rows="3"
            placeholder="请描述拍摄场景（可选），如：室内人像、日落风景、运动抓拍..."
          />
        </div>
        
        <div class="upload-actions">
          <el-button
            type="primary"
            size="large"
            :loading="isAnalyzing"
            :disabled="!uploadedFile || taskPending"
            @click="analyzePhoto"
          >
            {{ isAnalyzing ? '提交中...' : '开始分析' }}
          </el-button>
        </div>
      </el-card>
    </div>
    
    <!-- AI 异步任务状态：排队/分析中/失败/降级都在这里展示；SUCCESS 时不显示（直接看结果） -->
    <div v-if="taskStatus && taskStatus !== 'SUCCESS'" class="task-status-card" :class="['task-' + taskStatus.toLowerCase()]">
      <div class="task-status-inner">
        <el-icon v-if="taskPending" class="task-icon is-loading"><Loading /></el-icon>
        <el-icon v-else class="task-icon warn"><WarningFilled /></el-icon>
        <div class="task-status-body">
          <div class="task-status-title">{{ taskProgressText }}</div>
          <div v-if="taskErrorMessage" class="task-status-error">{{ taskErrorMessage }}</div>
          <div v-if="taskRetryable" class="task-status-actions">
            <el-button size="small" type="primary" @click="retryTask">重试</el-button>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="analysisResult" class="result-section">
      <el-card class="result-card">
        <div class="result-header">
          <h3>分析结果</h3>
          <div class="score-badge">
            <span class="score-value">{{ analysisResult.score }}</span>
            <span class="score-label">分</span>
          </div>
        </div>
        
        <div class="result-content">
          <div class="photo-preview-container">
            <img :src="previewUrl" class="result-image" />
          </div>
          
          <div class="analysis-details">
            <div class="analysis-list">
              <div class="analysis-item" v-for="(item, key) in analysisResult.analysis" :key="key">
                <div class="item-header">
                  <span class="item-icon">{{ getIcon(key) }}</span>
                  <span class="item-name">{{ getLabel(key) }}</span>
                  <span class="item-score" :class="getScoreClass(item.score)">{{ item.score }}</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: item.score + '%' }" :class="getScoreClass(item.score)"></div>
                </div>
                <p class="item-comment">{{ item.comment }}</p>
              </div>
            </div>
            
            <div class="suggestions-section">
              <h4 class="suggestions-title">💡 改进建议</h4>
              <ul class="suggestions-list">
                <li v-for="(suggestion, index) in analysisResult.suggestions" :key="index">
                  <span class="suggestion-number">{{ index + 1 }}</span>
                  <span class="suggestion-text">{{ suggestion }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </el-card>
    </div>
    
    <div class="history-section">
      <div class="section-header">
        <h3>历史记录</h3>
        <span class="total-count">共 {{ totalElements }} 条记录</span>
      </div>
      
      <div v-if="historyList.length > 0" class="history-list">
        <div v-for="item in historyList" :key="item.id" class="history-item">
          <div @click="viewHistoryDetail(item)" class="history-click-area">
            <div class="history-image">
              <img :src="item.filePath" :alt="item.id" />
              <div class="score-badge-small" :class="getScoreClass(item.score)">
                {{ item.score }}
              </div>
            </div>
            <div class="history-info">
              <span class="history-score" :class="getScoreClass(item.score)">{{ item.score }}分</span>
              <span class="history-level" :class="getScoreClass(item.score)">{{ getScoreLevel(item.score) }}</span>
              <span class="history-date">{{ formatDate(item.createdAt) }}</span>
            </div>
            <div class="history-overlay" @click.stop.prevent>
              <template v-if="isPublished(item.id)">
                <span class="publish-state published">✓ 已发布到广场</span>
              </template>
              <template v-else>
                <span class="view-text" @click="viewHistoryDetail(item)">查看详情</span>
                <span class="publish-btn" @click="openPublishDialog(item)">发布到广场</span>
              </template>
            </div>
          </div>
          <button class="delete-btn" @click.stop="deleteRecord(item.id)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>
      
      <div v-else class="empty-history">
        <el-empty description="暂无打分记录" />
      </div>
      
      <div v-if="totalPages > 1" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalElements"
          layout="prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 发布到作品广场 -->
    <el-dialog
      v-model="publishDialogVisible"
      title="发布到作品广场"
      width="520px"
      :close-on-click-modal="false"
    >
      <div class="publish-preview">
        <img :src="publishPreviewUrl()" alt="作品预览" />
        <p class="publish-tip">
          这张照片将作为<strong>公开作品</strong>展示在作品广场，其他用户可以查看、点赞与评论。
        </p>
      </div>
      <el-form label-position="top" class="publish-form">
        <el-form-item label="作品标题" required>
          <el-input
            v-model="publishForm.title"
            maxlength="100"
            show-word-limit
            placeholder="给你的作品起个标题（必填）"
          />
        </el-form-item>
        <el-form-item label="创作说明（可选）">
          <el-input
            v-model="publishForm.content"
            type="textarea"
            :rows="3"
            maxlength="5000"
            placeholder="聊聊拍摄灵感、构图思路或后期方法，让其他摄影师更懂你的作品"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="isPublishing"
          :disabled="!publishForm.title.trim()"
          @click="confirmPublish"
        >
          确认发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { UploadFilled, Delete, Loading, WarningFilled } from '@element-plus/icons-vue'
import { scorePhoto, getScoreHistory, getPhotoDetail, deleteAnalysis } from '../../api/photo'
import { getAiTask, retryAiTask } from '../../api/ai'
import { getMyWorks, publishWork } from '../../api/work'
import { ElMessageBox, ElMessage } from 'element-plus'

const uploadedFile = ref(null)
const previewUrl = ref('')
const photoDescription = ref('')
const isAnalyzing = ref(false)
const analysisResult = ref(null)
const historyList = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const totalElements = ref(0)
const totalPages = ref(0)

// ---------- AI 异步任务轮询状态 ----------
// 提交接口现在立即返回 taskId，真正的分析在后台执行；前端轮询 GET /api/ai/tasks/{taskId}
const AI_TASK_KEY = 'syt_ai_task_score' // sessionStorage 存 taskId：刷新/返回页面后继续轮询同一任务
const currentTaskId = ref(null)
const taskStatus = ref(null) // PENDING / RUNNING / SUCCESS / DEGRADED / FAILED / TIMEOUT
const taskErrorMessage = ref('')
const taskRetryable = ref(false)
let pollTimer = null
let pollStartTime = 0

const POLL_INTERVAL_FAST = 1500 // 前 30 秒 1.5s 一次
const POLL_INTERVAL_SLOW = 3000 // 之后放慢到 3s
const POLL_SLOW_AFTER = 30000
const POLL_CAP = 10 * 60 * 1000 // 10 分钟上限，超时提示可重试

const taskPending = computed(() => taskStatus.value === 'PENDING' || taskStatus.value === 'RUNNING')

const taskProgressText = computed(() => {
  const map = {
    PENDING: '任务排队中，等待 AI 分析...',
    RUNNING: 'AI 正在分析照片，通常需要 30~90 秒...',
    DEGRADED: 'AI 服务暂不可用，已展示降级建议结果',
    FAILED: '分析失败，可点击重试',
    TIMEOUT: '等待结果超时，可点击重试'
  }
  return map[taskStatus.value] || taskStatus.value || ''
})

// 已发布作品集合：photoAnalysisId → true（从"我的作品"拉取，用于禁用重复发布）
const publishedMap = ref(new Map())
// 发布弹窗状态
const publishDialogVisible = ref(false)
const publishTargetId = ref(null)
const publishForm = reactive({
  title: '',
  content: ''
})
const isPublishing = ref(false)

const isPublished = (photoAnalysisId) => publishedMap.value.has(photoAnalysisId)

const publishPreviewUrl = () =>
  publishTargetId.value ? `/api/photo/image/${publishTargetId.value}` : ''

const loadPublishedWorks = async () => {
  try {
    const works = await getMyWorks()
    const map = new Map()
    works.forEach((work) => map.set(work.photoAnalysisId, true))
    publishedMap.value = map
  } catch (error) {
    publishedMap.value = new Map()
  }
}

const openPublishDialog = (item) => {
  publishTargetId.value = item.id
  publishForm.title = ''
  publishForm.content = ''
  publishDialogVisible.value = true
}

const confirmPublish = async () => {
  const title = publishForm.title.trim()
  if (!title || !publishTargetId.value) return

  isPublishing.value = true
  try {
    await publishWork({
      photoAnalysisId: publishTargetId.value,
      title,
      content: publishForm.content.trim()
    })
    publishedMap.value.set(publishTargetId.value, true)
    publishDialogVisible.value = false
    ElMessage.success('发布成功，已展示在作品广场')
  } catch (error) {
    // 失败提示（如"该照片已发布过"）已由请求拦截器统一弹出
  } finally {
    isPublishing.value = false
  }
}

const mockAnalysisResult = {
  score: 85,
  analysis: {
    composition: {
      score: 88,
      comment: '构图运用了三分法，主体位置恰当，视觉引导良好。建议尝试对角线构图增加动感。'
    },
    lighting: {
      score: 82,
      comment: '光线充足，明暗对比适中。阴影部分细节保留较好，但高光区域略有过曝。'
    },
    color: {
      score: 86,
      comment: '色彩饱和度适中，白平衡准确。整体色调和谐，画面氛围营造良好。'
    },
    focus: {
      score: 84,
      comment: '主体对焦清晰，背景虚化效果自然。建议在拍摄时稍微缩小光圈以增加景深。'
    },
    content: {
      score: 85,
      comment: '照片主题明确，作者想要表达的想法基本清晰，画面能够传达一定的情感和故事性。'
    },
    suggestion: {
      score: 85,
      comment: '建议尝试从更低角度拍摄，可以增强主体的气势和画面的层次感。同时可以适当调整白平衡，让肤色更自然。'
    }
  },
  suggestions: [
    '尝试使用对角线构图，将主体沿对角线方向放置，可以让画面更有动感和延伸感。对角线构图能够引导观众的视线穿越画面，增加视觉张力。如果拍摄风景，可以寻找自然的对角线元素如道路、河流、山脉等作为引导线。',
    '注意高光区域的控制，避免过曝导致细节丢失。可以尝试使用曝光补偿减少0.3-0.7档，或者使用渐变滤镜来平衡天空和地面的曝光。在后期处理中，可以使用HDR工具或局部调整来恢复高光区域的细节。',
    '适当缩小光圈，增加景深，让前后景都更清晰。使用f/8-f/11的光圈可以获得较好的景深效果，适合拍摄风景或需要展现环境的人像照片。但也要注意过小的光圈会导致衍射现象，影响画质。',
    '后期可以稍微调整对比度，增强画面层次感。适当提高对比度可以让画面更加生动，但要注意不要过度调整导致暗部死黑或高光溢出。可以使用曲线工具进行精细调整，让画面的明暗过渡更加自然。'
  ]
}

const mockHistory = [
  {
    id: 1,
    score: 78,
    filePath: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=beautiful%20sunset%20landscape%20photography&image_size=square',
    createdAt: '2026-07-08 14:30:00'
  },
  {
    id: 2,
    score: 85,
    filePath: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=portrait%20photography%20beautiful%20woman%20natural%20light&image_size=square',
    createdAt: '2026-07-07 16:45:00'
  },
  {
    id: 3,
    score: 72,
    filePath: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=street%20photography%20urban%20scene%20people&image_size=square',
    createdAt: '2026-07-06 10:20:00'
  }
]

const handleFileChange = (file) => {
  stopPolling()
  currentTaskId.value = null
  taskStatus.value = null
  taskErrorMessage.value = ''
  taskRetryable.value = false
  uploadedFile.value = file.raw
  previewUrl.value = URL.createObjectURL(file.raw)
  analysisResult.value = null
}

const handleFileRemove = () => {
  clearFile()
}

const clearFile = () => {
  stopPolling()
  currentTaskId.value = null
  taskStatus.value = null
  taskErrorMessage.value = ''
  taskRetryable.value = false
  uploadedFile.value = null
  previewUrl.value = ''
  analysisResult.value = null
}

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const pad = (n) => String(n).padStart(2, '0')

/**
 * 后端 LocalDateTime 序列化为数组 [年,月,日,时,分,秒]，兼容 ISO 字符串/时间戳/Date
 */
const formatDate = (date) => {
  if (date === null || date === undefined || date === '') return ''

  if (Array.isArray(date)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = date
    if (!y) return ''
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`
  }

  if (date instanceof Date) {
    if (Number.isNaN(date.getTime())) return ''
    return formatDate([date.getFullYear(), date.getMonth() + 1, date.getDate(),
      date.getHours(), date.getMinutes(), date.getSeconds()])
  }

  if (typeof date === 'number') {
    const dt = new Date(date)
    return formatDate(dt)
  }

  return String(date).substring(0, 19).replace('T', ' ')
}

const getIcon = (key) => {
  const icons = {
    composition: '🎨',
    lighting: '💡',
    color: '🌈',
    focus: '🎯',
    content: '📖',
    suggestion: '💬'
  }
  return icons[key] || '📷'
}

const getLabel = (key) => {
  const labels = {
    composition: '构图',
    lighting: '光线',
    color: '色彩',
    focus: '对焦',
    content: '内容',
    suggestion: '建议'
  }
  return labels[key] || key
}

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-medium'
  return 'score-poor'
}

const getScoreLevel = (score) => {
  if (score >= 90) return '优秀'
  if (score >= 80) return '良好'
  if (score >= 60) return '中等'
  return '较差'
}

const parseSuggestions = (suggestions) => {
  if (!suggestions) return []
  return suggestions.split(/[\n、]/).map(s => s.trim()).filter(s => s && s.length > 1)
}

/**
 * 提交 AI 打分任务 → 轮询结果
 * 改造前这个接口要阻塞几十秒到几分钟；现在 POST /photo/score 立即返回 {taskId}，
 * 真正的分析在后台消费者线程执行，这里轮询 GET /api/ai/tasks/{taskId} 直到终态。
 */
const analyzePhoto = async () => {
  if (!uploadedFile.value || taskPending.value) return

  isAnalyzing.value = true
  analysisResult.value = null
  taskStatus.value = 'PENDING'
  taskErrorMessage.value = ''
  taskRetryable.value = false

  try {
    const response = await scorePhoto(uploadedFile.value, photoDescription.value)
    currentTaskId.value = response.taskId
    sessionStorage.setItem(AI_TASK_KEY, String(response.taskId))

    // broker 不可达降级为就地同步执行时：提交返回就是终态，直接渲染，不用等轮询
    if (response.status === 'SUCCESS' || response.status === 'DEGRADED' || response.status === 'FAILED') {
      const status = await getAiTask(response.taskId)
      renderTaskStatus(status)
    } else {
      startPolling(response.taskId)
    }
  } catch (error) {
    console.error('提交分析任务失败:', error)
    taskStatus.value = 'FAILED'
    taskErrorMessage.value = '任务提交失败，请重试'
  } finally {
    isAnalyzing.value = false
  }
}

// ---------- 轮询 ----------

const startPolling = (taskId) => {
  stopPolling()
  pollStartTime = Date.now()
  pollOnce(taskId)
}

const stopPolling = () => {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

const pollOnce = async (taskId) => {
  const elapsed = Date.now() - pollStartTime
  if (elapsed > POLL_CAP) {
    taskStatus.value = 'TIMEOUT'
    ElMessage.warning('等待结果超时，可点击“重试”继续')
    return
  }
  try {
    const status = await getAiTask(taskId)
    renderTaskStatus(status)
    if (!status.terminal) {
      const interval = elapsed < POLL_SLOW_AFTER ? POLL_INTERVAL_FAST : POLL_INTERVAL_SLOW
      pollTimer = setTimeout(() => pollOnce(taskId), interval)
    }
  } catch (error) {
    // 轮询接口偶发失败：退避后继续，不中断等待
    pollTimer = setTimeout(() => pollOnce(taskId), POLL_INTERVAL_SLOW)
  }
}

/** 把任务状态刷新到页面：终态且有 result 时渲染结果（结构与旧同步返回体完全一致） */
const renderTaskStatus = (status) => {
  currentTaskId.value = status.taskId
  taskStatus.value = status.status
  taskErrorMessage.value = status.errorMessage || ''
  taskRetryable.value = status.retryable
  if (status.result) {
    analysisResult.value = {
      score: status.result.score,
      analysis: status.result.analysis,
      suggestions: parseSuggestions(status.result.suggestions)
    }
  }
}

/** 手动重试 FAILED / DEGRADED 任务（复用已上传的原图，不需要重新上传） */
const retryTask = async () => {
  if (!currentTaskId.value) return
  try {
    await retryAiTask(currentTaskId.value)
    taskStatus.value = 'PENDING'
    taskErrorMessage.value = ''
    taskRetryable.value = false
    ElMessage.info('已重新提交任务，等待 AI 分析...')
    startPolling(currentTaskId.value)
  } catch (error) {
    // 失败提示已由请求拦截器统一弹出
    console.error('重试任务失败:', error)
  }
}

/** 页面刷新/返回后：从 sessionStorage 恢复未完成任务继续轮询 */
const resumePendingTask = async () => {
  const saved = sessionStorage.getItem(AI_TASK_KEY)
  if (!saved) return
  try {
    const status = await getAiTask(saved)
    renderTaskStatus(status)
    if (!status.terminal) {
      startPolling(saved)
    }
  } catch (error) {
    // 任务不存在/已换用户无权限：清掉残留
    sessionStorage.removeItem(AI_TASK_KEY)
  }
}

const loadHistory = async () => {
  try {
    const response = await getScoreHistory(currentPage.value - 1, pageSize.value)
    historyList.value = (response.content || []).map(item => ({
      id: item.id,
      score: item.score,
      filePath: '/api/photo/image/' + item.id,
      createdAt: formatDate(item.createdAt)
    }))
    totalElements.value = response.totalElements || 0
    totalPages.value = response.totalPages || 0
  } catch (error) {
    console.error('加载历史记录失败:', error)
    historyList.value = []
    totalElements.value = 0
    totalPages.value = 0
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadHistory()
}

const deleteRecord = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条打分记录吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await deleteAnalysis(id)
    publishedMap.value.delete(id)
    ElMessage.success('删除成功')
    loadHistory()
  } catch (error) {
    // 已发布为作品的记录无法删除，后端会返回明确提示并由请求拦截器统一弹出
    console.error('删除打分记录失败:', error)
  }
}

const viewHistoryDetail = async (item) => {
  previewUrl.value = item.filePath
  
  try {
    const response = await getPhotoDetail(item.id)
    analysisResult.value = {
      score: response.score,
      analysis: response.analysis,
      suggestions: parseSuggestions(response.suggestions)
    }
  } catch (error) {
    console.error('加载历史详情失败:', error)
    analysisResult.value = {
      score: item.score,
      analysis: {
        composition: { score: item.score, comment: '查看详情失败' },
        lighting: { score: item.score, comment: '查看详情失败' },
        color: { score: item.score, comment: '查看详情失败' },
        focus: { score: item.score, comment: '查看详情失败' }
      },
      suggestions: ['查看详情失败']
    }
  }
  
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  loadHistory()
  loadPublishedWorks()
  resumePendingTask()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.photo-score-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.page-subtitle {
  font-size: 16px;
  color: #666;
}

.upload-section {
  margin-bottom: 30px;
}

.upload-card {
  border-radius: 12px;
}

.upload-area {
  margin-bottom: 20px;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  border: 2px dashed #d9d9d9;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-placeholder:hover {
  border-color: #667eea;
  background: #f5f7ff;
}

.upload-icon {
  font-size: 48px;
  color: #667eea;
  margin-bottom: 15px;
}

.upload-text {
  font-size: 18px;
  color: #333;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 14px;
  color: #999;
}

.upload-preview {
  position: relative;
  border: 2px dashed #667eea;
  border-radius: 12px;
  overflow: hidden;
}

.preview-image {
  width: 100%;
  max-height: 400px;
  object-fit: contain;
}

.preview-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: #f5f7ff;
}

.file-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.file-size {
  font-size: 13px;
  color: #999;
  margin-left: 15px;
}

.remove-btn {
  color: #f56c6c;
  font-size: 13px;
}

.upload-form {
  margin-bottom: 20px;
}

.upload-actions {
  display: flex;
  justify-content: center;
}

/* ---------- AI 异步任务状态卡片 ---------- */
.task-status-card {
  margin-bottom: 30px;
  border-radius: 12px;
  border: 1px solid #e9ecef;
  background: #f8f9fa;
}

.task-status-card.task-degraded {
  background: #fffbe6;
  border-color: #ffe58f;
}

.task-status-card.task-failed {
  background: #fff2f0;
  border-color: #ffccc7;
}

.task-status-inner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
}

.task-icon {
  font-size: 26px;
  flex-shrink: 0;
}

.task-icon.is-loading {
  animation: rotating 1.2s linear infinite;
  color: #667eea;
}

.task-icon.warn {
  color: #e6a23c;
}

.task-status-body {
  flex: 1;
  min-width: 0;
}

.task-status-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.task-status-error {
  margin-top: 6px;
  font-size: 13px;
  color: #f56c6c;
  word-break: break-all;
}

.task-status-actions {
  margin-top: 10px;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.result-section {
  margin-bottom: 30px;
}

.result-card {
  border-radius: 12px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.result-header h3 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.score-badge {
  display: flex;
  align-items: baseline;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 12px 24px;
  border-radius: 50px;
  color: white;
}

.score-value {
  font-size: 48px;
  font-weight: bold;
}

.score-label {
  font-size: 18px;
  margin-left: 5px;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.photo-preview-container {
  max-width: 600px;
  margin: 0 auto;
}

.result-image {
  width: 100%;
  max-height: 400px;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.analysis-details {
  width: 100%;
}

.analysis-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
}

.analysis-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  border-left: 4px solid #667eea;
}

.item-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.item-icon {
  font-size: 20px;
  margin-right: 10px;
}

.item-name {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.item-score {
  font-size: 24px;
  font-weight: bold;
}

.score-excellent {
  color: #67c23a;
}

.score-good {
  color: #667eea;
}

.score-medium {
  color: #e6a23c;
}

.score-poor {
  color: #f56c6c;
}

.progress-bar {
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.progress-fill.score-excellent {
  background: #67c23a;
}

.progress-fill.score-good {
  background: #667eea;
}

.progress-fill.score-medium {
  background: #e6a23c;
}

.progress-fill.score-poor {
  background: #f56c6c;
}

.item-comment {
  font-size: 15px;
  line-height: 1.8;
  color: #444;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.suggestions-section {
  background: #fffbe6;
  border-radius: 12px;
  padding: 25px;
  margin-top: 25px;
}

.suggestions-title {
  font-size: 18px;
  font-weight: bold;
  color: #856404;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.suggestions-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.suggestions-list li {
  display: flex;
  gap: 15px;
  background: #fff;
  border-radius: 10px;
  padding: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border-left: 4px solid #ffc107;
}

.suggestion-number {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #ffc107 0%, #ffb300 100%);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(255, 193, 7, 0.3);
}

.suggestion-text {
  font-size: 15px;
  color: #444;
  line-height: 1.8;
  flex: 1;
}

.history-section {
  margin-top: 30px;
}

.section-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.total-count {
  font-size: 14px;
  color: #999;
}

.history-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.history-item {
  position: relative;
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.history-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.history-click-area {
  cursor: pointer;
}

.history-image {
  position: relative;
  height: 150px;
  overflow: hidden;
}

.history-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.score-badge-small {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.score-badge-small.score-excellent {
  background: #67c23a;
}

.score-badge-small.score-good {
  background: #667eea;
}

.score-badge-small.score-medium {
  background: #e6a23c;
}

.score-badge-small.score-poor {
  background: #f56c6c;
}

.history-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 15px;
}

.history-level {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
}

.history-level.score-excellent {
  background: #e8f5e9;
  color: #2e7d32;
}

.history-level.score-good {
  background: #e8eaf6;
  color: #3949ab;
}

.history-level.score-medium {
  background: #fff8e1;
  color: #f57c00;
}

.history-level.score-poor {
  background: #ffebee;
  color: #c62828;
}

.history-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.history-item:hover .history-overlay {
  opacity: 1;
}

.view-text {
  color: white;
  font-size: 14px;
  font-weight: 600;
  padding: 8px 16px;
  background: rgba(102, 126, 234, 0.9);
  border-radius: 20px;
}

.publish-btn {
  color: white;
  font-size: 13px;
  font-weight: 600;
  padding: 7px 14px;
  background: rgba(103, 194, 58, 0.9);
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.publish-btn:hover {
  background: #67c23a;
}

.publish-state {
  font-size: 13px;
  font-weight: 600;
  padding: 7px 14px;
  border-radius: 20px;
}

.publish-state.published {
  color: #2e7d32;
  background: rgba(232, 245, 233, 0.92);
}

.history-score {
  font-size: 16px;
  font-weight: bold;
  color: #667eea;
}

.history-date {
  font-size: 12px;
  color: #999;
}

.empty-history {
  padding: 40px 0;
}

.delete-btn {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: rgba(245, 108, 108, 0.9);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  z-index: 10;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #f56c6c;
  transform: scale(1.1);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

/* ---------- 发布到广场弹窗 ---------- */
.publish-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.publish-preview img {
  max-width: 100%;
  max-height: 220px;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}

.publish-tip {
  font-size: 13px;
  color: #888;
  margin: 0;
  text-align: center;
}

.publish-tip strong {
  color: #667eea;
}

@media (max-width: 768px) {
  .result-content {
    flex-direction: column;
  }
  
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}
</style>