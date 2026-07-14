<template>
  <div class="gallery-page">
    <section class="page-hero">
      <h1 class="page-title">🏆 作品赏析</h1>
      <p class="page-subtitle">精选摄影作品深度分析，学习大师的构图、用光与色彩运用思路</p>
    </section>

    <section class="filter-section">
      <div class="filter-row">
        <span class="filter-label">筛选分类：</span>
        <el-radio-group v-model="filterCategory" size="default">
          <el-radio-button label="all">全部作品</el-radio-button>
          <el-radio-button label="portrait">人像</el-radio-button>
          <el-radio-button label="landscape">风光</el-radio-button>
          <el-radio-button label="street">街头</el-radio-button>
          <el-radio-button label="night">夜景</el-radio-button>
          <el-radio-button label="architecture">建筑</el-radio-button>
        </el-radio-group>
      </div>
    </section>

    <section class="gallery-grid">
      <div v-for="work in filteredWorks" :key="work.id" class="work-card" @click="openWork(work)">
        <div class="work-image" :style="{ backgroundImage: `url(${work.image})` }">
          <div class="work-overlay">
            <el-icon><ZoomIn /></el-icon>
            <span>查看详情</span>
          </div>
          <div class="work-badge">{{ work.award }}</div>
        </div>
        <div class="work-info">
          <h3 class="work-title">{{ work.title }}</h3>
          <p class="work-author">📸 {{ work.author }}</p>
          <div class="work-params-preview">
            <span class="param-chip">{{ work.params.camera }}</span>
            <span class="param-chip">{{ work.params.aperture }} · {{ work.params.shutter }} · ISO{{ work.params.iso }}</span>
          </div>
          <div class="work-rating">
            <el-rate :model-value="work.rating" disabled size="small" />
            <span class="rating-text">{{ work.rating.toFixed(1) }} 分</span>
          </div>
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" width="900px" class="work-dialog" destroy-on-close>
      <template #header>
        <div class="dialog-header">
          <h2 class="dialog-title">{{ currentWork?.title }}</h2>
          <div class="dialog-meta">
            <span>作者：{{ currentWork?.author }}</span>
            <el-tag type="warning">{{ currentWork?.award }}</el-tag>
          </div>
        </div>
      </template>

      <div v-if="currentWork" class="dialog-content">
        <div class="work-full-image">
          <img :src="currentWork.image" :alt="currentWork.title" />
        </div>

        <div class="work-details">
          <div class="section">
            <h3 class="section-title">📊 拍摄参数</h3>
            <div class="params-table">
              <div class="param-row" v-for="(val, key) in paramLabels" :key="key">
                <span class="param-label">{{ val }}</span>
                <span class="param-value">{{ currentWork.params[key] || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="section">
            <h3 class="section-title">📝 创作自述</h3>
            <p class="content-text">{{ currentWork.description }}</p>
          </div>

          <div class="section">
            <h3 class="section-title">🎯 专家点评（综合评分 {{ currentWork.rating }} / 10）</h3>
            <div class="ratings-grid">
              <div v-for="score in currentWork.scoreBreakdown" :key="score.name" class="rating-item">
                <div class="rating-top">
                  <span class="rating-name">{{ score.name }}</span>
                  <span class="rating-num">{{ score.value }}/10</span>
                </div>
                <el-progress :percentage="score.value * 10" :color="score.color" :stroke-width="8" />
              </div>
            </div>
          </div>

          <div class="section">
            <h3 class="section-title">💡 亮点分析</h3>
            <ul class="highlight-list">
              <li v-for="(h, idx) in currentWork.highlights" :key="idx">
                <span class="hl-icon">✅</span>
                <span>{{ h }}</span>
              </li>
            </ul>
          </div>

          <div class="section">
            <h3 class="section-title">🔄 可改进之处</h3>
            <ul class="improve-list">
              <li v-for="(i, idx) in currentWork.improvements" :key="idx">
                <span class="im-icon">💬</span>
                <span>{{ i }}</span>
              </li>
            </ul>
          </div>

          <div class="section">
            <h3 class="section-title">📚 学习要点</h3>
            <div class="takeaways">
              <div class="takeaway-card" v-for="(t, idx) in currentWork.takeaways" :key="idx">
                <div class="takeaway-num">{{ idx + 1 }}</div>
                <p class="takeaway-text">{{ t }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ZoomIn } from '@element-plus/icons-vue'

const filterCategory = ref('all')
const dialogVisible = ref(false)
const currentWork = ref(null)

const paramLabels = {
  camera: '相机机身',
  lens: '镜头',
  focal: '焦距',
  aperture: '光圈',
  shutter: '快门速度',
  iso: 'ISO 感光度',
  wb: '白平衡',
  flash: '闪光灯'
}

const createImageUrl = (prompt) => {
  return `https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=${encodeURIComponent(prompt)}&image_size=landscape_16_9`
}

const works = ref([
  {
    id: 1,
    category: 'portrait',
    award: '2024 人像摄影大赛 金奖',
    title: '黄昏下的少女',
    author: '林慕青',
    rating: 9.2,
    image: createImageUrl('beautiful cinematic portrait photography of a young woman standing in a golden wheat field during sunset golden hour, warm backlight creating glowing hair rim light, soft bokeh background, professional medium format portrait photography, emotional mood, depth of field'),
    params: {
      camera: 'Sony A7R V',
      lens: 'Sony FE 85mm f/1.4 GM',
      focal: '85mm',
      aperture: 'f/2.0',
      shutter: '1/1000s',
      iso: '100',
      wb: '5200K（暖调偏移）',
      flash: '未使用'
    },
    description: '这张照片拍摄于一个夏末的黄昏，地点在北京郊外的一片麦田。模特是我的好朋友，那天的夕阳特别美，天空从金色过渡到粉紫色。我让她背对太阳站着，这样逆光勾勒出了她发丝的金色轮廓，同时用了一块银色反光板在正前方补光，让面部不至于太暗。按下快门的瞬间，一阵微风吹过，她的裙摆和发丝都轻轻飘动，就定格在了这个瞬间。',
    scoreBreakdown: [
      { name: '构图', value: 9, color: '#67c23a' },
      { name: '光线', value: 10, color: '#409eff' },
      { name: '色彩', value: 9, color: '#e6a23c' },
      { name: '对焦', value: 9, color: '#f56c6c' },
      { name: '内容表达', value: 9, color: '#909399' }
    ],
    highlights: [
      '完美的逆光运用：光源从后方 45 度打来，勾勒出清晰的金色发丝光（Hair Light），同时保留了面部层次',
      '反光板补光恰到好处：正面补光自然柔和，没有出现反光板的镜面高光或"死白脸"',
      '人物位置遵循三分法：眼睛位于右上黄金分割点，左侧留有足够的"视线空间"',
      '快门速度选择得当：1/1000s 凝固了飘动的发丝和裙摆，既有动态又不模糊'
    ],
    improvements: [
      '裙摆底部略微超出画面下方，如果退半步或用稍广一点的焦段可以完整保留全身比例',
      '背景中间远处隐约有一个电线杆（虽然虚化了），如果机位稍左移一步可以完美避开'
    ],
    takeaways: [
      '逆光人像不一定非要"剪影"，用反光板或闪光灯补光，可以同时得到漂亮的轮廓光和清晰的面部细节',
      '85mm 人像焦段配合 f/2.0 光圈，既能获得优秀的背景虚化，又能保证眼睛到脸颊的景深足够清晰',
      '拍摄时多留意"决定性瞬间"：风、微笑、眼神——这些可遇不可求的瞬间往往让一张好照片变成一张佳作'
    ]
  },
  {
    id: 2,
    category: 'landscape',
    award: '国家地理年度风光摄影 入围',
    title: '雪山之镜',
    author: '张远山',
    rating: 9.5,
    image: createImageUrl('epic landscape photography of snow capped mountain range with perfect reflection in a calm alpine lake at sunrise, dramatic pink and orange clouds, golden first light on peaks, crystal clear water reflection, wide angle, medium format, professional nature photography'),
    params: {
      camera: 'Fujifilm GFX 100 II',
      lens: 'GF 23mm f/4 R LM WR',
      focal: '23mm（等效 18mm）',
      aperture: 'f/11',
      shutter: '1.3s',
      iso: '100',
      wb: '自动（后期微调）',
      flash: '未使用'
    },
    description: '这张照片拍于西藏某圣湖，凌晨 4 点就从帐篷出发到达机位，等了整整 3 个小时才等到日出。最神奇的是当天完全无风，湖面像镜子一样完美倒映出雪山和朝霞。为了保证从前景到远山完全清晰，我用了 f/11 小光圈 + 1.3 秒长曝光，配合实时取景放大对焦，焦点落在大约画面下三分之一的湖面上。三脚架是必须的，我用了一款自重 4kg 的碳纤维脚架，加上 10 斤的摄影包挂在挂钩上增加稳定性。',
    scoreBreakdown: [
      { name: '构图', value: 10, color: '#67c23a' },
      { name: '光线', value: 10, color: '#409eff' },
      { name: '色彩', value: 9.5, color: '#e6a23c' },
      { name: '对焦', value: 9, color: '#f56c6c' },
      { name: '内容表达', value: 9.5, color: '#909399' }
    ],
    highlights: [
      '对称构图的完美运用：上半部分是雪山与天空，下半部分是倒影，地平线几乎完美地放在画面中心轴线上',
      '第一缕金光照亮雪峰尖顶，大面积冷蓝色的阴影与暖金色的阳光形成漂亮的冷暖对比',
      '前景的细小碎石与水草没有被忽略，虽然是广角但近景依然有丰富细节，增加了画面的层次感',
      '曝光精准：高光没有溢出，雪山的雪保留了白色纹理而不是一片死白'
    ],
    improvements: [
      '如果有一片云或一只鸟出现在天空上部的空白处，画面会更有生气（但这是可遇不可求的）',
      '长曝光时水面有几个微小的涟漪让倒影略有波动，虽然不影响整体但完美主义者会介意'
    ],
    takeaways: [
      '风光摄影三分靠拍七分靠"等"：早到机位、耐心等待、关注天气预报和日出日落时间',
      '完美对称构图 + 完美倒影是"出片神器"组合，但前提是当天必须绝对无风——这也是为什么很多风光摄影师凌晨就出门',
      '广角风景的对焦技巧：不要对焦在无限远，要对焦在超焦距点（大约画面下 1/3 处），配合 f/8-f/11 才能获得从前景到远景全实的效果'
    ]
  },
  {
    id: 3,
    category: 'street',
    award: '街拍中国 年度十佳',
    title: '雨中的红伞',
    author: '陈一格',
    rating: 9.0,
    image: createImageUrl('cinematic street photography on a rainy day in Shanghai old street, a person holding a vibrant red umbrella walking, wet pavement reflection of neon lights, black and white desaturated except red color accent, moody atmospheric, shallow depth of field, Leica photography style'),
    params: {
      camera: 'Leica Q3',
      lens: 'Summilux 28mm f/1.7 内置',
      focal: '28mm',
      aperture: 'f/2.8',
      shutter: '1/250s',
      iso: '800',
      wb: '自动',
      flash: '未使用'
    },
    description: '这张照片拍于上海的一场春雨。那天我拿着相机在弄堂口的咖啡馆躲雨，看到远处一把红色的伞慢慢走过青石板路，路面的倒影很漂亮，于是把相机设为全自动 + 区域对焦，等她走到我预估的"三分点"位置时，盲拍了三张——这张就是其中之一。后期我做了"单色留红"处理，把除了红色以外的饱和度都降到最低，突出这把雨伞作为视觉焦点。',
    scoreBreakdown: [
      { name: '构图', value: 9, color: '#67c23a' },
      { name: '光线', value: 8.5, color: '#409eff' },
      { name: '色彩', value: 10, color: '#e6a23c' },
      { name: '对焦', value: 8.5, color: '#f56c6c' },
      { name: '内容表达', value: 9.5, color: '#909399' }
    ],
    highlights: [
      '"决定性瞬间"的经典示范：撑伞的人正好走到了左下黄金分割点，雨伞与对面的店铺门面形成了对角呼应',
      '色彩对比运用极致：黑白灰的背景中唯一一抹红色，雨伞立刻成为无法忽视的视觉中心',
      '雨后的湿润地面反射了灯光和伞的倒影，让画面的下半部分不再空洞，增加了层次和氛围感',
      '28mm 的视角既包含了足够的环境信息，又不会因为太广而让主体过小'
    ],
    improvements: [
      '对焦稍微偏前了一点，雨伞最清晰而人物稍微有点虚，再往后偏 10cm 就完美了',
      '画面右上角有一小块半透明的雨渍挡在镜头前，如果当时擦一下镜头会更干净'
    ],
    takeaways: [
      '街头摄影的核心不是"拍清楚一张脸"，而是"捕捉一个故事感的瞬间"——人物、环境、氛围三者缺一不可',
      '28mm 是街拍的黄金焦段：近可拍人，退可拍景，既有代入感又不会因靠太近而冒犯对方',
      '"先设置好参数，再等待时机"：好的街头摄影师不是看到才去调参数，而是参数早就调好，眼到手到，一瞬间完成'
    ]
  },
  {
    id: 4,
    category: 'night',
    award: '夜空摄影大赛 银奖',
    title: '银河下的守望者',
    author: '赵星辰',
    rating: 9.3,
    image: createImageUrl('stunning astrophotography of a person standing under a bright milky way galaxy arch over mountains, long exposure night photography, dark foreground with silhouette, vivid purple and blue galactic core, stars and shooting stars, wide angle landscape photography'),
    params: {
      camera: 'Canon EOS R5',
      lens: 'RF 15-35mm f/2.8 L IS USM',
      focal: '15mm',
      aperture: 'f/2.8',
      shutter: '20s',
      iso: '6400',
      wb: '4000K',
      flash: 'LED 补光（前景）'
    },
    description: '这张拍于海拔 4000 米的川西高原。为了拍银河，我们在雪山脚下扎营了两晚，第一晚云太厚什么都没拍到。第二天凌晨 2 点云散了，银河正好从山后升起，形成了一个拱形。我让朋友站在我前方约 10 米的一块石头上，手里拿着一个小小的营灯，这样既能拍出人物的轮廓，又不会让灯光过曝。单张参数是 15mm f/2.8 20s ISO 6400，最终是 20 张堆栈降噪 + 一张前景单独长曝合成，所以人物和前景的石头的细节比单张要好很多。',
    scoreBreakdown: [
      { name: '构图', value: 9.5, color: '#67c23a' },
      { name: '光线', value: 9, color: '#409eff' },
      { name: '色彩', value: 9.5, color: '#e6a23c' },
      { name: '对焦', value: 9.5, color: '#f56c6c' },
      { name: '内容表达', value: 9, color: '#909399' }
    ],
    highlights: [
      '银河拱门完美地框住了人物和雪山，形成了天然的"框架构图"，视觉直接落在人物身上',
      '人物尺度参照物运用得当：渺小的人形对比壮阔的银河，瞬间传达出"宇宙浩瀚人类渺小"的意境',
      '前景补光技巧高明：手持营灯微微照亮石头和人物，既不是全黑剪影，也不破坏暗夜的氛围',
      '堆栈降噪效果出色：银河细节清晰，暗部噪点控制极佳，单张 ISO 6400 却几乎看不到颗粒'
    ],
    improvements: [
      '人物手中的灯稍微亮了一点，如果把灯调暗一档或者放在侧面，会更自然',
      '地平线轻微地有一点不水平，后期旋转 0.5 度就会更完美'
    ],
    takeaways: [
      '银河拍摄五要素：晴天无月 + 远离城市光污染 + 广角大光圈镜头 + 高感好的机身 + 稳固三脚架',
      '"人物 + 大风景"的构图比例参考：人物不要超过画面的 1/15，太小看不见、太大破坏风景感',
      '不要执着于"单张直出"：堆栈降噪（20-50 张同参数叠加）是现代星空摄影的标准流程，可以让 ISO 6400 的画面看起来像 ISO 400'
    ]
  },
  {
    id: 5,
    category: 'architecture',
    award: '建筑摄影年度作品 提名',
    title: '几何与光',
    author: '苏明远',
    rating: 8.9,
    image: createImageUrl('minimalist architectural photography of a modern concrete building with strong geometric lines and dramatic shadows, black and white photography, contrasty hard light, repeating patterns, perspective lines, Leica architecture photography style, clean composition'),
    params: {
      camera: 'Sony A7 IV',
      lens: 'Sony FE 24-70mm f/2.8 GM II',
      focal: '35mm',
      aperture: 'f/8',
      shutter: '1/500s',
      iso: '200',
      wb: '日光 5500K',
      flash: '未使用'
    },
    description: '这张照片拍摄于某美术馆的外立面。正午的阳光从正上方射下，在清水混凝土的几何结构上形成了极其锐利的明暗分割。为了让建筑线条不产生透视畸变，我找了一个完全正对着的机位，用水平仪反复确认相机既不俯仰也不歪斜。后期转成了黑白，并提亮了暗部、加深了亮部的反差，让"几何分割"的感觉更加纯粹。',
    scoreBreakdown: [
      { name: '构图', value: 10, color: '#67c23a' },
      { name: '光线', value: 9.5, color: '#409eff' },
      { name: '色彩', value: 8, color: '#e6a23c' },
      { name: '对焦', value: 9.5, color: '#f56c6c' },
      { name: '内容表达', value: 8.5, color: '#909399' }
    ],
    highlights: [
      '绝对严谨的透视控制：建筑的水平线和垂直线都完美平行于画面边缘，没有任何"下宽上窄"的透视变形',
      '正午硬光反而成为加分项：黑白影调下，光影形成的三角形、矩形分割本身就是作品的主题',
      '画面元素极简：只有三种明度（白、灰、黑）和两种线条（横、竖），形式感极强',
      '对焦在无限远 + f/8，整幅画面从近到远都锐利，建筑纹理清晰可见'
    ],
    improvements: [
      '画面右下角玻璃上有一个小小的反光（可能是拍摄者自身），可以通过后期修掉或机位略移来避免',
      '如果等到一个人影正好站在右下角的光斑里，会为冰冷的建筑增加对比和叙事性'
    ],
    takeaways: [
      '建筑摄影的第一要务是"横平竖直"：能上移轴镜头（Tilt-Shift）最好，没有的话就尽量正对建筑中心、后期用透视校正工具修正',
      '"不好看"的正午顶光在建筑摄影中反而好看：只要建筑有足够立体的几何结构，硬光就是最好的雕刻刀',
      '黑白转换适合建筑摄影：剥离色彩干扰后，线条、光影、纹理三种元素会被凸显出来，让画面更具"高级感"'
    ]
  },
  {
    id: 6,
    category: 'portrait',
    award: '新锐摄影奖 年度作品',
    title: '她的房间',
    author: '周小雅',
    rating: 8.8,
    image: createImageUrl('beautiful cinematic indoor natural light portrait of a young woman sitting by a large window reading a book, soft diffused daylight, cozy atmosphere, film grain aesthetic, warm tones, shallow depth of field, 35mm lifestyle photography'),
    params: {
      camera: 'Nikon Z f',
      lens: 'Nikkor Z 35mm f/1.4 S',
      focal: '35mm',
      aperture: 'f/2.0',
      shutter: '1/125s',
      iso: '1600',
      wb: '阴天 6000K',
      flash: '未使用'
    },
    description: '这是一组拍摄朋友在家中的日常作品。那天是阴天，窗外的光线非常柔和均匀，像一块巨大的柔光箱。我让她就像平时一样看书、喝茶、翻唱片，完全不要看镜头，我在一旁安静地拍。构图上刻意把她放在画面的一角，留出大部分空间给她生活的环境——书、植物、墙上的唱片，这些环境信息比拍一张大头照更能体现"她是谁"。后期只做了轻微的调色，加了一点点胶片颗粒感模拟胶片。',
    scoreBreakdown: [
      { name: '构图', value: 8.5, color: '#67c23a' },
      { name: '光线', value: 10, color: '#409eff' },
      { name: '色彩', value: 9, color: '#e6a23c' },
      { name: '对焦', value: 8.5, color: '#f56c6c' },
      { name: '内容表达', value: 9, color: '#909399' }
    ],
    highlights: [
      '光线控制满分：阴天的侧窗光均匀柔和，人物面部从左到右有非常自然的明暗过渡，没有任何硬阴影',
      '"环境人像"的正确打开方式：主体只占画面 1/4，但周围的书籍、绿植、老家具讲述了更完整的故事',
      '35mm 焦段的正确运用：既能让环境入镜，又不会产生广角近大远小的脸部畸变',
      '模特状态自然：不看镜头、专注于自己的事，这才是一组"生活感"照片的灵魂'
    ],
    improvements: [
      '背景的唱片架稍微有点乱，几本书可以整理得更有秩序一些，让画面更整洁',
      'ISO 1600 在高像素机身上如果能再降一点（比如补一块反光板在正面），画质会更干净'
    ],
    takeaways: [
      '拍人像不一定需要阳光灿烂的日子：阴天散射光、下雨前的柔和天光，都是拍安静感人像的绝佳时机',
      '拍"生活感"人像的秘诀是——不要让模特"摆姿势"。让她做一件具体的事：看书、泡茶、听音乐，动作越具体，状态越自然',
      '环境人像的构图思路：人物是"点"，环境是"面"，点面结合，故事才完整'
    ]
  }
])

const filteredWorks = computed(() => {
  if (filterCategory.value === 'all') return works.value
  return works.value.filter(w => w.category === filterCategory.value)
})

const openWork = (work) => {
  currentWork.value = work
  dialogVisible.value = true
}
</script>

<style scoped>
.gallery-page {
  padding: 30px 40px;
  max-width: 1300px;
  margin: 0 auto;
}

.page-hero {
  text-align: center;
  padding: 40px 20px 50px;
  background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
  border-radius: 16px;
  color: white;
  margin-bottom: 30px;
}

.page-title {
  font-size: 36px;
  margin: 0 0 12px;
}

.page-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.filter-section {
  background: white;
  padding: 18px 24px;
  border-radius: 12px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #555;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 26px;
}

.work-card {
  background: white;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.07);
  cursor: pointer;
  transition: all 0.3s ease;
}

.work-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 30px rgba(15, 32, 39, 0.2);
}

