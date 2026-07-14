<template>
  <div class="tips-page">
    <section class="page-hero">
      <h1 class="page-title">📝 摄影技巧库</h1>
      <p class="page-subtitle">按场景分类的实战技巧，从入门到进阶，让你轻松应对各种拍摄场景</p>
    </section>

    <section class="category-section">
      <div class="category-tabs">
        <button
          v-for="cat in categories"
          :key="cat.key"
          class="tab-btn"
          :class="{ active: activeCategory === cat.key }"
          @click="activeCategory = cat.key"
        >
          <span class="tab-icon">{{ cat.icon }}</span>
          <span class="tab-name">{{ cat.name }}</span>
          <el-badge :value="getCategoryCount(cat.key)" class="tab-count" :max="99" />
        </button>
      </div>
    </section>

    <section class="tips-grid">
      <div v-for="tip in filteredTips" :key="tip.id" class="tip-card">
        <div class="tip-header">
          <div class="tip-image" :style="{ backgroundImage: `url(${tip.cover})` }">
            <div class="tip-category-tag">{{ getCategoryName(tip.category) }}</div>
          </div>
          <div class="tip-level" :class="'level-' + tip.level">
            {{ tip.level === 1 ? '⭐ 新手' : tip.level === 2 ? '⭐⭐ 进阶' : '⭐⭐⭐ 高手' }}
          </div>
        </div>
        <div class="tip-body">
          <h3 class="tip-title">{{ tip.title }}</h3>
          <p class="tip-summary">{{ tip.summary }}</p>

          <el-collapse class="tip-collapse">
            <el-collapse-item name="1">
              <template #title>
                <span class="collapse-title">📖 查看详细技巧</span>
              </template>
              <div class="tip-details">
                <div class="detail-section">
                  <h4>🎯 核心要点</h4>
                  <ul class="point-list">
                    <li v-for="(point, idx) in tip.keyPoints" :key="idx">{{ point }}</li>
                  </ul>
                </div>
                <div class="detail-section">
                  <h4>📷 推荐参数</h4>
                  <div class="params-grid">
                    <div class="param-item">
                      <span class="param-label">模式</span>
                      <span class="param-value">{{ tip.params.mode }}</span>
                    </div>
                    <div class="param-item">
                      <span class="param-label">光圈</span>
                      <span class="param-value">{{ tip.params.aperture }}</span>
                    </div>
                    <div class="param-item">
                      <span class="param-label">快门</span>
                      <span class="param-value">{{ tip.params.shutter }}</span>
                    </div>
                    <div class="param-item">
                      <span class="param-label">ISO</span>
                      <span class="param-value">{{ tip.params.iso }}</span>
                    </div>
                    <div class="param-item">
                      <span class="param-label">焦段</span>
                      <span class="param-value">{{ tip.params.focal }}</span>
                    </div>
                    <div class="param-item">
                      <span class="param-label">测光</span>
                      <span class="param-value">{{ tip.params.metering }}</span>
                    </div>
                  </div>
                </div>
                <div class="detail-section" v-if="tip.mistakes && tip.mistakes.length">
                  <h4>⚠️ 常见误区</h4>
                  <ul class="mistake-list">
                    <li v-for="(m, idx) in tip.mistakes" :key="idx">{{ m }}</li>
                  </ul>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const activeCategory = ref('all')

const categories = [
  { key: 'all', name: '全部', icon: '🎯' },
  { key: 'portrait', name: '人像摄影', icon: '👤' },
  { key: 'landscape', name: '风光摄影', icon: '🏔' },
  { key: 'night', name: '夜景摄影', icon: '🌃' },
  { key: 'street', name: '街头摄影', icon: '🚶' },
  { key: 'sports', name: '运动摄影', icon: '⚽' },
  { key: 'food', name: '美食摄影', icon: '🍜' },
  { key: 'flower', name: '花卉微距', icon: '🌸' }
]

