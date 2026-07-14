<template>
  <div class="retouch-page">
    <div class="page-header">
      <h2>修图建议</h2>
      <p class="page-subtitle">上传照片，AI智能分析并给出专业修图建议和步骤</p>
    </div>
    
    <div class="upload-section">
      <el-card class="upload-card">
        <div class="upload-area" :class="previewUrl ? 'has-file' : ''">
          <input 
            type="file" 
            ref="fileInput"
            class="file-input" 
            accept="image/*" 
            @change="handleFileChange"
          />
          <div v-if="!previewUrl" class="upload-placeholder" @click="triggerUpload">
            <div class="upload-icon">📷</div>
            <p>点击或拖拽上传照片</p>
            <p class="upload-hint">支持 JPG、PNG 格式，最大 50MB</p>
          </div>
          <div v-else class="upload-preview">
            <img :src="previewUrl" :alt="uploadedFile?.name || '预览图片'" />
            <div class="preview-info">
              <span>{{ uploadedFile?.name || '历史图片' }}</span>
              <span>{{ uploadedFile ? formatFileSize(uploadedFile.size) : '' }}</span>
            </div>
            <button class="remove-btn" @click="removeFile">✕</button>
          </div>
        </div>
        
        <div class="description-input">
          <el-input 
            v-model="photoDescription" 
            type="textarea" 
            placeholder="描述拍摄场景或你想要的修图风格（可选）" 
            :rows="3"
          />
        </div>
        
        <div class="action-buttons">
          <el-button type="primary" size="large" :loading="isAnalyzing" :disabled="!uploadedFile" @click="analyzePhoto">
            {{ isAnalyzing ? '分析中...' : '🎨 获取修图建议' }}
          </el-button>
        </div>
      </el-card>
    </div>
    
    <div v-if="retouchResult" class="result-section">
      <div class="result-header">
        <h3>💡 修图建议</h3>
        <span class="result-date">{{ formatDate(new Date()) }}</span>
      </div>
      
      <div class="comparison-section">
        <el-card class="comparison-card">
          <div class="comparison-header">
            <h4>对比预览</h4>
            <div class="comparison-controls">
              <span class="control-label">原图</span>
              <el-slider 
                v-model="comparisonPosition" 
                :min="0" 
                :max="100" 
                :step="1"
                style="width: 300px"
              />
              <span class="control-label">效果图</span>
            </div>
          </div>
          
          <div class="comparison-view">
        <div class="comparison-container" @mousemove="handleMouseMove" @mouseleave="isDragging = false">
          <div class="original-image">
            <img :src="previewUrl" alt="原图" />
            <span class="image-label">原图</span>
          </div>
          <div class="retouched-image" :style="{ clipPath: `inset(0 ${100 - comparisonPosition}% 0 0)` }">
            <canvas ref="resultCanvas" class="result-canvas"></canvas>
            <span class="image-label">效果图</span>
          </div>
          <div 
            class="comparison-handle" 
            :style="{ left: comparisonPosition + '%' }"
            @mousedown="isDragging = true"
          >
            <div class="handle-line"></div>
            <div class="handle-icon">↔</div>
          </div>
        </div>
      </div>
        </el-card>
      </div>
      
      <div class="suggestions-section">
        <el-card class="suggestion-card">
          <h4 class="suggestion-title">🎯 修图目标</h4>
          <p class="suggestion-content">{{ retouchResult.objective }}</p>
        </el-card>
        
        <el-card class="steps-card">
          <h4 class="steps-title">📋 修图步骤</h4>
          <div class="steps-list">
            <div v-for="(step, index) in retouchResult.steps" :key="index" class="step-item">
              <div class="step-number" :class="getStepClass(index)">{{ index + 1 }}</div>
              <div class="step-content">
                <h5>{{ step.title }}</h5>
                <p>{{ step.description }}</p>
                <div class="step-tools">
                  <span v-for="(tool, i) in step.tools" :key="i" class="tool-tag">{{ tool }}</span>
                </div>
              </div>
              <div class="step-icon">{{ step.icon }}</div>
            </div>
          </div>
        </el-card>
        
        <div class="tools-section">
          <el-card class="tools-card">
            <h4 class="tools-title">🛠️ 推荐工具</h4>
            <div class="tools-grid">
              <div v-for="tool in retouchResult.recommendedTools" :key="tool.id" class="tool-item">
                <div class="tool-icon">{{ tool.icon }}</div>
                <div class="tool-name">{{ tool.name }}</div>
                <div class="tool-desc">{{ tool.description }}</div>
              </div>
            </div>
          </el-card>
          
          <el-card class="color-card">
            <h4 class="color-title">🎨 调色建议</h4>
            <div class="color-palette">
              <div v-for="(color, index) in retouchResult.colorPalette" :key="index" class="color-item">
                <div class="color-swatch" :style="{ background: color.hex }"></div>
                <span class="color-name">{{ color.name }}</span>
                <span class="color-hex">{{ color.hex }}</span>
              </div>
            </div>
            <div class="color-tips">
              <p><strong>白平衡调整：</strong>{{ retouchResult.whiteBalance }}</p>
              <p><strong>色调曲线：</strong>{{ retouchResult.toneCurve }}</p>
            </div>
          </el-card>
        </div>
        
        <el-card class="techniques-card">
          <h4 class="techniques-title">💡 常用修图技巧</h4>
          <div class="techniques-list">
            <div v-for="(technique, index) in retouchResult.techniques" :key="index" class="technique-item">
              <div class="technique-icon">{{ technique.icon }}</div>
              <div class="technique-content">
                <h5>{{ technique.title }}</h5>
                <p>{{ technique.description }}</p>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
    
    <div class="history-section">
      <div class="section-header">
        <h3>📸 历史记录</h3>
        <span class="total-count">共 {{ totalElements }} 条记录</span>
      </div>
      <div class="history-list">
        <div v-for="item in historyList" :key="item.id" class="history-item">
          <div @click="viewHistory(item)" class="history-click-area">
            <div class="history-image">
              <img :src="item.imageUrl" :alt="item.name" />
            </div>
            <div class="history-info">
              <span class="history-name">{{ item.name }}</span>
              <span class="history-date">{{ item.date }}</span>
            </div>
            <div class="history-overlay">
              <span class="view-text">查看详情</span>
            </div>
          </div>
          <button class="delete-btn" @click.stop="deleteRecord(item.id)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
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
    
    <div class="tips-section">
      <h3>📖 修图知识</h3>
      <div class="tips-cards">
        <div class="tip-card">
          <h4>🎯 人像修图流程</h4>
          <ol>
            <li>基础校正：白平衡、曝光、对比度</li>
            <li>肤色调整：磨皮、美白、去瑕疵</li>
            <li>五官精修：眼睛、鼻子、嘴唇</li>
            <li>色调风格：日系、欧美、复古等</li>
          </ol>
        </div>
        <div class="tip-card">
          <h4>🌄 风光修图流程</h4>
          <ol>
            <li>镜头校正：畸变、暗角、色差</li>
            <li>曝光合成：包围曝光HDR</li>
            <li>色调分级：天空、地面、前景</li>
            <li>细节增强：清晰度、纹理、降噪</li>
          </ol>
        </div>
        <div class="tip-card">
          <h4>🎨 调色技巧</h4>
          <ol>
            <li>色轮理论：互补色、邻近色搭配</li>
            <li>色彩分级：分离色调、曲线调色</li>
            <li>LUT预设：使用专业调色预设</li>
            <li>全局协调：保持画面色彩和谐</li>
          </ol>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { getRetouchSuggestions, getRetouchHistory, deleteRetouchRecord, getRetouchDetail } from '../../api/retouch'
