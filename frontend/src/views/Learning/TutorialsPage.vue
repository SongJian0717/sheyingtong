<template>
  <div class="tutorials-page">
    <section class="page-hero">
      <h1 class="page-title">📖 摄影入门教程</h1>
      <p class="page-subtitle">从零开始学习摄影，掌握基础理论，建立正确的拍摄思维</p>
    </section>

    <section class="progress-section">
      <div class="progress-card">
        <div class="progress-header">
          <span>学习进度</span>
          <span class="progress-text">{{ completedCount }} / {{ totalSections }} 章节</span>
        </div>
        <el-progress :percentage="Math.round(completedCount / totalSections * 100)" :stroke-width="12" />
      </div>
    </section>

    <section class="tutorials-content">
      <div class="tutorial-item" v-for="(tutorial, index) in tutorials" :key="tutorial.id">
        <div class="tutorial-header" @click="toggleSection(index)">
          <div class="tutorial-index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="tutorial-meta">
            <h3 class="tutorial-title">{{ tutorial.title }}</h3>
            <p class="tutorial-desc">{{ tutorial.description }}</p>
            <div class="tutorial-tags">
              <el-tag size="small" type="info">⏱ {{ tutorial.duration }}</el-tag>
              <el-tag size="small" :type="tutorial.difficulty === '初级' ? 'success' : tutorial.difficulty === '中级' ? 'warning' : 'danger'">
                {{ tutorial.difficulty }}
              </el-tag>
            </div>
          </div>
          <div class="tutorial-actions">
            <el-checkbox v-model="tutorial.completed" @change="updateProgress" />
            <el-icon class="expand-icon" :class="{ expanded: expandedSections.includes(index) }">
              <ArrowDown />
            </el-icon>
          </div>
        </div>

        <div class="tutorial-body" v-show="expandedSections.includes(index)">
          <div class="tutorial-content-block" v-for="(section, sIdx) in tutorial.sections" :key="sIdx">
            <h4 class="section-heading">{{ section.heading }}</h4>
            <p class="section-text" v-for="(p, pIdx) in section.paragraphs" :key="pIdx">{{ p }}</p>
            <div class="section-tips" v-if="section.tips">
              <div class="tip-card">
                <span class="tip-icon">💡</span>
                <div class="tip-content">
                  <strong>实战提示：</strong>
                  <span v-for="(tip, tIdx) in section.tips" :key="tIdx">{{ tip }}</span>
                </div>
              </div>
            </div>
            <div class="section-example" v-if="section.example">
              <div class="example-card">
                <span class="example-title">📷 示例场景</span>
                <p class="example-text">{{ section.example }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'

const expandedSections = ref([0])

const toggleSection = (index) => {
  const idx = expandedSections.value.indexOf(index)
  if (idx > -1) {
    expandedSections.value.splice(idx, 1)
  } else {
    expandedSections.value.push(index)
  }
}

const tutorials = ref([
  {
    id: 1,
    title: '认识你的相机',
    description: '了解相机的基本结构、模式转盘、按键功能，建立操作习惯',
    duration: '约 20 分钟',
    difficulty: '初级',
    completed: false,
    sections: [
      {
        heading: '1.1 相机的基本结构',
        paragraphs: [
          '无论你使用的是单反、微单还是高端卡片机，相机的基本结构都是类似的。主要部件包括：镜头、机身、传感器、取景器、液晶屏和各种功能按键。',
          '镜头负责收集光线并聚焦成像，是决定画质的最重要部件之一。机身内的图像传感器（CMOS/CCD）则负责将光信号转换为数字信号，是相机的"底片"。',
          '取景器分为光学取景器（OVF）和电子取景器（EVF）两种。光学取景器通过反光镜直接看到真实场景，几乎零延迟；电子取景器则显示传感器实时捕捉的画面，可以预览曝光效果。'
        ],
        tips: [
          '刚拿到相机时，第一件事是贴好液晶屏保护膜，并准备一块擦镜布（镜头笔）用于清洁镜头和屏幕。'
        ]
      },
      {
        heading: '1.2 模式转盘的使用',
        paragraphs: [
          '入门相机通常都有模式转盘，上面的字母分别代表：Auto（全自动）、P（程序自动）、S/Tv（快门优先）、A/Av（光圈优先）、M（手动）。',
          '新手建议先从 P 档开始：相机会自动帮你设置光圈和快门的组合，你只需要调整ISO、白平衡、曝光补偿等参数即可。熟悉之后再逐步切换到 A 档、S 档，最后再挑战 M 档。',
          '场景模式（人像、风景、微距、夜景、运动等）是厂商预设好的参数组合，适合完全零基础的阶段，但不建议长期依赖，因为它们会限制你的创作自由。'
        ],
        example: '拍摄风光时，先用 A 档（光圈优先），把光圈调到 f/8-f/11 之间保证画面清晰度，其余交给相机自动处理。'
      },
      {
        heading: '1.3 核心按键与菜单',
        paragraphs: [
          '相机背部常见的按键有：INFO（信息显示）、MENU（菜单）、回放、删除、Q/快捷键、以及五维导航键。顶部一般有电源开关、快门按钮、前后拨轮。',
          '建议花 10 分钟通读一遍说明书的"按键说明"部分，了解每个按钮的基本功能。之后再花几天时间熟悉"盲操作"——不用看屏幕就能切换关键参数。',
          '菜单里的项目很多，新手只需要关注：画质设置（建议 RAW + JPEG）、自动对焦模式、测光模式、白平衡偏移、以及屏幕和取景器的亮度调节。'
        ],
        tips: [
          'RAW 格式保留了传感器的原始数据，后期调整空间比 JPEG 大很多。存储卡够大的话，建议一直用 RAW 格式拍摄。'
        ]
      }
    ]
  },
  {
    id: 2,
    title: '曝光三要素：光圈·快门·ISO',
    description: '理解曝光的本质，掌握三者之间的平衡关系',
    duration: '约 40 分钟',
    difficulty: '初级',
    completed: false,
    sections: [
      {
        heading: '2.1 什么是曝光？',
        paragraphs: [
          '曝光是指光线通过镜头照射到传感器上的过程。照片的亮度由曝光量决定：曝光过量会过曝（发白），曝光不足会欠曝（发黑）。',
          '曝光量由三个参数共同控制，这就是摄影中最重要的"曝光三要素"：光圈、快门速度、ISO。三者共同决定了一张照片的明暗，同时各自影响画面的不同视觉效果。',
          '在曝光量相同的情况下，你可以通过调整三要素来获得不同的画面效果——这就是摄影创作的核心技巧之一。'
        ],
        example: '晴天户外，光圈 f/8 + 快门 1/250s + ISO 100，与 光圈 f/4 + 快门 1/1000s + ISO 100 的曝光量基本相同，但景深和动态凝固效果完全不同。'
      },
      {
        heading: '2.2 光圈（Aperture）',
        paragraphs: [
          '光圈是镜头内部的一组金属叶片，通过开合来控制通光孔的大小。光圈大小用 f 值表示：f/1.4、f/2、f/2.8、f/4、f/5.6、f/8、f/11、f/16……数字越小，光圈越大，通光量越多。',
          '光圈除了影响进光量，最主要的作用是控制景深：大光圈（f/1.4-f/4）景深很浅，背景虚化效果强，适合人像；小光圈（f/8-f/22）景深大，从近到远都清晰，适合风光和建筑。',
          '每一级光圈之间通光量相差一倍。例如从 f/4 到 f/2.8，通光量翻倍；从 f/5.6 到 f/8，通光量减半。'
        ],
        tips: [
          '拍摄人像时，不建议无脑开到最大光圈。f/1.4 的镜头通常在 f/2 到 f/2.8 时锐度和成像质量最佳，虚化也足够漂亮。'
        ]
      },
      {
        heading: '2.3 快门速度（Shutter Speed）',
        paragraphs: [
          '快门速度是指快门帘开启的时间长度，单位是秒。常见档位：1/1000s、1/500s、1/250s、1/125s、1/60s、1/30s、1/15s、1/8s、1/4s、1/2s、1s、2s 等。',
          '快门速度决定了画面的"时间感"：高速快门（1/500s 以上）可以凝固运动瞬间，如奔跑的动物、飞溅的水花；低速快门（1/30s 以下）可以记录物体运动轨迹，如流水、车轨、星轨。',
          '手持拍摄时，为避免手抖造成画面模糊，快门速度一般不要低于安全快门值。安全快门 ≈ 1/等效焦距，例如 50mm 焦段下，快门速度应不低于 1/80s。'
        ],
        example: '拍摄瀑布想要"丝绸流水"效果：用三脚架，快门速度设为 1s-4s，光圈根据亮度缩小到 f/8-f/16，ISO 100。'
      },
      {
        heading: '2.4 ISO 感光度',
        paragraphs: [
          'ISO 是传感器对光线的敏感程度。常见 ISO 值：100、200、400、800、1600、3200、6400、12800……数值越高，传感器越敏感，照片越亮。',
          '但 ISO 不是越高越好：高 ISO 会引入噪点（颗粒感），降低画面细节和动态范围。一般原则是：在保证安全快门的前提下，ISO 越低越好。',
          '现代相机（尤其是全画幅）的高感能力已经很强，ISO 3200 甚至 6400 都能接受。但如果有三脚架和充足时间，还是应该坚持用低 ISO 拍摄以获得最佳画质。'
        ],
        tips: [
          '新手常见误区：为了让画面亮而盲目拉高 ISO，结果照片噪点严重。正确做法是先开大光圈、放慢快门，实在不行再提高 ISO。'
        ]
      },
      {
        heading: '2.5 曝光补偿与测光',
        paragraphs: [
          '在 P/A/S 档下，相机自带的测光系统会给出一个它认为"正确"的曝光。但有时你会发现：拍雪景结果是灰的，拍夜景人物脸是黑的——这就是测光"被骗了"。',
          '曝光补偿（±EV）就是用来解决这个问题的。原则是：白加黑减——拍白色、明亮的场景（雪景、沙滩）要加曝光（+0.7 到 +1.5 EV）；拍黑色、昏暗的场景（夜景、深色服装）要减曝光（-0.5 到 -1 EV）。',
          '常用的测光模式有：评价测光（矩阵测光，适合大多数场景）、中央重点测光（适合人像）、点测光（适合明暗反差大的场景，如舞台、逆光人像）。'
        ],
        example: '逆光拍人像时，切换到点测光模式，对准人物的脸部测光，锁定曝光后再构图拍摄，这样人脸亮度准确，背景可能会过曝成美丽的光晕。'
      }
    ]
  },
  {
    id: 3,
    title: '构图的艺术',
    description: '学习经典构图法则，让你的照片更有秩序和美感',
    duration: '约 35 分钟',
    difficulty: '初级',
    completed: false,
    sections: [
      {
        heading: '3.1 什么是构图？',
        paragraphs: [
          '构图是指在画面中安排和组织元素的方式。好的构图能引导观众的视线，突出主体，传达情绪。摄影是"减法的艺术"——构图的本质就是决定拍什么、不拍什么。',
          '构图没有绝对的对错，初学者可以先学习一些经典的构图法则，在熟练掌握之后再尝试打破规则，形成自己的风格。',
          '构图时要时刻问自己三个问题：我的画面主体是什么？我要把观众的注意力引向哪里？画面中有没有多余的元素可以去掉？'
        ],
        tips: [
          '构图的第一步不是"怎么摆"，而是"机位怎么选"。多走几步、蹲下来、爬上去……机位的变化带来的构图变化远比后期剪裁有效。'
        ]
      },
      {
        heading: '3.2 三分法 / 黄金分割',
        paragraphs: [
          '最经典也最实用的构图法：把画面用两条横线和两条竖线分成九等份，线条交汇处称为"黄金分割点"。把主体放在黄金分割点或线上，比放在正中央更有美感。',
          '大多数相机都可以打开"网格线"功能，取景时直接看到九宫格辅助线。拍风光时，地平线一般放在上 1/3 或下 1/3 线的位置，不要放在正中间。',
          '拍人像时，人物的眼睛是画面的视觉焦点，把眼睛放在上 1/3 线的位置通常效果最好。垂直构图时，把人物放在左 1/3 或右 1/3 处，留出"呼吸空间"。'
        ],
        example: '拍摄海边日落：把地平线放在下 1/3 线（如果天空好看），让夕阳落在右下黄金分割点；如果海面倒影更美，就把地平线放在上 1/3 线。'
      },
      {
        heading: '3.3 常见的构图法则',
        paragraphs: [
          '引导线构图：利用画面中的线条（道路、栏杆、河流、建筑轮廓）将视线引向主体。引导线可以是直线，也可以是曲线（S 形构图特别优美）。',
          '对称构图：画面左右或上下对称，给人稳定、庄严、平静的感觉。适合拍摄建筑、倒影、中式园林。拍摄时要注意：对称构图最忌讳机位稍微不正，一定要对齐中心轴。',
          '框架构图：利用前景的门框、窗户、树枝、山洞等形成"框架"，把主体框在里面，增强纵深感和层次感。框架式构图同时还有"减法"功能——遮挡掉杂乱的背景。',
          '留白构图：画面中留出大面积空白（天空、水面、纯色背景），给主体足够的呼吸空间，营造简洁、宁静、有意境的氛围。',
          '对角线构图：将主体沿对角线方向摆放，打破平衡，带来动感和张力。常用于拍摄人物侧脸、建筑物的一角、飘落的树叶等。'
        ],
        tips: [
          '尝试用多种构图拍摄同一个场景：先拍一张标准三分法，再拍一张对称，再试试框架……对比之后你就会体会到不同构图带来的情感差异。'
        ]
      },
      {
        heading: '3.4 构图中的常见问题',
        paragraphs: [
          '画面太满：主体太大，四周没有留白，显得压抑。解决方法：后退一步，或用更广角的焦段，给画面留出空间。',
          '地平线倾斜：风光摄影的致命错误。打开相机的虚拟水平仪（或网格线辅助），确保地平线与画面边缘平行。',
          '杂物干扰：电线杆从头顶长出来、垃圾出现在角落、路人的半只脚在画面边缘……拍摄前养成"扫描四边"的习惯。',
          '主体太小不突出：背景比主体还抢眼。解决方法：靠近、用长焦距压缩背景、开大光圈虚化背景、或利用光影突出主体。'
        ]
      }
    ]
  },
  {
    id: 4,
    title: '对焦与清晰度',
    description: '掌握对焦模式，拍出清晰锐利的照片',
    duration: '约 25 分钟',
    difficulty: '初级',
    completed: false,
    sections: [
      {
        heading: '4.1 自动对焦的基本模式',
        paragraphs: [
          '相机的自动对焦（AF）模式通常分为三类：AF-S（单次自动对焦）、AF-C（连续自动对焦）、AF-A（自动切换）。',
          'AF-S 适合拍摄静止的主体：风光、人像、静物。半按快门对焦一次，对焦成功后锁定，可重新构图。',
          'AF-C 适合拍摄运动中的主体：奔跑的孩子、飞翔的鸟、体育比赛。半按快门期间，相机会持续追踪主体进行对焦。'
        ],
        example: '带孩子在公园玩：切换到 AF-C 连续对焦 + 区域对焦，快门速度不低于 1/500s，保持快门半按跟踪孩子，关键时刻按下快门。'
      },
      {
        heading: '4.2 对焦区域选择',
        paragraphs: [
          '单点对焦：只对一个点对焦，精度最高，适合主体明确且静止的场景。拍人像时，对焦点一定要选在眼睛上。',
          '区域对焦（扩展对焦）：选择一个区域，相机会在这个区域内寻找主体对焦，比单点更宽容，适合小幅移动的主体。',
          '广域/全域对焦：整个画面自动识别主体对焦，适合抓拍。缺点是有时会对到前景而不是你想要的主体上。'
        ],
        tips: [
          '使用大光圈镜头（如 f/1.4、f/1.8）时，景深非常浅，务必使用精确的单点对焦并对在眼睛上——睫毛清晰、瞳孔模糊都算失败。'
        ]
      },
      {
        heading: '4.3 为什么照片不够清晰？',
        paragraphs: [
          '对焦不准：最常见原因。检查是否对焦到了错误的物体上，或者主体移动导致对焦偏移。建议先对焦再构图（或配合对焦锁定）。',
          '手抖造成的模糊：快门速度低于安全快门。解决：提高快门速度、使用三脚架、开启镜头或机身的防抖功能。',
          '光圈过大锐度不够：镜头最大光圈通常不是最锐利的，收一到两档（如 f/1.4 收到 f/2 或 f/2.8）画质会明显提升。',
          'ISO 过高产生噪点：噪点会让画面看起来"脏"、"糊"。能低 ISO 就低 ISO，实在不行再接受高感。',
          '镜头脏了：镜头前组有指纹、灰尘、油污，会严重影响清晰度，尤其是逆光时产生严重的眩光和雾化。'
        ]
      }
    ]
  },
  {
    id: 5,
    title: '光的魔法',
    description: '认识光线的方向、色温与质感，学会用光来表达',
    duration: '约 30 分钟',
    difficulty: '中级',
    completed: false,
    sections: [
      {
        heading: '5.1 光线的四个维度',
        paragraphs: [
          '光线是摄影的灵魂。评价一束光，我们通常看四个方面：强度（强/弱）、方向（顺/侧/逆/顶/底）、色温（暖/冷）、质感（硬/软）。',
          '不同的光线组合传达完全不同的情绪。明亮的顺光带来清晰、开朗的感觉；侧光带来立体感和质感；逆光带来梦幻、氛围感；硬光硬朗、强烈；柔光温柔、平静。',
          '初学阶段不要追求"光线好的时候才拍"，而应该学会在任何光线下都能找到合适的拍摄方式——这才是真正的用光能力。'
        ]
      },
      {
        heading: '5.2 光的方向',
        paragraphs: [
          '顺光：光源在摄影师身后，直接照在被摄体正面。优点是被摄体明亮、细节清晰、曝光容易；缺点是画面平、缺乏立体感。',
          '侧光：光源从侧面照来，被摄体一面亮一面暗，产生强烈的明暗对比和立体感。非常适合拍人像、风光和强调质感的物体。',
          '逆光：光源在被摄体后方，正面是暗的。可以拍出"剪影"效果（对着亮处曝光），或对暗部补光拍出梦幻的"轮廓光 + 发丝光"。',
          '顶光：光线从头顶垂直射下（正午太阳），会在人脸上造成眼袋阴影，是拍摄人像最不理想的光线。建议避开正午拍摄，或寻找阴影处。'
        ],
        example: '夕阳下拍剪影：让太阳在人物身后，用点测光对准天空（亮部）测光并锁定曝光，人物就会变成黑色的漂亮剪影，配合美丽的晚霞效果极佳。'
      },
      {
        heading: '5.3 色温与白平衡',
        paragraphs: [
          '色温用开尔文（K）表示：2000K-4000K 是暖色调（橙红色，如火光、日出日落）；5000K-6500K 是中性白（如正午日光）；7000K 以上是冷色调（蓝青色，如阴天、阴影处）。',
          '白平衡（WB）的作用是告诉相机"什么是白色"，从而让画面的色彩还原准确。自动白平衡（AWB）在大多数场景下都能胜任，但在极端色温（如烛光、荧光灯）下会偏色。',
          '建议：平时用 AWB 即可，拍摄 RAW 格式可以在后期随意调整白平衡而不损失画质。如果追求 JPEG 直出，可以手动选择对应的白平衡预设（日光、阴天、白炽灯、荧光灯等）。'
        ],
        tips: [
          '创意白平衡：想要冷调忧郁的氛围，可以把白平衡往冷色调偏移；想要温暖怀旧的氛围，可以往暖色调偏移。不必总追求"正确"，有时"好看"更重要。'
        ]
      },
      {
        heading: '5.4 把握"黄金时段"',
        paragraphs: [
          '一天中最适合拍照的时间是日出后 1 小时和日落前 1 小时——这段时间被称为"黄金时刻"（Golden Hour）。此时光线角度低、色温暖、光线柔和，无论是人像还是风光都能拍出极好的效果。',
          '日落后 20-30 分钟是"蓝调时刻"（Blue Hour），天空呈现出深邃的宝蓝色，和城市的暖黄色灯光形成对比，是拍摄城市夜景的最佳时间。',
          '黄金时段虽然好，但不代表其他时间不能拍照。阴天的散射光非常适合拍人像和花；正午的硬光可以用来拍高对比的黑白片——关键是理解光线并为我所用。'
        ]
      }
    ]
  },
  {
    id: 6,
    title: '焦距与镜头',
    description: '理解不同焦段的视角与透视效果，选对镜头事半功倍',
    duration: '约 25 分钟',
    difficulty: '中级',
    completed: false,
    sections: [
      {
        heading: '6.1 焦段与视角',
        paragraphs: [
          '焦距的单位是毫米（mm）。焦距越短，视角越广，容纳的场景越多；焦距越长，视角越窄，能把远处的物体"拉近"。',
          '以全画幅相机为例：24mm 以下是超广角（适合风光、建筑、室内）；24mm-70mm 是标准焦段（适合日常、人文、环境人像）；70mm-200mm 是中长焦（适合人像、风光细节）；200mm 以上是长焦（适合动物、体育、舞台）。',
          '"等效焦距"的概念：APS-C 画幅相机需要乘以焦距转换系数（佳能约 1.6x，索尼/尼康约 1.5x），所以 50mm 镜头在 APS-C 上的等效焦距约是 75mm-80mm。'
        ]
      },
      {
        heading: '6.2 常见焦段的用途',
        paragraphs: [
          '24mm / 28mm / 35mm（广角）：画面有明显的透视效果，近大远小，能增强空间感。靠近被摄者拍能拍出具有张力的人像；拍风光能容纳更多场景。35mm 被称为人文之眼，是很多街头摄影师的最爱。',
          '50mm（标准）：视角最接近人眼，没有明显的透视畸变，画面自然。适合日常生活、环境人像、美食等题材。是最值得新手入手的第一支定焦镜头（尤其是"小痰盂" f/1.8，价格便宜画质好）。',
          '85mm（人像黄金焦段）：虚化效果自然，人脸不会变形，能让模特和背景分离开。是拍摄人像最经典的焦段，无论室内室外都非常好用。',
          '70-200mm（中长焦变焦）：拍摄风光时可以"裁剪"出远方的画面元素，压缩空间感；拍摄人像时可以在不打扰被摄者的情况下抓拍到自然的表情。'
        ],
        tips: [
          '新手买镜头不要纠结"一镜走天下"的大变焦比镜头。先买一支 50mm f/1.8 定焦，用几个月，对你的构图和用光的提升远比买昂贵的变焦镜头大。'
        ]
      },
      {
        heading: '6.3 定焦还是变焦？',
        paragraphs: [
          '定焦镜头：焦距固定，画质通常比同价位变焦更好、光圈更大、体积更小。最大的缺点是"变焦全靠腿"，拍摄不够方便。',
          '变焦镜头：焦距可变，一镜覆盖多个焦段，旅行和日常非常方便。缺点是大光圈变焦非常昂贵且笨重，同光圈下画质不如定焦。',
          '新手推荐方案：机身 + 套机头（18-55 或 24-70）熟悉焦段，确定自己的常用焦段后，再买对应的定焦镜头。大多数人最终会留下 35mm / 50mm / 85mm 三支定焦覆盖日常拍摄需求。'
        ],
        example: '第一次买镜头犹豫不决：先花几百块买一支 50mm f/1.8 定焦，用三个月。如果你觉得"经常退太远"，说明你需要更广的焦段（换 35mm）；如果你觉得"经常拉不近"，说明你需要更长的焦段（换 85mm 或变焦）。'
      }
    ]
  }
])

const updateProgress = () => {}

const completedCount = computed(() => tutorials.value.filter(t => t.completed).length)
const totalSections = computed(() => tutorials.value.length)
</script>

<style scoped>
.tutorials-page {
  padding: 30px 40px;
  max-width: 1100px;
  margin: 0 auto;
}

.page-hero {
  text-align: center;
  padding: 40px 20px 50px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

.progress-section {
  margin-bottom: 30px;
}

.progress-card {
  background: white;
  padding: 20px 28px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #555;
  margin-bottom: 12px;
  font-weight: 500;
}

.progress-text {
  color: #667eea;
}

.tutorials-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tutorial-item {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: box-shadow 0.25s ease;
}

.tutorial-item:hover {
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.12);
}

.tutorial-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px 28px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.tutorial-header:hover {
  background: #f9fafb;
}

.tutorial-index {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  font-size: 18px;
  font-weight: 700;
  border-radius: 12px;
  flex-shrink: 0;
}

.tutorial-meta {
  flex: 1;
  min-width: 0;
}

.tutorial-title {
  font-size: 20px;
  margin: 0 0 6px;
  color: #1a1a1a;
}

.tutorial-desc {
  font-size: 14px;
  color: #777;
  margin: 0 0 10px;
}

.tutorial-tags {
  display: flex;
  gap: 8px;
}

.tutorial-actions {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-shrink: 0;
}

.expand-icon {
  font-size: 18px;
  color: #999;
  transition: transform 0.3s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
  color: #667eea;
}

.tutorial-body {
  border-top: 1px solid #f0f0f0;
  padding: 28px 36px 32px;
  background: #fcfcfd;
}

.tutorial-content-block + .tutorial-content-block {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px dashed #e8e8e8;
}

.section-heading {
  font-size: 17px;
  color: #667eea;
  margin: 0 0 14px;
  padding-left: 10px;
  border-left: 3px solid #667eea;
}

.section-text {
  font-size: 15px;
  line-height: 1.85;
  color: #444;
  margin: 0 0 10px;
  text-indent: 2em;
}

.section-tips,
.section-example {
  margin-top: 18px;
}

.tip-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: #fff8e6;
  border-left: 4px solid #f5a623;
  padding: 14px 18px;
  border-radius: 0 8px 8px 0;
}

.tip-icon {
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}

.tip-content strong {
  color: #8a6d00;
  margin-right: 4px;
}

.tip-content span {
  color: #6a5a00;
  line-height: 1.7;
  font-size: 14px;
}

.example-card {
  background: #e8f5e9;
  border-radius: 8px;
  padding: 14px 18px;
  border-left: 4px solid #66bb6a;
}

.example-title {
  font-weight: 600;
  color: #2e7d32;
  margin-right: 8px;
}

.example-text {
  margin: 6px 0 0;
  color: #1b5e20;
  line-height: 1.75;
  font-size: 14px;
}

@media (max-width: 768px) {
  .tutorials-page {
    padding: 20px 16px;
  }

  .page-title {
    font-size: 26px;
  }

  .tutorial-header {
    padding: 18px 16px;
    gap: 12px;
    flex-wrap: wrap;
  }

  .tutorial-index {
    width: 40px;
    height: 40px;
    font-size: 15px;
  }

  .tutorial-title {
    font-size: 17px;
  }

  .tutorial-body {
    padding: 18px 20px 24px;
  }
}
</style>