const COVER_IMAGES = {
  portrait: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=beautiful%20portrait%20photography%20of%20a%20young%20woman%20in%20golden%20hour%20soft%20light%20blurred%20bokeh%20background%20professional%20photography&image_size=landscape_16_9',
  landscape: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=beautiful%20mountain%20landscape%20photography%20sunrise%20golden%20light%20dramatic%20clouds%20lake%20reflection%20professional%20nature%20photography&image_size=landscape_16_9',
  night: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=stunning%20city%20night%20skyline%20photography%20with%20light%20trails%20and%20stars%20long%20exposure%20vivid%20colors%20professional&image_size=landscape_16_9',
  street: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=cinematic%20street%20photography%20rainy%20city%20night%20neon%20lights%20reflection%20pedestrians%20walking%20umbrella%20moody%20atmosphere&image_size=landscape_16_9',
  sports: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=dynamic%20sports%20photography%20of%20soccer%20player%20kicking%20ball%20dramatic%20action%20moment%20freeze%20motion%20professional%20sports%20photography&image_size=landscape_16_9',
  food: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=appetizing%20food%20photography%20of%20a%20gourmet%20dish%20beautiful%20plating%20soft%20natural%20lighting%20shallow%20depth%20of%20field%20professional&image_size=landscape_16_9',
  flower: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=stunning%20macro%20flower%20photography%20beautiful%20rose%20dewdrops%20extreme%20close%20up%20soft%20bokeh%20natural%20light%20vivid%20colors&image_size=landscape_16_9'
}

