-- =====================================================================
-- 社影通 · 种子数据初始化脚本（data.sql）
-- =====================================================================
-- 执行时机：Spring Boot 启动时，由 application.yml spring.sql.init 触发
--           且配置了 defer-datasource-initialization=true
--           → 保证 Hibernate ddl-auto=update 先建表，再跑脚本，不会表不存在报错
-- 幂等性：所有 INSERT 均使用「INSERT IGNORE + ON DUPLICATE KEY UPDATE」，
--         重复启动不会抛主键/唯一索引冲突，会更新 updated_at 字段
-- =====================================================================
--
-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  📌 如果你已经在旧设备攒了 200+ 条真实的摄影术语 / 器材数据，     ║
-- ║     不要保留下面 30 条示例数据！请按以下步骤把真实数据导进来：     ║
-- ╠══════════════════════════════════════════════════════════════════╣
-- ║                                                                    ║
-- ║  第一步 · 旧设备导出真实数据（命令行执行 mysqldump）：              ║
-- ║  -------------------------------                                    ║
-- ║   mysqldump -uroot -p123456 sheyingtong                           ║
-- ║     --no-create-info          ← 不要建表语句                        ║
-- ║     --skip-extended-insert    ← 一行一条INSERT（方便看和改）       ║
-- ║     --complete-insert         ← INSERT明确写出列名（最关键！）     ║
-- ║     --default-character-set=utf8mb4                                ║
-- ║     photography_terms gear gear_categories                          ║
-- ║     > sheyingtong_seed_data.sql                                     ║
-- ║                                                                    ║
-- ║  第二步 · 打开导出的 .sql → 复制里面的 INSERT 语句（不要带          ║
-- ║         开头的 LOCK/UNLOCK TABLES 和末尾的注释）→ 完整替换本文件   ║
-- ║         「第 1 节 / 第 2 节 / 第 3 节」里原本的 30 条示例数据。    ║
-- ║                                                                    ║
-- ║  第三步 · 把导出的 INSERT 都改写成：                                ║
-- ║         INSERT INTO   → INSERT IGNORE INTO                         ║
-- ║         并在每条末尾追加：                                           ║
-- ║         ON DUPLICATE KEY UPDATE updated_at = NOW();                ║
-- ║                                                                    ║
-- ║  第四步 · 启动后端，data.sql 会自动跑 → 旧机器的所有词条和器材      ║
-- ║         全部出现在新机器的 MySQL 里，不用再手动插入 100 次！         ║
-- ║                                                                    ║
-- ╚══════════════════════════════════════════════════════════════════╝
--
-- =====================================================================
-- 第 1 节 · 摄影术语示例（30条，覆盖6大类，id=1~30 连续）
-- 表名：photography_terms （@Table(name="photography_terms")）
-- 唯一索引：term 列 → 必须用 INSERT IGNORE，否则二次启动报错
-- =====================================================================