.work-image {
  position: relative;
  width: 100%;
  height: 220px;
  background-size: cover;
  background-position: center;
}

.work-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.work-card:hover .work-overlay {
  opacity: 1;
}

.work-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(230, 162, 60, 0.95);
  color: white;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  max-width: 80%;
  line-height: 1.4;
}

.work-info {
  padding: 18px 20px 22px;
}

.work-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px;
  color: #1a1a1a;
}

.work-author {
  font-size: 13px;
  color: #888;
  margin: 0 0 12px;
}

.work-params-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.param-chip {
  background: #f0f3fa;
  color: #667eea;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 500;
}

.work-rating {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-text {
  font-size: 13px;
  color: #e6a23c;
  font-weight: 600;
}

.dialog-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dialog-title {
  font-size: 22px;
  margin: 0;
  color: #1a1a1a;
}

.dialog-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #777;
}

.work-full-image {
  width: 100%;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 24px;
  background: #000;
}

.work-full-image img {
  width: 100%;
  height: auto;
  display: block;
}

.work-details {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.section-title {
  font-size: 16px;
  margin: 0 0 14px;
  color: #333;
  padding-left: 10px;
  border-left: 3px solid #2c5364;
}

.params-table {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 24px;
  background: #f7f8fa;
  border-radius: 10px;
  padding: 16px 20px;
}

.param-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px dashed #e4e6ea;
  font-size: 13.5px;
}