const tips = ref([
  {
    id: 1,
    category: 'portrait',
    level: 1,
    title: '黄金时刻人像拍摄',
    summary: '利用日出日落前后的黄金光线，拍出温暖柔和、皮肤质感绝佳的人像大片。',
    cover: COVER_IMAGES.portrait,
    keyPoints: [
      '选择日落前 1 小时或日出后 1 小时拍摄，光线角度低、色温暖、质感柔',
      '让模特侧对光源，让光线勾勒脸部轮廓，避免正对光线导致眯眼',
      '背景选择简洁的场景：天空、花丛、纯色墙面，突出人物主体',
      '使用大光圈（f/1.4-f/2.8）虚化背景，让模特从环境中分离出来',
      '注意补光：背光拍摄时，用反光板或白色衣服对暗部进行补光，避免人脸过暗'
    ],
    params: {
      mode: 'A 档（光圈优先）',
      aperture: 'f/1.8 - f/2.8',
      shutter: '1/200s - 1/800s',
      iso: '100 - 400',
      focal: '50mm / 85mm',
      metering: '评价测光 / 点测光（脸部）'
    },
    mistakes: [
      '正午太阳下拍摄，光线硬、阴影重，人物眼袋和法令纹明显',
      '背景杂乱，电线杆和路人比模特还抢眼',
      '对焦在衣服或背景上，人物眼睛模糊',
      '把人物放在画面正中间，背景太满缺乏呼吸感'
    ]
  },
  {
    id: 2,
    category: 'portrait',
    level: 2,
    title: '室内自然光人像',
    summary: '利用窗边的柔和散射光，不用闪光灯也能拍出文艺清新的室内人像。',
    cover: COVER_IMAGES.portrait,
    keyPoints: [
      '找到房间里最大的窗户，让模特站在距离窗户 0.5-1 米的位置',
      '让模特身体 45 度侧对窗户，主光从侧面来，面部有立体感',
      '使用白色床单、泡沫板当反光板，把暗部细节补出来',
      'ISO 可以适当提高到 800-1600，保证快门速度不低于安全快门',
      '如果窗外景色杂乱，可以拉上半透明的纱帘柔化光线并简化背景'
    ],
    params: {
      mode: 'A 档 / M 档',
      aperture: 'f/1.8 - f/4',
      shutter: '1/80s - 1/200s',
      iso: '400 - 1600',
      focal: '35mm / 50mm',
      metering: '点测光（脸部）'
    },
    mistakes: [
      '模特正面对着强光窗户，结果人脸全黑成剪影',
      '头顶吸顶灯做主光源，造成难看的顶光阴影',
      '闪光灯直射人脸，油光明显、红眼、背景漆黑'
    ]
  },
  {
    id: 3,
    category: 'landscape',
    level: 1,
    title: '风光摄影构图基础',
    summary: '掌握风光摄影最核心的构图原则，让你的风景照片层次分明、意境深远。',
    cover: COVER_IMAGES.landscape,
    keyPoints: [
      '三分法放置地平线：天空好看就把地平线放在下 1/3，地面好看就放在上 1/3',
      '前景构图：找一个有特色的前景（石头、花丛、栏杆），增加画面纵深感',
      '引导线：利用道路、河流、围栏把视线引向远方的主体',
      '画面要有层次：前景、中景、远景各有内容，照片就会"立体"起来',
      '多等一会儿：风光摄影靠"等"——等云、等光、等雾气，耐心会给你回报'
    ],
    params: {
      mode: 'A 档（光圈优先）',
      aperture: 'f/8 - f/16',
      shutter: '根据光线',
      iso: '100（最低）',
      focal: '16mm - 35mm（广角）',
      metering: '评价测光 ± 曝光补偿'
    },
    mistakes: [
      '地平线放在画面正中间，上下对半分，缺乏重点',
      '地平线倾斜，水面和建筑歪歪扭扭',
      '光圈开到最大想拍全景，结果远景是虚的',
      '只有远景没有前景，画面空荡荡像手机随手拍'
    ]
  },
  {
    id: 4,
    category: 'landscape',
    level: 2,
    title: '长曝光拍丝绸流水',
    summary: '用慢速快门把瀑布、河流、海浪拍成如丝如雾的梦幻效果。',
    cover: COVER_IMAGES.landscape,
    keyPoints: [
      '三脚架是必须的，任何手抖都会让整张照片糊掉',
      '快门速度设置在 1s - 10s 之间，水流越慢需要的快门时间越长',
      '为了在白天也能放慢快门，需要配一块 ND 减光镜（ND8、ND64 或 ND1000）',
      '光圈不要太小（不小于 f/16），避免产生衍射降低画质',
      '使用相机的 2 秒自拍或快门线，避免按快门瞬间产生的机振',
      '如果水太亮过曝，可以加一块渐变灰镜（GND）压暗天空亮度'
    ],
    params: {
      mode: 'M 档 / S 档（快门优先）',
      aperture: 'f/8 - f/11',
      shutter: '1s - 10s',
      iso: '100',
      focal: '24mm - 70mm',
      metering: '评价测光'
    },
    mistakes: [
      '手持拍摄或三脚架不稳，结果整张图都糊了',
      '没加 ND 镜，白天强行长曝光结果整张过曝发白',
      '快门速度太快（1/30s），水流还没雾化成丝的效果'
    ]
  },
  {
    id: 5,
    category: 'night',
    level: 1,
    title: '城市夜景基础拍摄',
    summary: '掌握三脚架 + 低 ISO + 小光圈的夜景三要素，拍出通透干净的城市夜景。',
    cover: COVER_IMAGES.night,
    keyPoints: [
      '夜景拍摄最佳时间是日落后 20-30 分钟的蓝调时刻，天空还保留深蓝色，不会全黑',
      '三脚架必须！稳定是夜景第一要素，配合自拍定时器或快门线使用',
      '使用小光圈（f/8-f/13），能让灯光产生漂亮的星芒效果',
      'ISO 固定 100，保证画质干净无噪点，用快门速度来控制曝光亮度',
      '白平衡可以偏冷一点（4000K 左右），突出冷调都市感'
    ],
    params: {
      mode: 'M 档 / B 门',
      aperture: 'f/8 - f/13',
      shutter: '5s - 30s',
      iso: '100',
      focal: '16mm - 50mm',
      metering: '评价测光（参考直方图）'
    },
    mistakes: [
      '手持夜景，ISO 拉到 12800，结果全是噪点颗粒',
      '天空完全黑透了才拍，结果照片只有灯光没有层次',
      '光圈 f/1.4 全开拍夜景，灯光虚成一大坨，没有细节'
    ]
  },
  {
    id: 6,
    category: 'night',
    level: 2,
    title: '车轨与光绘拍摄',
    summary: '长曝光记录车流轨迹和光绘涂鸦，拍出赛博朋克感十足的炫酷夜景。',
    cover: COVER_IMAGES.night,
    keyPoints: [
      '选一个高机位的天桥或高楼，俯拍十字路口的车流效果最好',
      '车轨拍摄要等到天完全黑透（日落后 1 小时），车灯才会突出',
      '快门速度 10s-60s，时间越长轨迹越完整连续',
      '光圈 f/8-f/13，ISO 100，小光圈可以让车灯拉出锐利的光线',
      '光绘摄影要在全黑的环境，用手电筒或荧光棒在空中画画，人要穿深色衣服并在曝光期间移动，这样人才不会被拍进去'
    ],
    params: {
      mode: 'M 档 / B 门',
      aperture: 'f/8 - f/16',
      shutter: '10s - 60s',
      iso: '100',
      focal: '24mm - 70mm',
      metering: '参考直方图，高光不过曝'
    },
    mistakes: [
      '车流量少的时间段拍，结果只拉出几条稀稀拉拉的线',
      '镜头起雾或有灰尘，灯光产生大量眩光影响画面',
      '光绘时人站着不动，结果人影被拍进画面里'
    ]
  },
  {
    id: 7,
    category: 'street',
    level: 1,
    title: '街头摄影入门',
    summary: '记录真实的城市生活气息，街头摄影的核心是"观察"而不是"拍摄"。',
    cover: COVER_IMAGES.street,
    keyPoints: [
      '装备要轻：一机一镜（35mm 或 28mm），低调不引人注目',
      '先设置好相机：A 档 + 光圈 f/5.6 + ISO 自动（上限 1600）+ 对焦 AF-C，保证随时抬手就能拍',
      '不要偷拍！礼貌和勇气比技术更重要，主动微笑、打个招呼，大多数人都不会拒绝',
      '预判：站在一个好场景前面（有特色的墙面、招牌），等待有趣的人走进画面',
      '多拍同一场景：看到一个画面，至少从 3 个不同角度各拍 3 张，回去选最好的那张'
    ],
    params: {
      mode: 'A 档 / P 档',
      aperture: 'f/4 - f/8',
      shutter: '≥ 1/250s（凝固动作）',
      iso: '自动（最高 800-1600）',
      focal: '28mm / 35mm',
      metering: '评价测光'
    },
    mistakes: [
      '用 200mm 长焦偷拍别人，远远地举着相机非常不礼貌',
      '快门太慢，路人走路导致画面模糊',
      '照片只有"街"没有"人"，街头摄影的灵魂永远是人'
    ]
  },
  {
    id: 8,
    category: 'sports',
    level: 2,
    title: '运动瞬间抓拍',
    summary: '捕捉运动中的精彩瞬间，冻结动作同时保证画面清晰。',
    cover: COVER_IMAGES.sports,
    keyPoints: [
      '对焦模式 AF-C（连续自动对焦）+ 扩展对焦区域，让相机牢牢咬住主体',
      '驱动模式设为"高速连拍"（8-10 张/秒或更高），关键时刻按住快门不松手',
      '快门速度不低于 1/500s，拍高速运动（赛车、飞鸟）需要 1/1000s 以上',
      '用长焦镜头（70-200mm、100-400mm），远距离不干扰运动员也能拍到特写',
      '追焦技巧：身体跟随主体转动，相机与主体保持同步移动，即使快门慢一些也能拍到主体清晰背景带动感的画面'
    ],
    params: {
      mode: 'S/Tv 档（快门优先）',
      aperture: '最大可用光圈',
      shutter: '1/500s - 1/2000s',
      iso: '自动（上限 3200-6400）',
      focal: '70-200mm / 100-400mm',
      metering: '点测光 / 中央重点'
    },
    mistakes: [
      '单次对焦（AF-S），半按快门对好焦主体已经跑了',
      '快门速度太慢，动作糊成一团',
      '只盯着取景器看，结果更精彩的瞬间从眼前溜过去了'
    ]
  },
  {
    id: 9,
    category: 'food',
    level: 1,
    title: '美食拍摄技巧',
    summary: '用手机或相机都能拍出朋友圈点赞爆棚的美食大片。',
    cover: COVER_IMAGES.food,
    keyPoints: [
      '光线第一：坐到窗边的位置，利用自然的散射光从侧面或 45 度照射食物',
      '绝对不要用闪光灯！强光会让食物反光、出油、发白，完全没有食欲',
      '构图方式：45 度俯拍最通用，平拍突出食物的厚度和层次（如汉堡、蛋糕），完全俯拍（Flat Lay）适合多道菜和丰富的餐具搭配',
      '适当的留白：画面四周留出一定的空间，搭配餐巾、餐具、饮料作为道具增加氛围',
      '热菜要快拍！刚出锅的食物有热气、色泽饱满，放凉了拍出来就是一坨死物'
    ],
    params: {
      mode: 'A 档',
      aperture: 'f/2.8 - f/5.6',
      shutter: '≥ 1/80s',
      iso: '100 - 800',
      focal: '50mm / 微距',
      metering: '中央重点测光'
    },
    mistakes: [
      '头顶餐厅的灯光直接拍，食物顶部亮、碗底黑，色彩非常差',
      '食物堆得满满当当毫无空隙，画面很闷',
      '桌面上乱七八糟：纸巾、账单、手机、水杯全部入镜，喧宾夺主'
    ]
  },
  {
    id: 10,
    category: 'flower',
    level: 1,
    title: '花卉微距拍摄',
    summary: '用微距镜头或手机特写，记录花朵令人惊叹的细节之美。',
    cover: COVER_IMAGES.flower,
    keyPoints: [
      '拍摄时间：清晨带露水时最佳，花朵状态饱满，水珠还能反射光线',
      '有风的日子不要拍！微距景深极浅，花瓣轻轻一晃就虚了',
      '光圈 f/4-f/8，不要全开，保证花蕊和花瓣的主要部分都在焦内',
      '使用单点精确对焦，焦点一定要选在花蕊上，这是花朵的"眼睛"',
      '如果光线不好，用一块大的白色泡沫板当柔光板，让光线均匀柔和地照亮花瓣',
      '可以使用小喷壶在花瓣上喷点水珠，增加通透感和画面亮点'
    ],
    params: {
      mode: 'A 档 / M 档',
      aperture: 'f/4 - f/11',
      shutter: '≥ 1/125s',
      iso: '100 - 400',
      focal: '60mm / 90mm / 100mm 微距',
      metering: '点测光 / 中央重点'
    },
    mistakes: [
      '光圈 f/2.8 全开，结果只有一条花瓣清晰，其余全是虚的',
      '正午阳光下拍摄，花瓣颜色发白、阴影浓重',
      '对焦在花瓣边缘，花蕊完全模糊，画面没有重点'
    ]
  },
  {
    id: 11,
    category: 'night',
    level: 3,
    title: '星空与银河拍摄',
    summary: '在没有光污染的地方，记录璀璨的星空和绚丽的银河。',
    cover: COVER_IMAGES.night,
    keyPoints: [
      '选址要远离城市，至少 100 公里以上，光污染越轻银河越明亮',
      '时间：每月农历二十五到下月初五（没有月亮），春夏季银河最亮（3-9 月）',
      '广角大光圈镜头必备：14mm f/2.8、16-35mm f/2.8 是标配',
      '使用 500 法则计算最大快门时间（不拖线）：快门秒数 = 500 ÷ 等效焦距（如 20mm 全画幅，500÷20=25s）',
      'ISO 开高：3200-6400，现代全画幅相机完全扛得住',
      '对焦：镜头切换到 MF 手动对焦，拧到无限远再回拧一点点，用实时放大确认星星成点状'
    ],
    params: {
      mode: 'M 档',
      aperture: '最大光圈（f/2.8 / f/1.8）',
      shutter: '15s - 30s',
      iso: '3200 - 6400',
      focal: '14mm - 24mm 广角',
      metering: '手动，参考直方图'
    },
    mistakes: [
      '满月时去拍银河，月亮太亮把银河盖过去了',
      '在城市郊区拍，光污染严重，天空灰蒙蒙一片根本拍不到银河',
      '用 50mm 焦段拍，即使 10 秒也会把星星拖成一条线'
    ]
  },
  {
    id: 12,
    category: 'portrait',
    level: 3,
    title: '夜景人像拍摄',
    summary: '城市霓虹灯 + 人物主体，拍出极具氛围感的夜景人像大片。',
    cover: COVER_IMAGES.portrait,
    keyPoints: [
      '主光源选择：让模特站在路灯、橱窗、霓虹灯的前面或侧面，让环境光成为主光',
      '大光圈镜头 + 中长焦（85mm f/1.4、50mm f/1.2），把身后的灯光虚化出漂亮的圆形光斑（Bokeh）',
      '补光方案一：机顶闪光灯 + 柔光罩，对着天花板或墙面打跳闪补光',
      '补光方案二：LED 补光灯（小型棒灯），放在模特侧前方补亮脸部',
      '对焦切换到单点精确对焦，对在模特眼睛上，弱光下对焦困难可以用手机手电筒辅助对焦后再关掉'
    ],
    params: {
      mode: 'M 档',
      aperture: 'f/1.4 - f/2.8',
      shutter: '1/80s - 1/200s',
      iso: '800 - 3200',
      focal: '50mm / 85mm',
      metering: '点测光（脸部）'
    },
    mistakes: [
      '闪光灯直接怼脸，人物像被拍证件照一样，背景全黑',
      '没有补光，人物脸是黑的，只有背景的灯光亮',
      'ISO 太低导致快门太慢，人脸虚掉'
    ]
  }
])