import { ElMessageBox, ElMessage } from 'element-plus'

const fileInput = ref(null)
const resultCanvas = ref(null)
const uploadedFile = ref(null)
const previewUrl = ref('')
const photoDescription = ref('')
const isAnalyzing = ref(false)
const retouchResult = ref(null)
const comparisonPosition = ref(50)
const isDragging = ref(false)
const historyList = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const totalElements = ref(0)
const totalPages = ref(0)

const defaultResult = {
  objective: '',
  afterImage: '',
  steps: [],
  recommendedTools: [
    {
      id: 1,
      name: 'Adobe Lightroom',
      icon: '🖥️',
      description: '专业RAW格式处理，适合批量修图'
    },
    {
      id: 2,
      name: 'Adobe Photoshop',
      icon: '🎨',
      description: '强大的像素级编辑，适合精细修图'
    },
    {
      id: 3,
      name: '醒图',
      icon: '📱',
      description: '手机端优秀修图App，滤镜丰富'
    },
    {
      id: 4,
      name: 'Snapseed',
      icon: '🌸',
      description: 'Google出品，功能强大且免费'
    }
  ],
  colorPalette: [
    { name: '肤色', hex: '#f5d6c8' },
    { name: '暖白', hex: '#fff8f0' },
    { name: '暖黄', hex: '#ffd966' },
    { name: '浅棕', hex: '#c9a962' },
    { name: '深红', hex: '#c75b7a' },
    { name: '暗蓝', hex: '#2d4a53' }
  ],
  whiteBalance: '根据照片情况调整白平衡，还原真实色彩',
  toneCurve: '建议使用S型曲线，增加对比度和层次感',
  techniques: [
    {
      title: '频率分离',
      icon: '🔄',
      description: '将图片分解为高频（细节）和低频（色调）两个图层，分别进行处理，既能磨皮又能保留皮肤纹理。'
    },
    {
      title: 'D&B（加深减淡）',
      icon: '🎨',
      description: '通过加深和减淡工具，手动调整面部光影，塑造立体感，让五官更精致。'
    },
    {
      title: '色彩分级',
      icon: '🌈',
      description: '分别调整阴影、中间调、高光的色相和饱和度，创造独特的色调风格。'
    },
    {
      title: '蒙版技巧',
      icon: '🔍',
      description: '使用蒙版精确控制调整范围，让修图更加自然，避免过度处理。'
    }
  ]
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    uploadedFile.value = file
    previewUrl.value = URL.createObjectURL(file)
  }
}