.param-row:last-child,
.param-row:nth-last-child(2):nth-child(odd) {
  border-bottom: none;
}

.param-label {
  color: #888;
}

.param-value {
  font-weight: 600;
  color: #333;
  text-align: right;
}

.content-text {
  font-size: 14.5px;
  line-height: 1.9;
  color: #444;
  margin: 0;
  text-indent: 2em;
}

.ratings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px 28px;
}

.rating-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rating-top {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
}

.rating-name {
  color: #555;
}

.rating-num {
  font-weight: 700;
  color: #333;
}

.highlight-list,
.improve-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.highlight-list li,
.improve-list li {
  display: flex;
  gap: 10px;
  font-size: 14px;
  line-height: 1.75;
  color: #444;
}

.hl-icon,
.im-icon {
  flex-shrink: 0;
}

.takeaways {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.takeaway-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: linear-gradient(135deg, #f5f7ff, #eef0fb);
  border-left: 4px solid #667eea;
  border-radius: 0 10px 10px 0;
  padding: 14px 18px;
}

.takeaway-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #667eea;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.takeaway-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}

@media (max-width: 768px) {
  .gallery-page {
    padding: 20px 16px;
  }

  .page-title {
    font-size: 26px;
  }

  .gallery-grid {
    grid-template-columns: 1fr;
  }

  .params-table {
    grid-template-columns: 1fr;
  }

  .work-dialog :deep(.el-dialog) {
    width: 95% !important;
  }
}
</style>