const getCategoryName = (key) => {
  const cat = categories.find(c => c.key === key)
  return cat ? cat.name : key
}

const getCategoryCount = (key) => {
  if (key === 'all') return tips.value.length
  return tips.value.filter(t => t.category === key).length
}

const filteredTips = computed(() => {
  if (activeCategory.value === 'all') return tips.value
  return tips.value.filter(t => t.category === activeCategory.value)
})
</script>

<style scoped>
.tips-page {
  padding: 30px 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-hero {
  text-align: center;
  padding: 40px 20px 50px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
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
  opacity: 0.95;
  margin: 0;
}

.category-section {
  margin-bottom: 30px;
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.category-tabs {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.category-tabs::-webkit-scrollbar {
  display: none;
}

.tab-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: #f5f7fa;
  border: 2px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: #555;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  background: #eef2ff;
  color: #667eea;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: #667eea;
}

.tab-icon {
  font-size: 16px;
}

.tab-name {
  font-weight: 500;
}

.tab-count :deep(.el-badge__content) {
  margin-left: 4px;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.tip-card {
  background: white;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.07);
  transition: all 0.3s ease;
}

.tip-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.18);
}

.tip-header {
  position: relative;
}

.tip-image {
  width: 100%;
  height: 200px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.tip-category-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.65);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  backdrop-filter: blur(4px);
}