const removeFile = () => {
  uploadedFile.value = null
  previewUrl.value = ''
  photoDescription.value = ''
  retouchResult.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDate = (date) => {
  if (!date) return ''
  let dateStr = date
  if (typeof date === 'object') {
    dateStr = date.toISOString ? date.toISOString() : String(date)
  } else if (typeof date !== 'string') {
    dateStr = String(date)
  }
  return dateStr.substring(0, 19).replace('T', ' ')
}

const getStepClass = (index) => {
  const classes = ['step-first', 'step-second', 'step-third', 'step-fourth', 'step-fifth']
  return classes[index] || 'step-other'
}

const analyzePhoto = async () => {
  if (!uploadedFile.value) return
  isAnalyzing.value = true
  
  try {
    const response = await getRetouchSuggestions(uploadedFile.value, photoDescription.value)
    
    retouchResult.value = {
      ...defaultResult,
      ...response,
      afterImage: previewUrl.value
    }
    
    await nextTick()
    renderFilteredImage()
  } catch (error) {
    console.error('分析失败:', error)
    retouchResult.value = {
      ...defaultResult,
      objective: '分析失败，请重试',
      afterImage: previewUrl.value
    }
  } finally {
    isAnalyzing.value = false
  }
}

const renderFilteredImage = () => {
  if (!resultCanvas.value || !previewUrl.value || !retouchResult.value?.imageParams) return
  
  const canvas = resultCanvas.value
  const ctx = canvas.getContext('2d')
  const img = new Image()
  
  img.onload = () => {
    canvas.width = img.width
    canvas.height = img.height
    
    const params = retouchResult.value.imageParams
    const filterString = buildFilterString(params)
    
    ctx.filter = filterString
    ctx.drawImage(img, 0, 0)
    ctx.filter = 'none'
  }
  
  img.src = previewUrl.value
}

const buildFilterString = (params) => {
  const filters = []
  
  if (params.brightness !== undefined && params.brightness !== 1) {
    filters.push(`brightness(${params.brightness})`)
  }
  
  if (params.contrast !== undefined && params.contrast !== 1) {
    filters.push(`contrast(${params.contrast})`)
  }
  
  if (params.saturation !== undefined && params.saturation !== 1) {
    filters.push(`saturate(${params.saturation})`)
  }
  
  if (params.hueRotate !== undefined && params.hueRotate !== 0) {
    filters.push(`hue-rotate(${params.hueRotate}deg)`)
  }
  
  if (params.blur !== undefined && params.blur > 0) {
    filters.push(`blur(${params.blur}px)`)
  }
  
  if (params.sepia !== undefined && params.sepia > 0) {
    filters.push(`sepia(${params.sepia})`)
  }
  
  if (params.grayscale !== undefined && params.grayscale > 0) {
    filters.push(`grayscale(${params.grayscale})`)
  }
  
  if (params.opacity !== undefined && params.opacity !== 1) {
    filters.push(`opacity(${params.opacity})`)
  }
  
  return filters.join(' ') || 'none'
}

const handleMouseMove = (event) => {
  if (!isDragging.value) return
  
  const container = event.currentTarget
  const rect = container.getBoundingClientRect()
  const x = event.clientX - rect.left
  const percentage = (x / rect.width) * 100
  
  comparisonPosition.value = Math.max(0, Math.min(100, percentage))
}

watch(() => retouchResult.value, () => {
  nextTick(() => {
    renderFilteredImage()
  })
}, { deep: true })

const viewHistory = async (item) => {
  previewUrl.value = item.imageUrl
  
  try {
    const response = await getRetouchDetail(item.id)
    
    retouchResult.value = {
      ...defaultResult,
      ...response,
      afterImage: item.imageUrl
    }
    
    await nextTick()
    renderFilteredImage()
  } catch (error) {
    console.error('加载历史详情失败:', error)
    retouchResult.value = {
      ...defaultResult,
      objective: item.suggestions || '查看历史修图记录',
      afterImage: item.imageUrl
    }
  }
  
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const loadHistory = async () => {
  try {
    const response = await getRetouchHistory(currentPage.value - 1, pageSize.value)
    historyList.value = (response.content || []).map(item => ({
      id: item.id,
      name: item.fileName || '修图记录',
      imageUrl: item.imageUrl || '/api/retouch/image/' + item.id,
      date: formatDate(item.createdAt)
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
      '确定要删除这条记录吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteRetouchRecord(id)
    ElMessage.success('删除成功')
    loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.retouch-page {
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
  border: 2px dashed #ddd;
  border-radius: 10px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.upload-area:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.upload-area.has-file {
  border: none;
  padding: 0;
}

.file-input {
  display: none;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.upload-placeholder p {
  margin: 5px 0;
  font-size: 15px;
  color: #666;
}

.upload-hint {
  font-size: 13px;
  color: #999 !important;
}

.upload-preview {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
}

.upload-preview img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
}

.preview-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 10px 15px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.remove-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.remove-btn:hover {
  background: rgba(255, 0, 0, 0.8);
}

.description-input {
  margin-top: 20px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.result-section {
  margin-bottom: 30px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-header h3 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.result-date {
  font-size: 14px;
  color: #999;
}

.comparison-section {
  margin-bottom: 20px;
}

.comparison-card {
  border-radius: 12px;
}

.comparison-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.comparison-header h4 {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.comparison-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.control-label {
  font-size: 13px;
  color: #666;
}

.comparison-view {
  margin-top: 15px;
}

.comparison-container {
  position: relative;
  width: 100%;
  height: 400px;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5f5;
}

.original-image,
.retouched-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.original-image img,
.retouched-image img,
.result-canvas {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
}

.result-canvas {
  display: block;
}

.image-label {
  position: absolute;
  top: 15px;
  padding: 5px 12px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 13px;
  border-radius: 6px;
}

.original-image .image-label {
  left: 15px;
}

.retouched-image .image-label {
  right: 15px;
}

.comparison-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 4px;
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  cursor: col-resize;
  transform: translateX(-50%);
  z-index: 10;
}

.handle-line {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  border: 2px solid white;
  border-radius: 50%;
}

.handle-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.suggestions-section {
  margin-top: 20px;
}

.suggestion-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.suggestion-title {
  font-size: 16px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 15px;
}

.suggestion-content {
  font-size: 15px;
  line-height: 1.7;
  color: #666;
}

.steps-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.steps-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.step-item {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
}

.step-first {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
}

.step-second {
  background: linear-gradient(135deg, #ffa502 0%, #ff9f43 100%);
}

.step-third {
  background: linear-gradient(135deg, #2ed573 0%, #1dd1a1 100%);
}

.step-fourth {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.step-fifth {
  background: linear-gradient(135deg, #a29bfe 0%, #6c5ce7 100%);
}

.step-other {
  background: #999;
}

.step-content {
  flex: 1;
}

.step-content h5 {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.step-content p {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin: 0 0 10px;
}

.step-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tool-tag {
  padding: 4px 10px;
  background: #e8f0fe;
  color: #667eea;
  border-radius: 8px;
  font-size: 12px;
}

.step-icon {
  font-size: 24px;
}

.tools-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.tools-card,
.color-card {
  border-radius: 12px;
}

.tools-title,
.color-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.tool-item {
  text-align: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.tool-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.tool-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 4px;
}

.tool-desc {
  font-size: 12px;
  color: #999;
}

.color-palette {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
}

.color-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.color-swatch {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  margin-bottom: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.color-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.color-hex {
  font-size: 11px;
  color: #999;
  font-family: 'Courier New', monospace;
}

.color-tips p {
  font-size: 14px;
  color: #666;
  margin: 5px 0;
}

.techniques-card {
  border-radius: 12px;
}

.techniques-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.techniques-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 15px;
}

.technique-item {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
}

.technique-icon {
  font-size: 28px;
}

.technique-content h5 {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.technique-content p {
  font-size: 13px;
  line-height: 1.5;
  color: #666;
  margin: 0;
}

.history-section {
  margin-top: 30px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.history-section h3 {
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
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 15px;
}

.history-item {
  position: relative;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.history-click-area {
  cursor: pointer;
}

.history-image {
  height: 150px;
  overflow: hidden;
}

.history-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.history-info {
  padding: 12px;
}

.history-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.history-date {
  font-size: 12px;
  color: #999;
}

.history-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.history-click-area:hover .history-overlay {
  opacity: 1;
}

.view-text {
  color: white;
  font-size: 14px;
  font-weight: 600;
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

.tips-section {
  margin-top: 30px;
}

.tips-section h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.tips-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.tip-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.tip-card h4 {
  font-size: 16px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 15px;
}

.tip-card ol {
  margin: 0;
  padding-left: 20px;
}

.tip-card li {
  font-size: 14px;
  line-height: 1.8;
  color: #666;
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .tools-section {
    grid-template-columns: 1fr;
  }
  
  .comparison-container {
    height: 250px;
  }
  
  .step-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .step-number {
    align-self: flex-start;
  }
}
</style>