-- ===== 分类 1/6 · 基础构图类（id=1~5） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(1, '三分法构图', '构图基础',
 '将画面横向与纵向各分为三等份，形成九宫格；把主体放在四个交叉点上，能打破中心对称带来的呆板，提升画面平衡感与视觉吸引力。',
 '三分法是摄影构图的入门第一课。人的视觉习惯会自然落在画面 1/3 与 2/3 交界处，而非正中心。通过将主体眼睛、地平线、主线条与交叉点/分隔线对齐，画面会更有张力。',
 '① 人像摄影时，将模特眼睛放在画面右上或左上交叉点，避免居中；② 风景照中将地平线置于上1/3或下1/3，而非画面正中心。',
 '黄金分割,对称构图,引导线构图,九宫格构图',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(2, '黄金分割构图', '构图基础',
 '基于古希腊黄金比例 1:1.618（φ≈0.618）划分画面的构图方法；比三分法更精细，能产生更符合人眼审美的和谐比例。',
 '常见形式有黄金螺旋（斐波那契螺旋）、黄金矩形、黄金三角。将画面最大兴趣中心置于螺旋的收敛点，视觉引导效果更强。',
 '① 在经典人像照中用黄金螺旋把观众视线引导到模特的眼神处；② 利用道路、河流形成的黄金三角引导线构图。',
 '三分法构图,引导线构图,斐波那契螺旋',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(3, '引导线构图', '构图基础',
 '利用画面中自然存在的线条（道路、栏杆、河流、光影、围墙等）引导观众视线，逐步汇聚至主体或兴趣中心的构图方法。',
 '引导线的核心作用是"引导视线"，能让观众在短时间内找到画面重点，并增强画面的空间纵深感与代入感。线条形式有直线、曲线、汇聚线、平行线四种。',
 '① 长曝中把马路车流灯光作为S形引导线，引向远处城市天际线；② 利用树林间的自然阳光汇聚线突出中心人物。',
 '三分法构图,S形构图,汇聚透视,框架式构图',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(4, '框架式构图', '构图基础',
 '用前景中天然或人工的「框」（门窗、拱门、树枝、建筑结构等）包围主体，形成内景，突出主题并增加画面层次。',
 '框架式构图的好处：一是能把观众视线牢牢框在主体上，避免干扰；二是形成「前景框 + 中景主体 + 远景背景」三层景深，画面立体感更强。',
 '① 通过老城的圆形门洞拍摄里面的街道；② 利用树枝围成的天然天窗拍月亮或建筑。',
 '前景虚化,层次感,留白构图',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(5, '对称构图', '构图基础',
 '以画面中轴线（水平轴或垂直轴）为基准，左右或上下元素在形状、大小、颜色、位置上基本对等的构图方式。',
 '对称构图能传递出庄重、稳定、典雅、静谧的氛围，适合建筑、倒影、宗教题材、产品摄影。但使用过多会显呆板，常结合"对称破一点点"手法增加活力。',
 '① 正对寺庙大门或宫殿中轴拍摄建筑正立面；② 利用湖面倒影形成上下完美对称风景照。',
 '倒影摄影,建筑摄影,静态平衡,非对称平衡',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 分类 2/6 · 曝光基础类（id=6~10） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(6, '曝光三角', '曝光基础',
 '决定照片最终亮度与风格的三大参数——光圈(Aperture)、快门(Shutter Speed)、感光度(ISO)——三者相互联动、此消彼长的总称。',
 '任何一次曝光调整，都要同时思考三个参数：光圈影响景深、快门冻结/模糊运动、ISO影响噪点。三者加起来就是最终的曝光值 EV；调整一者必须补偿另两者才能保持亮度一致。',
 '① 人像虚化：大光圈f/1.8 + 1/200s + ISO100；② 夜景车流光轨：小光圈f/11 + 4s + ISO100；③ 舞台运动：高速快门1/800s + f/2.8 + ISO800。',
 '光圈,快门速度,感光度ISO,景深,曝光补偿',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(7, '光圈 (Aperture)', '曝光基础',
 '镜头内由多片金属叶片组成的可调节孔径大小，用 f 值表示（如 f/1.4、f/2.8、f/8、f/16）；f 值越小光圈越大、进光越多、景深越浅。',
 '光圈同时影响两件事：(1) 进光量 → 每差一整档（例如 f/1.4 → f/2）进光量翻倍/减半；(2) 景深 → 大光圈（f/1.4~f/2.8）背景虚化、人物突出；小光圈（f/8~f/16）风景前后清晰。',
 '① 拍摄糖水片人像：f/1.4 让焦外变成奶油般化开的焦外成像（Bokeh）；② 拍摄风光全景接片：f/11 保证远山近景都锐利。',
 '景深,Bokeh焦外虚化,f值,f1.8,景深预览',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(8, '快门速度 (Shutter Speed)', '曝光基础',
 '相机快门帘幕打开让传感器曝光的时间长短，以秒为单位（如 1/1000s、1/60s、4s、30s）；决定运动物体是「冻住」还是「拉丝模糊」。',
 '两个安全阈值务必记住：① 手持不糊的最低快门 ≈ 1/焦距（mm）。例如 50mm 镜头至少 1/50s 才不会手抖糊；② 拍流水要丝缎感至少 1s、拍车流光轨 4~10s、拍星空 20~30s。',
 '① 体育摄影抓扣篮：1/1000s 把运动员凝固在半空；② 瀑布慢门：ND1000 减光镜 + 6s 曝光，水流变成绸缎般柔顺。',
 '安全快门,长曝光,ND减光镜,B门,防抖IS/OSS',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(9, '感光度 ISO', '曝光基础',
 '传感器对光线的敏感程度；ISO 数值越低（100/200）画质越干净噪点越少；数值越高（800/3200/12800）在暗光下越亮但噪点越多。',
 '现代全幅相机 ISO 1600 以下基本可用、APS-C 800 以下可用；超过这个阈值尽量用"开大光圈 / 放慢快门 / 补光"三条路替代，而不是继续加高 ISO。',
 '① 白天户外固定 ISO100，画质最佳；② 婚礼室内无闪灯可开到 ISO1600；③ 极光星空长曝可接受 ISO3200 但后期一定要降噪。',
 '噪点,信噪比SNR,原生双ISO,高感降噪,长曝降噪',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(10, '曝光补偿 (EV ±)', '曝光基础',
 '在相机自动曝光（P/Av/Tv 档）模式下，人工用 +1EV、-0.7EV 等指令干预最终亮度，+是加亮、-是压暗；一般以 1/3 档为最小步长。',
 '核心口诀是「白加黑减」：画面大面积是白色/亮色（雪、婚纱、天空）→ 自动曝光会压暗，要 +1~+2EV；大面积黑色/暗色（黑衣、夜景、黑背景）→ 自动曝光会提亮，要 -0.7~-1.3EV。',
 '① 拍雪天人像：相机自动把雪拍成灰 → +1.3EV 雪变白；② 黑背景舞台人像 → -0.7EV 避免背景发灰。',
 '白加黑减,A档光圈优先,T档快门优先,测光模式,直方图',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 分类 3/6 · 对焦与镜头（id=11~15） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(11, '景深 (Depth of Field)', '对焦与镜头',
 '被摄主体前后一段能保持"锐利可接受"的范围；范围内清晰、范围外逐步虚化。由光圈、焦距、拍摄距离三者共同决定。',
 '经验公式：景深 ∝ 1/光圈、∝ 1/焦距²、∝ 距离²。即：开大光圈 / 长焦 / 凑近拍 → 景深极浅、人物突出；缩小光圈 / 广角 / 远距离 → 景深很大、全景清晰。',
 '① 微距镜头 f/8 + 10cm 距离拍花，景深可能只有 0.5mm 薄如纸张；② 16mm 广角 f/11 + 5m 超焦距拍街景，从 0.8m 到无穷远全画面锐利。',
 '光圈,焦距,超焦距,焦平面,弥散圆',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(12, '焦距 (Focal Length)', '对焦与镜头',
 '镜头光学中心到相机传感器（焦平面）的物理距离，单位 mm；决定拍摄视角宽窄：数字小=广角（容纳多）、数字大=长焦（拍得远）。',
 'APS-C 相机要乘"裁切系数"（佳能 1.6、索尼/尼康 1.5），所以 50mm 在 APS-C 实际等效约 75~80mm，视角更接近人像黄金 85mm。常见焦段：14-24 超广角风光、24-70 标准挂机、70-200 中长焦人像体育。',
 '① 手机主摄 24~28mm 等效广角；② 85mm f/1.4 被称为人像皇，距离 3m 左右拍半身裁切刚好、背景压缩自然；③ 400mm+ 长焦可拍月亮环形山。',
 '等效焦距,裁切系数,定焦镜头,变焦镜头,人像焦段',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(13, '对焦模式 (AF-S / AF-C / MF)', '对焦与镜头',
 '自动/手动对焦的三大模式：AF-S(单次)适合静物人像、AF-C(连续)适合运动追焦、MF(手动)适合微距/星空/特殊场景。',
 '搭配 AF 区域模式：单点、扩展、广域、追脸/追眼（现代相机 Eye-AF 能自动锁定眼睛，人像成功率>95%，建议常开）。',
 '① 拍婚礼：AF-C + 人眼追焦 + 连拍，新人走动中也能保证每张眼睛锐利；② 拍银河：三脚架固定 + 切换 MF，实时放大 10x 手动拧到最远处星星锐利。',
 '人眼追焦Eye-AF,单点对焦,广域对焦,连拍,后键对焦AF-ON',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(14, 'Bokeh 焦外虚化', '对焦与镜头',
 '源自日语「ボケ（boke）」模糊；特指焦外区域散景的质感。受光圈叶片形状、镀膜、光圈开口形状影响。',
 '圆形光圈叶片（9 片以上）能产生更圆润的光斑；老镜头或反射镜头会有甜甜圈形状的 Bokeh；夜景点光源可得到漂亮的圆形光斑。',
 '① 85mm f/1.4 拍人像，背景霓虹灯变成一个个圆润彩色圆斑；② 折返镜 500mm f/8 的焦外点光源是甜甜圈环形的特殊散景。',
 '光圈,85mm人像,散景,口径蚀,焦外二线性',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(15, '大三元镜头', '对焦与镜头',
 '摄影圈对三支恒定 f/2.8 光圈高端变焦镜头的俗称：超广角 14-24/16-35 f/2.8、标准 24-70 f/2.8、中长焦 70-200 f/2.8；价格昂贵、画质顶级、覆盖 90% 拍摄场景。',
 '三兄弟组合可以"一机三镜拍天下"，从风光广角到人像中焦再到体育长焦都覆盖；缺点是每支都在 700~1500g 之间，一天背下来肩颈会酸。',
 '① 婚礼摄影师标配三兄弟：教堂室内广角拍场面、24-70 拍仪式流程、70-200 拍新人特写和表情。',
 '小三元(f/4),恒定光圈,变焦镜头,索尼GM,佳能红圈,尼康金圈',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 分类 4/6 · 光线与色彩（id=16~20） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(16, '色温 (Color Temperature)', '光线与色彩',
 '衡量光线颜色冷暖的指标，单位开尔文(K)；K 值低=偏黄橙（2700K 暖灯泡、日出日落黄金光），K 值高=偏蓝（6500K 阴天、9000K 阴影、闪光灯 5500K）。',
 '相机白平衡设置：自动 AWB 大部分可用；日落/蜡烛把白平衡调低到 3200K 保持暖调；雪地阴影把白平衡调到 6500K+ 纠正蓝紫偏色。',
 '① 早晨 6 点黄金小时拍人像，色温约 3200K，拍出的皮肤呈金色暖色调；② 白炽灯室内如果用 AWB 会偏黄，可手动设 2800K 让皮肤变白。',
 '白平衡WB,自定义白平衡,Kelvin,色偏,灰卡校准',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(17, '黄金小时 (Golden Hour)', '光线与色彩',
 '日出后约 1 小时内、日落前约 1 小时内，太阳角度低、光线柔和温暖的拍摄窗口期；是拍人像风光的最佳时间。',
 '此时光线三大优点：① 色温低，金色调；② 光线斜射形成强烈明暗立体，人脸五官有轮廓；③ 阴影长，造型丰富。夏天高纬度地区黄金小时可能长达 2 小时。',
 '① 下午 5 点在海边逆光拍剪影；② 早晨 7 点侧光拍人像，脸上光影层次丰富。',
 '蓝色小时Blue Hour,逆光,侧光,剪影,软光硬光',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(18, '白平衡 (White Balance / WB)', '光线与色彩',
 '告诉相机「什么颜色是中性白」，让相机按这个基准调整整张照片色彩；目的是还原被摄物真实固有色，消除光线本身的色偏。',
 '原理：白纸在任何灯光下物理固有色都是白，所以要把当前环境里的白纸调成 RGB(200,200,200) 附近的中性灰。常用方法：自动 AWB、日光/阴影/白炽灯预设、自定义灰卡校准、RAW 后期调。',
 '① 室内日光灯下拍出来的人像脸发青 → 手动调白平衡荧光灯预设或取一张白纸做自定义 WB；② 日落故意设成阴天预设保留金黄色氛围。',
 '色温Kelvin,RAW格式,灰卡,自定义白平衡,色彩漂移',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(19, '直方图 (Histogram)', '光线与色彩',
 '在相机液晶屏 / Lightroom / Photoshop 中显示的柱状图：横轴从左到右代表暗→亮，纵轴代表对应亮度像素数量；用来判断曝光是否过曝/欠曝。',
 '安全区间：整座"山"落在两头边界以内（没有贴左=欠曝死黑/贴右=过曝死白），两端不撞墙就是曝光安全；人像可以整体偏右（向右曝光法，减少暗部噪点）。',
 '① 拍雪白连衣裙：直方图山峰集中在右侧，但右边缘不能贴墙（贴墙=裙子细节丢失死白）；② 拍夜景剪影：山峰集中在左侧，保留一点点黑色层次即可。',
 '向右曝光ETTR,死黑,死白裁切,过曝警告,阴影高光',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(20, '动态范围 (Dynamic Range)', '光线与色彩',
 '相机传感器一次曝光能同时记录的「最暗细节」到「最亮细节」的亮度跨度，以 EV（档）为单位；人眼约 20EV、现代全幅相机 14~15EV、手机多帧合成可达 20EV+。',
 '场景动态范围超过相机怎么办？① 包围曝光 3~5 张 + 后期 HDR 合成；② 用中灰渐变镜 GND 压暗天空；③ 用 RAW 格式 + 后期提亮阴影/压暗高光（单帧挽救空间大）。',
 '① 正午大光比风光：天空亮地面暗→用 GND0.9 减光压暗天空三档；② 逆光人像：相机直出人脸黑→切换 RAW+后期提亮暗部 2EV。',
 '包围曝光,HDR合成,渐变镜GND,RAW,影调分离',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 分类 5/6 · 闪光灯与附件（id=21~25） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(21, '闪光灯 GN 值 (Guide Number)', '闪光灯与附件',
 '衡量闪灯功率大小的国际标准：GN = 拍摄距离(m) × 光圈(f 值)，在 ISO100 下测定；数字越大闪灯越猛。',
 '示例：神牛 V1 机顶闪 GN60，ISO100 下 f/4 时有效距离 = 60/4 = 15m；2.8 时可达 21m，足够照亮 15 人大合影。',
 '① 婚礼室内跳闪：机顶闪 f/4 + 距离 5m，选 GN≥20 的闪灯即可；② 大型商业棚拍静物选 GN120+ 的专业影室灯。',
 '机顶闪,影室灯,TTL自动闪光,跳闪,离机引闪',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(22, '跳闪 (Bounce Flash)', '闪光灯与附件',
 '把机顶闪光灯头朝上/朝向白墙/天花板打光，借墙面反射形成巨大柔和面光；替代硬光直射避免"大油脸+红眼+死白背景"。',
 '跳闪三大禁忌：① 天花板太高（>4m）反射回来太弱；② 彩色天花板会把颜色染到人脸（酒店红色天花板慎用）；③ 广角镜头灯头要配扩散片，否则画面中心亮边缘暗。',
 '① 室内派对直闪人像难看→灯头转 45° 弹到米白天花板，拍出柔和自然的眼神光；② 没有天花板→背后架一块白色柔光板朝板子打。',
 '柔光箱,柔光伞,直闪,红眼效果,反射闪光,眼神光',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(23, 'ND 减光镜 (Neutral Density)', '闪光灯与附件',
 '一块均匀灰色的滤镜，物理上衰减进入镜头的所有光线；不改变色彩，只允许"在大白天故意放慢快门"做特殊效果。',
 '常见规格：ND4(减 2 档)/ND8(3 档)/ND64(6 档)/ND1000(10 档)。ND1000 能让正午 1/1000s 变成 1s，是白天拍丝缎瀑布/流云/人流拉丝的神器。',
 '① 黄果树瀑布正午：f/11+ISO100 最快也只能 1/30s（水还是颗粒感）→ 加 ND1000 变成 30s，水流变牛奶丝滑；② 白天慢门 6s 把天安门广场人群全部虚化消失。',
 '中灰镜,ND1000,流云拉丝,长曝光,偏振镜CPL,渐变镜GND',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(24, 'CPL 偏振镜 (Circular PL)', '闪光灯与附件',
 '两块偏振玻璃组成的可调角度滤镜，能消除玻璃/水面/非金属表面的定向反光，并让天空更蓝、云层更立体、色彩饱和度显著提高。',
 '旋转外圈，旋转到某个角度时：① 汽车/橱窗反光完全消失；② 水面可以看见水下的鱼和石头；③ 天空蓝色从白蒙蒙的灰蓝变成纯净的西藏蓝。',
 '① 橱窗展品前拍商品：转 CPL 消掉室内灯光的玻璃反光；② 西藏风光必带 CPL：天空的蓝度和云朵立体感肉眼可见地提升。',
 '偏振镜,消除反光,渐变镜GND,滤镜口径,UV镜',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(25, '三脚架选购要点', '闪光灯与附件',
 '稳定相机的必备附件；选购四大指标：最大承重≥相机+镜头的 2 倍、材质（铝合金便宜但重，碳纤维贵但轻 30%）、节数（3 节比 5 节稳）、球形/三维云台（球形快、三维精密）。',
 '经验法则：能买碳纤维别买铝合金（一天轻 1kg 肩颈幸福感差很多）；云台一定要买与脚架同价位等级的，不要脚架 3000 云台 200。',
 '① 长曝星轨必须三脚架 + 快门线/定时拍摄；② 微距翻拍 1:1 花、产品细节必须三脚架。',
 '碳纤维三脚架,球形云台,三维云台,快装板,快门线,延时摄影',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 分类 6/6 · 后期与文件格式（id=26~30） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(26, 'RAW 格式', '后期与格式',
 '相机传感器输出的"未经处理的原始光电信号数据包"，含 12~14bit 高色深和完整动态范围，是专业摄影师的必存格式。',
 '对比 JPEG：JPEG 是相机内置 DSP 压缩过的 8bit 文件，后期提亮/压暗 1EV 就开始出噪点和色断层；RAW 可在 Lightroom 中 ±3~5EV 仍保留细节，白平衡可无损重调。',
 '① 逆光人像脸黑：JPEG 提亮 2EV 会有大片噪点和色阶断层；RAW 提亮 3EV 皮肤仍干净，高光部分天空云细节能重新拉回来。',
 'JPEG,Adobe DNG,高动态范围,14bit,无损压缩',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(27, 'HDR 合成 (High Dynamic Range)', '后期与格式',
 '同一场景连续拍 3~5 张（-2EV 欠曝、0EV 正常、+2EV 过曝，步进 2EV），后期用 Lightroom/Photomatix 合成一张同时保留高光+暗部细节的超高动态范围影像。',
 '关键前提：必须三脚架固定相机，否则合成时画面会重影；运动物体（人流、云）要开"HDR 去鬼影"功能。',
 '① 正午阳光照古镇街道：光比大，直出要么天空死白要么暗部全黑→包围曝光 3 张合成 HDR，两端细节都保留；② 建筑室内外一体场景必备。',
 '包围曝光,影调,Photomatix,Adobe Lightroom,移动HDR算法',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(28, '色阶 (Levels) 与曲线 (Curves)', '后期与格式',
 'Photoshop/Lightroom 中最常用的两大色调调节工具：色阶是三点（黑场/灰场/白场）控制影调，曲线是任意 N 个控制点自定义 S 形对比度。',
 '万能修图第一步：打开曲线 → 两端点收回直方图两端山脚下 → 拉一个微微的 S 形（提亮上半段+压暗下半段），整张图立刻通透起来。',
 '① 灰蒙蒙的阴天风景照：曲线 S 型 + 白场吸管点最亮云、黑场吸管点最暗树枝，瞬间有反差；② 肤色偏黄：曲线单独拉蓝通道轻微提升上半段。',
 '直方图,Photoshop,Lightroom,色彩平衡,色相饱和度HSL',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(29, '锐化 (Sharpening)', '后期与格式',
 '后期加强画面边缘像素对比，让照片看起来更"锐"更清晰；但过度锐化会产生白边、白边晕、噪点。',
 '四步锐化法：① 蒙版只锐化边缘，天空/皮肤别锐化（越锐越脏）；② Amount 100%~120%；③ Radius 0.8~1.2（风景稍大，人像稍小）；④ Masking 按住 Alt 拖动，只保留硬边缘部分。',
 '① 人像修图最后一步：蒙版只锐化眼睛、睫毛、眉毛、嘴唇，脸颊额头完全不碰；② 风景照：加大锐化量，蒙版保留山河树木石头轮廓。',
 'USM锐化,高反差保留,智能锐化,锐化蒙版,细节丢失',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(30, 'EXIF 信息', '后期与格式',
 'JPEG/RAW 文件头内嵌的拍摄元数据，包含：相机/镜头型号、光圈/快门/ISO/焦距、拍摄日期时间、GPS 定位（如果开了）、闪光灯是否触发等。',
 '社影通照片打分功能就是通过 Qwen-VL 多模态看图分析；专业比赛的照片评委可以用 EXIF 查看你是否达到参赛参数要求。也可以通过 Bulk Rename Utility 按 EXIF 的 yyyyMMdd_HHmmss 批量重命名照片。',
 '① 手机图库"信息"里看拍摄参数，学习别人好片的光圈焦距设置；② 版权登记时 EXIF 能证明这张照片是你的相机在某时刻拍的。',
 '元数据,GIS地理标签,XMP,IPTC版权信息,批量重命名',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- =====================================================================
-- 第 2 节 · 器材分类（6 条）
-- 表名：gear_categories
-- =====================================================================

INSERT IGNORE INTO gear_categories (id, name, description, icon) VALUES
(1, '数码微单', '以 Sony A7 系列、Canon R 系列、Nikon Z 系列为代表的无反光镜可换镜头相机，轻便且视频性能强', '📷'),
(2, '数码单反', '传统反光镜结构的可换镜头相机（Canon EOS / Nikon D 系列），光学取景直观、续航强', '📸'),
(3, '镜头', '可换镜头系统：定焦、变焦、广角、长焦、微距、移轴等', '🔭'),
(4, '三脚架', '稳定支撑系统，长曝光、夜景、微距、延时摄影必备', '🗼'),
(5, '滤镜系统', 'UV、CPL 偏振、ND 减光、GND 渐变、黑柔焦等光学滤镜', '🪟'),
(6, '闪光灯与灯光', '机顶闪、离机闪、常亮补光灯、柔光箱、反光板等', '💡')
ON DUPLICATE KEY UPDATE name = name;


-- =====================================================================
-- 第 3 节 · 器材示例（10 条，覆盖 6 大分类，价格为 2026 参考价人民币）
-- 表名：gear
-- =====================================================================

-- ------- 微单 3 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(1, 'Sony', 'Alpha A7 IV 全画幅微单', 1, 15999.00,
 '2022 年度水桶机王，3300万像素全画幅背照 + 5 轴防抖 + 4K60p 10bit 4:2:2 视频；AI 实时人眼/鸟眼追焦；兼顾拍照和视频双修。',
 NULL,
 '3300万像素全画幅', '10张/秒机械快门', '4K 60p 10bit 4:2:2',
 'AI人眼/鸟眼实时追焦',
 '传感器:IMX410 | ISO:100-51200可扩展到204800 | 防抖:5轴5.5档 | 存储:双SD/CFexpress | 重量:658g | 电池:NP-FZ100',
 5, 4823,
 '全画幅双修首选，5 轴防抖 + AI 追焦在弱光人像、婚礼跟拍场景可靠性拉满；视频侧翻屏对 vlogger 友好；建议搭配 24-70GM II 挂机。', 96, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(2, 'Canon', 'EOS R6 Mark II 全画幅微单', 1, 16499.00,
 '佳能 2022 年发布的第二代全幅微单，2420 万像素 + 双核 CMOS AF II + 最高 40 张/秒电子快门；对肤色直出党是人像王。',
 NULL,
 '2420万像素全画幅', '40张/秒电子快门', '6K超采样4K60p',
 '双核CMOS AF II 全覆盖',
 '处理器:DIGIC X | 连拍:RAW+JPEG缓存680张 | 防抖:机身RF镜头协同8档 | 侧翻屏 | 重量:680g',
 5, 2341,
 '人像直出色彩无敌，双核 AF II 识别成功率高；弱光对焦 EV-6.5 也能咬住人脸；喜欢婚礼、人像、街拍风格的佳能老用户首选。', 95, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(3, 'Nikon', 'Z6 III 全画幅微单', 1, 15299.00,
 '尼康 2024 年 Z6 三代，3600 万像素 + 堆栈式 CMOS 读速度加倍；12bit RAW 电子前帘；N-Log Z 视频调色友好。',
 NULL,
 '3600万像素堆栈式', '12张/秒', '6K 超采样4K60p 12bit',
 '3D追焦+动物/车辆',
 '处理器:EXPEED 7 | 防抖:机身+镜头协同7档 | 存储:双CFexpress | ISO:64-25600原生 | 重量:695g',
 5, 1890,
 '高像素+高视频规格的兼顾型；Nikon Z 卡口镜头群近年进步神速，S-Line 50/1.2、85/1.2 画质逆天；风景用户会被 3600W 像素细节打动。', 94, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 镜头 4 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(4, 'Sony', 'FE 24-70mm F2.8 GM II 标准变焦', 3, 16999.00,
 '索尼 2022 二代大三元标准变焦；695g 比一代轻 30%；近摄能力强悍，24mm 端最近对焦 21cm，可拍半微距。',
 NULL, NULL, NULL, NULL, 'DDSSM 超声波马达',
 '镜片:13组17片(2片XA+3片非球+2片ED) | 最近对焦:0.21m | 最大放大:0.32x | 滤镜:82mm | 重量:695g',
 5, 5620,
 '全画幅标准变焦之王！画质与重量的黄金平衡点；人像/风景/街拍/婚礼挂机一支搞定，2026 年依旧是大三元标变首选。', 98, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(5, 'Sony', 'FE 85mm F1.4 GM II 人像定焦', 3, 12999.00,
 '人像镜皇二代；11 片圆形光圈叶营造奶油 Bokeh；XA 超级非球镜片全开边角锐利；对焦呼吸效应极低，视频对焦平滑。',
 NULL, NULL, NULL, NULL, 'XD线性马达',
 '镜片:8组11片(1片XA+3片ED) | 最近对焦:0.55m | 散景:11片光圈叶 | 重量:778g | 滤镜:77mm',
 5, 3120,
 '人像摄影师的终极武器；f/1.4 全开锐度惊人，焦外虚化圆润不杂乱；配合 A7R V 6100 万像素时，人像发丝根根分明如数头发。', 97, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(6, 'Canon', 'RF 50mm F1.2 L USM 标准定焦', 3, 15499.00,
 '佳能 RF 卡口旗舰 50/1.2；2 片非球 + 1 片 UD 超低色散；全开边缘素质超高，锐度和虚化兼得；人像/纪实双修。',
 NULL, NULL, NULL, NULL, '环形USM',
 '镜片:9组15片 | 最近对焦:0.4m | 光圈叶片:11片圆形 | 滤镜:82mm | 重量:950g',
 5, 1520,
 '50mm 人文焦段之王；搭配 R5 R6 人像直出肤色非常讨喜；是「拍女朋友永远不被骂」这一应用场景的最佳镜头。', 95, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(7, 'Tamron', 'A056 70-180mm F2.8 Di III VXD 长焦变焦', 3, 8380.00,
 '腾龙为索尼 E 卡口量身打造的长焦大三元；仅 810g + 149mm 长度，轻便到完全颠覆 70-200 f/2.8 的沉重印象。',
 NULL, NULL, NULL, NULL, 'VXD 线性马达',
 '镜片:12组19片 | 最近对焦:0.85m@70端 / 1.0m@180端 | 放大倍率:1:4.6 | 滤镜:67mm | 重量:810g',
 5, 2012,
 '穷玩党福音，索尼大三元长焦的最佳「不破产版」；画质接近原厂 GM 1.05x 倍数，价格只有一半；婚礼摄影师、单兵作业首选。', 93, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 三脚架 1 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(8, 'Gitzo', 'GT1545T 旅行者 1 号碳纤维三脚架', 4, 6800.00,
 '捷信百年顶级碳纤维；1.5 公斤自重承重 10 公斤；折叠仅 42cm 可登机；6X 碳纤维管吸振性能极强，长曝 30s 画面不抖。',
 NULL, NULL, NULL, NULL, NULL,
 '节数:4节 | 材质:6X碳纤维 | 承重:10kg | 自重:1.49kg | 最大高度:167cm | 折叠长度:42cm',
 5, 720,
 '三脚架里的劳斯莱斯，一次投资 15 年不换；长曝星轨、风光旅拍神器；买它的唯一理由就是"轻+稳+100%不后悔"。', 98, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 滤镜 1 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(9, 'NiSi', 'V7 滤镜支架套装（CPL + GND8 + ND1000）', 5, 1280.00,
 '耐司方形支架旗舰 V7；自带 True Color CPL 不偏色；赠送软渐变 GND8 与 ND1000 减光镜；风光摄影师入门三件套。',
 NULL, NULL, NULL, NULL, NULL,
 '口径:100mm 方形 | 含:V7支架+TrueColorCPL+ND1000(10档)+GND8(3档软渐变) | 接圈:67/72/77/82mm',
 5, 980,
 '风光摄影「白天能把水流拍成丝缎、流云拍成奶油」的最低成本一步到位方案；支架+CPL+ND+GND 全齐，直接出门拍风景。', 94, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 闪光灯 1 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(10, 'Godox', 'V1 Pro 圆头机顶闪光灯', 6, 1880.00,
 '神牛 2025 年圆头灯旗舰；GN65 大功率 + 2600mAh 锂电 480 次全光回闪；支持 TTL 自动跳闪、HSS 高速同步 1/8000s；人像婚礼跟拍利器。',
 NULL, NULL, NULL, NULL, 'TTL/M/Multi',
 '灯头:圆形 | 功率:GN65 | 供电:VB26A 2600mAh | 回电:0.01~1.2s | 触发:2.4G X系统 | HSS:1/8000s',
 5, 4200,
 '机顶闪界千元档没有对手；圆灯头光质接近 Profoto A10 但价格只有 1/4；TTL 精准 + 光质软 + 电池给力，婚礼、商拍、街拍都胜任。', 92, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- =====================================================================
-- 第 4 节 · 用户表不在这里手动插！
-- （密码是 BCrypt 哈希，写在 SQL 里是死的不优雅。
--  两个默认账号 demo/123456、tester_001/123456
--  由 SeedDataRunner.java 在用户表空的时候实时 passwordEncoder.encode 写入。）
-- =====================================================================