.tip-level {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.level-1 {
  background: rgba(82, 196, 26, 0.9);
  color: white;
}
.level-2 {
  background: rgba(250, 173, 20, 0.9);
  color: white;
}
.level-3 {
  background: rgba(245, 34, 45, 0.9);
  color: white;
}

.tip-body {
  padding: 20px 22px 24px;
}

.tip-title {
  font-size: 18px;
  margin: 0 0 10px;
  color: #1a1a1a;
  font-weight: 600;
}

.tip-summary {
  font-size: 14px;
  color: #666;
  line-height: 1.7;
  margin: 0 0 14px;
}

.collapse-title {
  color: #667eea;
  font-weight: 500;
}

.tip-collapse {
  border-top: 1px solid #f0f0f0;
  padding-top: 4px;
}

.detail-section + .detail-section {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #eee;
}

.detail-section h4 {
  font-size: 14px;
  margin: 0 0 10px;
  color: #333;
}

.point-list,
.mistake-list {
  padding-left: 20px;
  margin: 0;
}

.point-list li,
.mistake-list li {
  font-size: 13.5px;
  line-height: 1.75;
  color: #555;
  margin-bottom: 5px;
}

.mistake-list li {
  color: #d4380d;
}

.params-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.param-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.param-label {
  font-size: 11px;
  color: #999;
}

.param-value {
  font-size: 13px;
  color: #333;
  font-weight: 600;
}

@media (max-width: 768px) {
  .tips-page {
    padding: 20px 16px;
  }

  .page-title {
    font-size: 26px;
  }

  .tips-grid {
    grid-template-columns: 1fr;
  }

  .params-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
