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
-- 第 1 节 · 摄影术语（共50条，覆盖7大类，id=1~50 连续）
--   · id=1~30 ：基础6大类（构图/曝光/对焦/光线/闪光/后期）
--   · id=31~50：增补7个进阶板块（构图进阶/曝光进阶/对焦进阶/光线进阶/闪光进阶）
-- 表名：photography_terms （@Table(name="photography_terms")）
-- 唯一索引：term 列 → 所有 INSERT 都是 INSERT IGNORE，重复启动不会报错
-- ⚠️ 【乱码/旧数据清理】如果你 MySQL 里旧的 photography_terms 表有乱码/重复/脏数据：
--    1. mysql -uroot -p123456 sheyingtong -e "TRUNCATE photography_terms;"
--    2. 重启 Spring Boot → data.sql 重新把 50 条干净术语插入
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
(25, '三脚架', '闪光灯与附件',
 '由三条可伸缩支腿组成的相机稳定支撑系统；是长曝光、微距、夜景、延时、接片摄影的必备附件；核心作用是消除手抖造成的画面模糊。',
 '选购四大指标：(1)最大承重≥相机+镜头总重的2倍，留安全余量；(2)材质——铝合金便宜耐用但重，碳纤维轻30%但贵；(3)节数——3节比5节稳定性好；(4)球形云台操作快、三维云台角度精。经验：脚架投入不要低于机身价格的1/5，云台和脚架同价位等级。',
 '① 银河长曝30s必须三脚架+快门线，否则再高ISO放大都是糊；② 1:1微距拍花蕊，手一抖景深就歪了，三脚架+实时放大对焦成功率翻倍；③ 接片全景风光，三脚架球台带水平仪保证每张地平线一致。',
 '碳纤维脚架,球形云台,三维云台,快装板,云台,慢门长曝,延时摄影',
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
-- 第 1.7 节 · 增补常用摄影术语（id=31~50，新增覆盖构图/曝光进阶/对焦进阶/光线进阶/闪光进阶）
-- 唯一索引：term 列 → INSERT IGNORE 保证二次启动不冲突
-- =====================================================================

-- ===== 增补 1/7 · 构图进阶（id=31~36） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(31, '留白构图', '构图基础',
 '在画面主体周围有意保留大面积空白（天空、白墙、水面、纯色背景等），通过"空"反衬"实"，营造极简、安静、想象空间的构图方法。',
 '留白≠空白，是东方美学"少即是多"在摄影中的体现。留白区域通常占到画面 40%~70%，把主体压缩到 1/3 以内的小区域，立刻有高级感和呼吸感。适合极简风光、禅意人像、产品摄影。',
 '① 海边地平线放最下1/6，上方全是云朵+一只小船=留白；② 纯色白墙背景拍人像，人物只占画面右侧1/3，左边全空。',
 '极简摄影,负空间,东方美学,画面呼吸感,三分法',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(32, 'S形构图', '构图基础',
 '画面主线条（道路、河流、沙丘、光影边缘）呈现英语字母 S 形状的构图；能最大程度延展画面纵深，引导观众视线曲折深入到画面远处。',
 'S 形曲线比直线更柔和有韵律，是最优雅的引导线类型；在风光、街拍、艺术人体中极其常用。也可以是反向 S 形（Z 字形），效果等价。',
 '① 盘山公路从上往下拍形成完美S形弯道；② 溪流在森林里蜿蜒流过形成S形主引导线。',
 '引导线构图,Z形构图,曲线,风光构图,纵深感',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(33, '负空间构图', '构图基础',
 '与留白构图类似，强调画面中"主体以外的空闲区域（负空间）"与"主体（正空间）"的比例关系；负空间 ≥ 正空间的 2 倍时视觉效果强烈。',
 '正空间=主体所在的实体区，负空间=围绕主体的空旷区。负空间作用：① 给视线"休息"的地方；② 让主体更加突出；③ 给阅读或标题预留位置（杂志封面的标准做法）。',
 '① 一只鹰在画面右上角翱翔，左下方 80% 全是纯蓝天空=负空间；② 沙漠中一个极小的人走在中间，四周全是金色沙丘。',
 '留白构图,极简主义,杂志封面构图,画面重心',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(34, '对角线构图', '构图基础',
 '把画面中最主要的线条、物体边缘、光影分隔沿对角线（45°角）方向延伸摆放，打破水平/垂直的稳定感，使画面充满张力与动感。',
 '人类看画面的阅读顺序是"左上→右下"，对角线正好契合视觉动线，画面一眼看完，不会显得呆板。对角线可分为左上→右下和右上→左下两种方向。',
 '① 滑板运动员跃起时身体正好落在右上→左下的对角线上，动感强烈；② 森林道路从左下向右上延伸直达远方山顶。',
 '三分法,动感构图,视觉动线,斜线构图,汇聚透视',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(35, '中心构图', '构图基础',
 '把主体严格放在画面正中心（几何中心），四周留对称背景；是最容易上手、最稳妥的构图方式，但需要主体自身足够好看才能避免呆板。',
 '适用场景：① 本身对称的建筑/倒影/产品；② 有强烈视觉冲击力的主体（如大眼睛、大花、海报式人像）；③ 正方形画幅 1:1 最适合中心构图。可搭配"中心+四周暗角"增强聚焦。',
 '① 1:1正方形画幅拍正面特写的人像，眼睛在中心，四周略暗角；② 正圆形花朵居中拍微距，花瓣呈完美放射状。',
 '对称构图,1:1正方形画幅,中心聚焦,暗角vignette,海报式构图',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(36, '低角度仰拍', '构图基础',
 '摄影师蹲下、趴下或放低机位，相机朝斜上方拍摄；与常规平视视角形成对比，能夸张透视、使人物/建筑显得高大挺拔、天空占据画面大部分。',
 '低机位三大优势：① 透视夸张，腿显长、楼显高（女生拍照最爱的"大长腿机位"）；② 前景杂物（地面垃圾、路人）被排除到镜头下方以外，画面干净；③ 画面以天空为背景，主体立刻突出。',
 '① 拍女生全身人像：蹲下来机位只到膝盖高度，从下往上拍，24mm 广角立刻拉出 1 米 8 大长腿；② 拍摩天大楼：躺在地面朝上仰拍，建筑汇聚到天上一点更震撼。',
 '高角度俯拍,广角夸张透视,机位选择,拍摄角度,上帝视角',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 增补 2/7 · 曝光进阶（id=37~40） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(37, '向右曝光 (ETTR)', '曝光基础',
 'Expose To The Right 的缩写，在高光不过曝的前提下，刻意把直方图整体"向右（亮部方向）"移动一点再拍；目的是用传感器更大的动态范围存储亮部信号，后期提亮暗部时噪点显著减少。',
 '原理：数码相机传感器对亮度是线性采样的，最亮那档（最右 1EV）的信息量占了总信息的一半，越往左信息越少。因此同一场景，偏右曝光的 RAW 后期提亮后暗部噪点比正常曝光少 2~3 倍。人像、风光、弱光场景推荐。',
 '① 拍 RAW 风光：测光后再加 +0.7~+1.0EV 向右曝光，直方图山峰靠右但右边不贴墙=完美；② 暗部占比大的室内婚礼：+0.7EV 向右拍，后期拉回阴影干净。',
 '直方图,RAW格式,死白裁切,暗部噪点,曝光补偿',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(38, '包围曝光 (Bracketing)', '曝光基础',
 '对同一个构图，连续拍 3 张或 5 张：-2EV（欠曝）、0EV（正常）、+2EV（过曝），每张相差 1~2EV；目的是应对相机单次记录不下的高动态范围场景，供后期 HDR 合成或挑选。',
 '现代相机内置 AEB（Auto Exposure Bracketing）自动包围，按一次快门连拍 3 张。前提：三脚架固定，否则后期对不齐。除了曝光包围，还有白平衡包围、景深包围、对焦包围等进阶包围类型。',
 '① 正午大光比风光：天空亮地面暗，包围 3 张（-2/0/+2）→ 后期 Lightroom HDR 合成，天空云和地面草地细节都有；② 不确定曝光是否准确时：包围 3 张选最满意的一张即可，比错了强。',
 'HDR合成,光比大,AEB自动包围,三脚架,焦点包围',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(39, '测光模式 (Metering)', '曝光基础',
 '相机内部测光元件测量画面光线强度、给出推荐曝光值的算法模式；常见四种：评价/矩阵测光（全局平均）、中央重点测光、局部测光、点测光。',
 '选择原则：① 光线均匀=评价测光（90%场景默认）；② 主体占画面很小且逆光（如舞台聚光灯、窗边人像）=点测光（只测主体 2%~4% 小区域）；③ 中间大主体+边缘不重要=中央重点。',
 '① 拍舞台追光灯下的歌手：点测光对着歌手脸部测，避免背景全黑导致歌手过曝；② 拍明亮窗边逆光的人像：点测光人脸后按 AEL 锁定再构图。',
 '点测光,评价测光,中央重点测光,AEL曝光锁定,逆光',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(40, '安全快门', '对焦与镜头',
 '手持拍摄时，为了避免手抖导致整张照片虚糊，所需的最低快门速度；经验公式：安全快门（秒） ≈ 1 / 等效焦距（mm）。',
 '举例：全幅 50mm → 1/50s 以上；APS-C 50mm 等效 75mm → 1/80s 以上；长焦 200mm → 1/200s 以上。低于这个速度，要么上三脚架，要么开镜头/机身防抖，要么提高 ISO 加快快门。注意：有 5 轴防抖可放宽 4~5 档。',
 '① 全幅 200mm 长焦打鸟，快门只有 1/60s=几乎全糊，需 ISO 拉高到 1/200s 以上；② 手机 28mm 主摄手持，1/30s 基本不会糊，1/15s 开始糊。',
 '防抖IS/OSS/IBIS,焦距,等效焦距,手抖模糊,三脚架',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 增补 3/7 · 对焦与镜头进阶（id=41~43） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(41, '超焦距 (Hyperfocal Distance)', '对焦与镜头',
 '当镜头对焦到某个特定距离时，景深会从该距离的一半处一直延伸到无穷远，这个"特定距离"就叫超焦距；是风光摄影保证前后景都锐利的黄金法则。',
 '使用方法：① 切 MF 手动对焦；② 打开相机景深预览或峰值对焦；③ 拧对焦环到无穷远再往回一点点，让无穷远刚好处在景深后边缘。此时前景能清晰到 1~2 米的距离。现代广角镜头一般有超焦距刻度线。',
 '① 24mm 广角 f/11 拍街景：切 MF 超焦距 → 从 1.2 米到无穷远全锐利，举起就拍不用对焦，扫街神器；② 风光全景接片必须超焦距，保证每张景深一致。',
 '景深,手动对焦MF,广角镜头,峰值对焦,景深预览',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(42, '微距摄影 (Macro)', '对焦与镜头',
 '以 1:1 甚至更高放大倍率拍摄微小物体（昆虫、花蕊、产品细节）的摄影门类；此时景深极浅（常常只有 0.5~1mm），对焦是最大挑战。',
 '三种实现方案：① 真微距镜头（如 90mm f/2.8 Macro、百微 100mm f/2.8，放大倍率 1:1）；② 反接标头（便宜方案，画质尚可）；③ 近摄接圈（装在镜头与机身之间延长像距）。拍摄要点：三脚架 + 实时放大对焦 + 补光。',
 '① 百微 1:1 拍蜻蜓复眼，景深只有 0.3mm，必须用对焦堆叠（20 张不同焦点合成）；② 珠宝拍产品细节：f/11 + 环形闪光灯补光避免阴影。',
 '对焦堆叠Focus Stacking,放大倍率,百微镜头,环形闪光灯,近摄接圈',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(43, '等效焦距 (35mm等效)', '对焦与镜头',
 '把不同传感器尺寸（全幅 / APS-C / M43 / 手机）下的实际焦距，统一换算成"全幅相机上产生相同视角"的焦距值，单位 mm；方便跨设备横向对比视角。',
 '换算公式：等效焦距 = 实际焦距 × 裁切系数。常见裁切系数：全幅 1.0x，索尼/尼康 APS-C 1.5x，佳能 APS-C 1.6x，M43 2.0x，手机主摄 6~8x。例：索尼 APS-C 机身上 50mm 实际焦距 → 等效 75mm 人像视角。',
 '① 手机主摄实际 4mm + 裁切系数 7x → 等效 28mm 广角，和相机 28mm 镜头同视角；② APS-C 想拍 85mm 人像 → 实际买 56mm 定焦（56×1.5≈85）。',
 '裁切系数,全画幅,APS-C,M43,传感器尺寸',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 增补 4/7 · 光线与色彩进阶（id=44~47） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(44, '蓝色小时 (Blue Hour)', '光线与色彩',
 '日出前约 30 分钟、日落后约 30 分钟的短暂窗口期，天空整体呈均匀柔和的深蓝色/紫罗兰色，色温约 7000~20000K；是拍夜景、城市风光、人像蓝调的黄金时间。',
 '蓝色小时比黄金小时更稀有，只有约 20 分钟，必须提前踩点架好三脚架等时间。此时天空还有细节，地面灯光也亮了，对比不会太大，是拍城市天际线、桥梁、蓝调人像的最佳时机。',
 '① 日落后 15 分钟拍城市天桥：天空正好呈现梦幻宝蓝色，和地面路灯暖黄色形成冷暖对比；② 海边日落之后拍人像剪影，背景蓝调非常高级。',
 '黄金小时,城市风光,冷暖对比,长曝光,夜景摄影',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(45, '伦勃朗光 (Rembrandt)', '光线与色彩',
 '经典三点布光法之一，主光源在人物侧上方约 45°，让人物暗侧脸颊上形成一块倒三角形的亮斑（约鼻子大小）；因荷兰画家伦勃朗油画常用而得名；立体感极强。',
 '布置四步：① 主光 45°侧上打；② 脸另一侧放反光板/辅光（亮度差 2:1~4:1）；③ 背景光打背景分离人物；④ 发光打头发轮廓。适合男性肖像、成熟气质人像、证件照。',
 '① 男性人像：伦勃朗光拍出脸部骨骼感和阳刚气；② 文艺片剧照：只用一扇窗户做主光源，暗侧脸留小三角光斑=天然伦勃朗光。',
 '三点布光,蝴蝶光,伦勃朗三角光,影棚布光,窗户光',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(46, '硬光与软光', '光线与色彩',
 '按照光线"边界锐利程度"分类：硬光来自小光源（太阳、裸灯泡、裸闪灯），阴影边缘锐利清晰、反差强；软光来自大光源（柔光箱、阴天、窗光），阴影边界柔和渐变、反差低。',
 '选择原则：① 拍阳刚/硬朗/产品质感 → 硬光（单灯裸打）；② 拍女性/儿童/糖水片 → 软光（柔光箱、反光板反射）；③ 软光面积越大越柔和：2 米柔光伞 > 60cm 柔光箱 > 裸闪灯。',
 '① 拍手表：硬光小光源拍出金属表壳的高对比反光和质感；② 女生糖水片：3 米宽柔光白布+跳闪，脸部完全没有硬阴影，皮肤显光滑。',
 '柔光箱,柔光伞,反光板,窗户光,裸闪,蝴蝶光',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(47, '反光板 (Reflector)', '闪光灯与附件',
 '一块可折叠的硬质板（常用金银白三色），用来反射主光到暗部；是最便宜、最有效、最便携的补光工具，婚礼、户外人像必带。',
 '常见颜色用法：① 白色=中性软反光（最常用）；② 金色=傍晚黄金光场景，反射暖调；③ 银色=阴天/冷色环境，反射强冷调补光；④ 黑色=吸光板，减暗某侧打出阴影。尺寸至少 80cm 才够补全身人像。',
 '① 正午强光下的户外人像：侧逆光，人脸部全黑 → 白色反光板 45° 反到脸，立刻柔和补光；② 拍新娘化妆特写：小反光板补下巴阴影避免双下巴。',
 '柔光箱,跳闪,五点布光,户外人像,金银双色',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- ===== 增补 5/7 · 闪光灯与后期格式进阶（id=48~50） =====
INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(48, 'TTL 自动闪光', '闪光灯与附件',
 'Through The Lens 的缩写，闪灯通过镜头实时测光自动输出正确功率；类似相机自动曝光，是新手使用闪光灯的首选模式。',
 '工作流程：预闪（肉眼几乎不可见的低功率闪）→ 相机镜头测光 → CPU 计算正确功率 → 主闪实际触发。优点：不用自己算 GN 值、距离、光圈；缺点：会被镜面、白墙欺骗，必要时加闪光补偿 ±1~2 档。',
 '① 婚礼跟拍全程开 TTL：环境明暗变化快，TTL 自动调节功率出片率极高；② 黑背景 TTL 拍人像：闪光补偿 -0.7 档避免主体过曝。',
 'GN值,闪光补偿,手动M档闪光,离机引闪,机顶闪',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(49, '高速同步 HSS (High Speed Sync)', '闪光灯与附件',
 '允许闪灯在高于相机 X 同步速度（一般 1/200s~1/250s）的快门下也能触发的功能；最高可达 1/8000s，是正午大光圈户外人像补光的利器。',
 '原理：普通闪灯是一次强光脉冲，X 同步速度以上快门帘会切光造成黑条；HSS 模式下闪灯变为数万次连续高速频闪，让整个快门行程都有光照。代价是功率损耗大，有效距离缩短到原来的 1/2~1/3。',
 '① 正午阳光下拍人像：想用 f/1.8 虚化 → 快门要 1/4000s → 开 HSS + 闪灯压暗阳光 + 主体补光，立刻有商业片的干净背景虚化；② 体育摄影 1/2000s + 闪灯冻结动作。',
 '闪光同步速度X-sync,TTL,大光圈户外人像,压光,神牛AD系列',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO photography_terms
  (id, term, category, definition, explanation, examples, related_terms, created_at, updated_at) VALUES
(50, '暗角 (Vignette) 与色散 (CA)', '后期与格式',
 '暗角=画面四角比中心暗的光学现象；色散=高反差边缘（如树枝+天空）出现紫/绿边的色差（Chromatic Aberration）；两者都是镜头常见的光学缺陷，后期一键可修。',
 '暗角分三类：自然暗角（正常）、机械暗角（遮光罩或滤镜挡住）、像素暗角（传感器边缘接收光少）。色散分轴向色散（紫边）和横向色散（绿边）。现代 LR/PS 的「镜头校正」面板勾选配置文件即可自动修掉 95%。',
 '① 24mm 广角全开 f/1.4 四角明显暗角 → LR 「镜头校正+暗角修复」两滑块一拉即可；② 高反差树枝放大 1:1 有紫/绿边 → PS 镜头校正-色散-删除色差 勾上自动清。',
 '镜头校正,Lightroom,光学缺陷,紫边,边缘校正',
 NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- =====================================================================
-- 第 2 节 · 器材分类（6 条）
-- 表名：gear_categories
-- =====================================================================

INSERT INTO gear_categories (id, name, description, icon) VALUES
(1, '相机', '以 Sony A7 系列、Canon R 系列、Nikon Z 系列为代表的各类相机机身（微单/单反）', '📷'),
(2, '镜头', '可换镜头系统：定焦、变焦、广角、长焦、微距、移轴等', '🔍'),
(3, '配件', '存储卡、相机包、肩带、电池、清洁工具等通用配件', '🔌'),
(4, '三脚架', '稳定支撑系统，长曝光、夜景、微距、延时摄影必备', '🦶'),
(5, '滤镜系统', 'UV、CPL 偏振、ND 减光、GND 渐变、黑柔焦等光学滤镜', '🎨'),
(6, '闪光灯与灯光', '机顶闪、离机闪、常亮补光灯、柔光箱、反光板等', '💡')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  icon = VALUES(icon);


-- =====================================================================
-- 第 3 节 · 器材示例（10 条，覆盖 6 大分类，价格为 2026 参考价人民币）
-- 表名：gear
-- =====================================================================

-- ------- 微单 3 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(1, 'Sony', 'Alpha A7 IV 全画幅微单', 1, 15999.00,
 '2022 年度水桶机王，3300万像素全画幅背照 + 5 轴防抖 + 4K60p 10bit 4:2:2 视频；AI 实时人眼/鸟眼追焦；兼顾拍照和视频双修。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%20A7%20IV%20fullframe%20mirrorless%20camera%20product%20photo%20white%20bg&image_size=square',
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
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Canon%20EOS%20R6%20fullframe%20mirrorless%20camera%20product%20photo&image_size=square',
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
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Nikon%20Z%208%20pro%20fullframe%20mirrorless%20camera%20product%20white%20bg&image_size=square',
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
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%20ZV-E10%20vlog%20APS-C%20camera%20product%20photo%20white&image_size=square', NULL, NULL, NULL, 'DDSSM 超声波马达',
 '镜片:13组17片(2片XA+3片非球+2片ED) | 最近对焦:0.21m | 最大放大:0.32x | 滤镜:82mm | 重量:695g',
 5, 5620,
 '全画幅标准变焦之王！画质与重量的黄金平衡点；人像/风景/街拍/婚礼挂机一支搞定，2026 年依旧是大三元标变首选。', 98, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(5, 'Sony', 'FE 85mm F1.4 GM II 人像定焦', 3, 12999.00,
 '人像镜皇二代；11 片圆形光圈叶营造奶油 Bokeh；XA 超级非球镜片全开边角锐利；对焦呼吸效应极低，视频对焦平滑。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%2085mm%20f1.4%20GM%20portrait%20prime%20lens%20product%20white%20bg&image_size=square', NULL, NULL, NULL, 'XD线性马达',
 '镜片:8组11片(1片XA+3片ED) | 最近对焦:0.55m | 散景:11片光圈叶 | 重量:778g | 滤镜:77mm',
 5, 3120,
 '人像摄影师的终极武器；f/1.4 全开锐度惊人，焦外虚化圆润不杂乱；配合 A7R V 6100 万像素时，人像发丝根根分明如数头发。', 97, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(6, 'Canon', 'RF 50mm F1.2 L USM 标准定焦', 3, 15499.00,
 '佳能 RF 卡口旗舰 50/1.2；2 片非球 + 1 片 UD 超低色散；全开边缘素质超高，锐度和虚化兼得；人像/纪实双修。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Canon%2050mm%20f1.8%20STM%20nifty%20fifty%20lens%20product%20photo&image_size=square', NULL, NULL, NULL, '环形USM',
 '镜片:9组15片 | 最近对焦:0.4m | 光圈叶片:11片圆形 | 滤镜:82mm | 重量:950g',
 5, 1520,
 '50mm 人文焦段之王；搭配 R5 R6 人像直出肤色非常讨喜；是「拍女朋友永远不被骂」这一应用场景的最佳镜头。', 95, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(7, 'Tamron', 'A056 70-180mm F2.8 Di III VXD 长焦变焦', 3, 8380.00,
 '腾龙为索尼 E 卡口量身打造的长焦大三元；仅 810g + 149mm 长度，轻便到完全颠覆 70-200 f/2.8 的沉重印象。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Nikon%2024-70mm%20f2.8%20S%20zoom%20lens%20product%20photo%20white%20bg&image_size=square', NULL, NULL, NULL, 'VXD 线性马达',
 '镜片:12组19片 | 最近对焦:0.85m@70端 / 1.0m@180端 | 放大倍率:1:4.6 | 滤镜:67mm | 重量:810g',
 5, 2012,
 '穷玩党福音，索尼大三元长焦的最佳「不破产版」；画质接近原厂 GM 1.05x 倍数，价格只有一半；婚礼摄影师、单兵作业首选。', 93, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 三脚架 1 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(8, 'Gitzo', 'GT1545T 旅行者 1 号碳纤维三脚架', 4, 6800.00,
 '捷信百年顶级碳纤维；1.5 公斤自重承重 10 公斤；折叠仅 42cm 可登机；6X 碳纤维管吸振性能极强，长曝 30s 画面不抖。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Gitzo%20GT1545T%20carbon%20fiber%20tripod%2C%20pro%20photo%20tripod%20product%20white%20bg&image_size=square', NULL, NULL, NULL, NULL,
 '节数:4节 | 材质:6X碳纤维 | 承重:10kg | 自重:1.49kg | 最大高度:167cm | 折叠长度:42cm',
 5, 720,
 '三脚架里的劳斯莱斯，一次投资 15 年不换；长曝星轨、风光旅拍神器；买它的唯一理由就是"轻+稳+100%不后悔"。', 98, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 滤镜 1 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(9, 'NiSi', 'V7 滤镜支架套装（CPL + GND8 + ND1000）', 5, 1280.00,
 '耐司方形支架旗舰 V7；自带 True Color CPL 不偏色；赠送软渐变 GND8 与 ND1000 减光镜；风光摄影师入门三件套。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=NiSi%20V7%20filter%20kit%20CPL%20ND1000%20GND8%20square%20filters%20photo%20product&image_size=square', NULL, NULL, NULL, NULL,
 '口径:100mm 方形 | 含:V7支架+TrueColorCPL+ND1000(10档)+GND8(3档软渐变) | 接圈:67/72/77/82mm',
 5, 980,
 '风光摄影「白天能把水流拍成丝缎、流云拍成奶油」的最低成本一步到位方案；支架+CPL+ND+GND 全齐，直接出门拍风景。', 94, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 闪光灯 1 款 -------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(10, 'Godox', 'V1 Pro 圆头机顶闪光灯', 6, 1880.00,
 '神牛 2025 年圆头灯旗舰；GN65 大功率 + 2600mAh 锂电 480 次全光回闪；支持 TTL 自动跳闪、HSS 高速同步 1/8000s；人像婚礼跟拍利器。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Godox%20V1%20Pro%20round%20head%20speedlite%20flash%20product%20white%20bg&image_size=square', NULL, NULL, NULL, 'TTL/M/Multi',
 '灯头:圆形 | 功率:GN65 | 供电:VB26A 2600mAh | 回电:0.01~1.2s | 触发:2.4G X系统 | HSS:1/8000s',
 5, 4200,
 '机顶闪界千元档没有对手；圆灯头光质接近 Profoto A10 但价格只有 1/4；TTL 精准 + 光质软 + 电池给力，婚礼、商拍、街拍都胜任。', 92, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ================= 新增器材（id=11~36）=================

-- ------- 机身 · 数码微单 7 款（id=11~17）-------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(11, 'Sony', 'Alpha A7C II 全画幅便携微单', 1, 13499.00,
 '2023 年发布的小型化全画幅；3300 万像素 A7 IV 同款传感器，A7C 机身塞进 5 轴防抖 + 4K60p；最轻全幅之一，旅行街拍随身首选。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%20A7C%20II%20compact%20fullframe%20mirrorless%20camera%20silver%20product&image_size=square',
 '3300万像素全画幅', '10张/秒机械快门', '4K 60p 7K超采样',
 'AI人眼/鸟眼实时追焦',
 '传感器:IMX410 | 防抖:5轴5档 | 存储:双SD卡 | 重量:514g | 电池:NP-FZ100 | 翻折屏',
 5, 1680,
 '全画幅性能塞进 APS-C 体积，这是目前能买到的最轻的「认真拍照级」全幅机；旅行党、扫街党、Vloger 直接盲买不后悔。', 94, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(12, 'Sony', 'Alpha A7R V 高像素全画幅微单', 1, 21999.00,
 '6100 万像素 BSI-CMOS 天花板；8 档 5 轴防抖 + AI 主体识别（人/鸟/猫/车/昆虫/火车）；适合商拍风光婚纱影楼。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%20A7R%20V%20high%20resolution%20fullframe%20camera%20product%20white%20bg&image_size=square',
 '6100万像素全画幅背照', '10张/秒机械', '8K 24p / 4K 60p 6K超采',
 'AI主体识别 7种类型',
 '传感器:IMX661 | 处理器:BIONZ XR | 防抖:5轴8档 | 存储:双CFexpress/SD | 重量:723g',
 5, 1120,
 '如果你不知道买什么高像素机身，买 A7R V 就对了；商业人像、风光、产品数毛党的终极选择，后期裁切空间巨大。', 97, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(13, 'Sony', 'Alpha ZV-E1 全画幅 Vlog 微单', 1, 15999.00,
 '为内容创作者量身定做的全幅 Vlog 机；A7S III 同款 1200 万像素视频传感器 + AI 跟焦；4K 120p 全幅无裁切，电影级画质。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%20ZV-E1%20fullframe%20vlog%20camera%20creator%20product%20white%20background&image_size=square',
 '1200万像素全画幅', '10张/秒电子', '4K 120p 全幅无裁切 / S-Cinetone',
 'AI 跟焦（人/猫/犬/鸟）',
 '传感器:IMX540 | 防抖:5轴5档 + 主动防抖 | 存储:SD双槽 | 重量:483g | 专用 Vlog 按键',
 5, 980,
 '内容创作者和视频博主的终极武器；ZV-E1 是目前市面上视频画质最好、对焦最稳、机身最轻的全幅 Vlog 相机，没有之一。', 93, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(14, 'Canon', 'EOS R5 高像素全画幅旗舰', 1, 25999.00,
 '佳能 2020 发布的高像素旗舰；4500 万像素全幅 + 8K 30p RAW 视频 + 双核 CMOS AF II；商拍风光摄影的老牌旗舰。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Canon%20EOS%20R5%20high%20pixel%20mirrorless%20camera%20product%20white%20bg&image_size=square',
 '4500万像素全画幅', '12张/秒机械', '8K 30p / 4K 120p',
 '双核CMOS AF II 覆盖100%',
 '处理器:DIGIC X | 防抖:机身RF镜头协同8档 | 存储:双CFexpress | 重量:738g',
 5, 2210,
 '老牌商业工作室的主力机；8K RAW 视频是视频工作室的专业生产力；佳能直出肤色的天花板之一。', 95, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(15, 'Nikon', 'Z 8 专业级全画幅微单', 1, 24999.00,
 '尼康 Z 9 的简化版机身；4571 万像素堆叠式 + 8K 60p 12bit N-RAW + 120 张/秒电子快门连拍；无机械快门镜间设计。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Nikon%20Z%208%20pro%20fullframe%20mirrorless%20camera%20product%20white%20background&image_size=square',
 '4571万像素堆叠式全幅', '120张/秒电子', '8K 60p 12bit N-RAW / ProRes RAW',
 '3D追焦 + 人/犬/猫/鸟/车/飞机/摩托',
 '处理器:EXPEED 7 | 防抖:5轴+镜头协同7档 | 存储:双CFexpress | 重量:910g',
 5, 1480,
 '尼康 2024 年的真香机；Z 9 一样的传感器和 8K 视频，少了竖拍手柄轻了 500g，价格便宜一半，是专业摄影师的性价比首选。', 96, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(16, 'Fujifilm', 'X-T5 APS-C 复古微单', 1, 12790.00,
 '富士 4000 万像素 APS-C X-Trans 5 HR 传感器；19 种胶片模拟（NC/Velvia/Astia 等）；三转盘机械复古外形；直出党最爱。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Fujifilm%20X-T5%20retro%20vintage%20APS-C%20mirrorless%20camera%20silver%20product&image_size=square',
 '4020万像素 APS-C X-Trans', '15张/秒机械', '6.2K 30p / 4K 60p 10bit',
 '相位+对比混合 AF 74%区域',
 '传感器:X-Trans 5 HR | 防抖:5轴7档 | 存储:双SD UHS-II | 重量:557g | 机械转盘',
 5, 1850,
 '文艺复古、直出王者；富士胶片模拟是真的"拍出来就好看"，不用后期；街拍/人像/旅行首选，妹子拿在手里就是出片保证。', 94, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(17, 'Sony', 'Alpha A6700 APS-C 旗舰微单', 1, 10999.00,
 '2600 万像素 APS-C Exmor R 背照传感器；A7 IV 同款 BIONZ XR 处理器；AI 主体追焦 + 4K 120p 视频；APS-C 卷王。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%20A6700%20APS-C%20flagship%20mirrorless%20camera%20product%20white%20bg&image_size=square',
 '2600万像素 APS-C 背照', '11张/秒机械', '4K 120p 6K超采样 10bit 4:2:2',
 'AI主体识别 7种类型',
 '传感器:APS-C Exmor R | 防抖:5轴5档 | 存储:双SD UHS-II | 重量:493g | S-Cinetone',
 5, 2240,
 'APS-C 综合性能天花板；画质、对焦、视频、防抖、AI 追焦全部拉满；是入门用户升级 APS-C 或副机的首选。', 92, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 镜头 9 款（id=18~26）-------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(18, 'Sony', 'FE 16-35mm F2.8 GM II 广角大三元', 3, 16999.00,
 '2023 年新一代广角大三元；547g 比一代轻 300g；2 片 XA 超级非球 + 2 片 ED；全开边角锐利无畸变；风光/建筑/室内/Vlog。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%2016-35mm%20F2.8%20GM%20II%20wide%20angle%20zoom%20lens%20product%20photo&image_size=square', NULL, NULL, NULL, 'XD 双线性马达 + DDSSM',
 '镜片:12组16片(2XA+2非球+2ED) | 最近对焦:0.22m | 放大:0.30x | 滤镜:82mm | 重量:547g',
 5, 1950,
 '广角大三元里的王；547g 轻到可以长期挂机；风光、婚礼、商拍室内一支搞定；配合 A7R V 6100 万像素可以当半个移轴用。', 97, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(19, 'Sony', 'FE 70-200mm F2.8 GM OSS II 长焦大三元', 3, 21999.00,
 '索尼二代长焦炮皇；1045g 比一代轻 29%；4 片 XA 超级非球全开画质爆炸；最近对焦 0.4m 有 0.3x 微距，人像商拍双杀。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%2070-200mm%20F2.8%20GM%20II%20white%20telephoto%20lens%20product%20white%20bg&image_size=square', NULL, NULL, NULL, 'XD 四线性马达',
 '镜片:15组21片(4XA+1非球+2ED) | 最近对焦:0.4m | 放大:0.30x | 滤镜:77mm | 重量:1045g',
 5, 1620,
 '人像、体育、野生动物摄影师的白月光；f/2.8 200mm 端全开锐利到变态；Bokeh 虚化丝滑如德芙，是目前最好的 70-200 没有之一。', 99, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(20, 'Sigma', '24-70mm F2.8 DG DN Art 大三元标变', 3, 7299.00,
 '适马为 E/L 卡口打造的 Art 级别标变；全开锐度媲美原厂；9 片圆形光圈 + 大灯泡前组；性价比党的挂机首选。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sigma%2024-70mm%20F2.8%20DG%20DN%20Art%20standard%20zoom%20lens%20product%20white%20bg&image_size=square', NULL, NULL, NULL, 'HSM 超声波马达',
 '镜片:15组19片(1SLD+6非球+2FLD) | 最近对焦:0.18m | 放大:1:2.9 | 滤镜:82mm | 重量:830g',
 5, 3150,
 '一半的价格，95% 的原厂画质；预算不够 GM II 的话直接上这支 24-70 Art；婚礼摄影师用它拍出来的图客户分不出和 GM 的差别。', 91, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(21, 'Tamron', '28-75mm F2.8 Di III VXD G2 标变', 3, 5980.00,
 '腾龙二代 28-75；540g 轻到没朋友；VXD 马达超快超静音；最近对焦 0.18m 半微距；学生党/婚礼助理的最佳挂机标变。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Tamron%2028-75mm%20F2.8%20G2%20compact%20zoom%20lens%20product%20white%20background&image_size=square', NULL, NULL, NULL, 'VXD 线性马达',
 '镜片:12组16片(2LD+3非球+1复合非球) | 最近对焦:0.18m | 放大:1:2.7 | 滤镜:67mm | 重量:540g',
 5, 4280,
 '全画幅能买到的最轻的 F2.8 标变之一；67mm 通用滤镜口径省钱；学生党和新手第一支大三元，买它不破产。', 90, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(22, 'Canon', 'RF 70-200mm F2.8 L IS USM Z 长焦大三元', 3, 20999.00,
 '佳能 RF 卡口长焦炮皇二代；1420g 外变焦；2 片萤石级 UD + 4 片 UD；IS 8 级防抖；人像体育拍鸟通吃。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Canon%20RF%2070-200mm%20F2.8%20L%20white%20telephoto%20lens%20product%20photo&image_size=square', NULL, NULL, NULL, 'NANO USM + 环形 USM',
 '镜片:17组23片(2超级UD+4UD) | 最近对焦:0.6m@70端 / 0.85m@200端 | 放大:0.23x | 滤镜:77mm | 重量:1420g',
 5, 1880,
 '佳能用户的信仰；RF 系统的长焦画质天花板；配合 R5/R6 拍人像和活动，背景虚化刀锐奶化，客户看了都满意。', 95, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(23, 'Sony', 'FE 50mm F1.2 GM 标准定焦', 3, 19999.00,
 '索尼标头天花板；3 片 XA 超级非球全开锐度惊人；11 片圆形光圈营造奶油散景；人像/纪实/人文一镜三用。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%2050mm%20F1.2%20GM%20large%20aperture%20prime%20lens%20product%20white%20bg&image_size=square', NULL, NULL, NULL, 'XD 四线性马达',
 '镜片:10组14片(3XA+1非球+2ED) | 最近对焦:0.4m | 光圈叶片:11片圆形 | 重量:778g | 滤镜:72mm',
 5, 820,
 '50mm 焦段的终极答案；配合 A7R V 使用，全开锐度令人发指；是标准焦段能买到的最好的镜头，没有之一。', 97, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(24, 'Sigma', '35mm F1.4 DG DN Art 人文定焦', 3, 4999.00,
 '适马 35mm F1.4 二代微单版；轻到 640g；HSM 马达 + 全金属镜身；全开锐度硬刚原厂；街拍纪实人文首选焦段。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sigma%2035mm%20F1.4%20DG%20DN%20Art%20prime%20lens%20product%20white%20background&image_size=square', NULL, NULL, NULL, 'HSM 超声波马达',
 '镜片:11组15片(1SLD+2非球+1FLD) | 最近对焦:0.3m | 光圈叶片:11片圆形 | 重量:640g | 滤镜:67mm',
 5, 2640,
 '布列松时代的人文焦段；35mm f/1.4 拍人、拍环境人像、拍街景全能；预算不够原厂 35GM 的话直接适马这支没毛病。', 92, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(25, 'Sony', 'FE 90mm F2.8 Macro G OSS 微距镜头', 3, 7299.00,
 '原厂 1:1 真微距；150mm 等效工作距离 + OSS 光学防抖 + DDSSM 马达；拍珠宝/昆虫/美食/人像细节特写神器。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Sony%2090mm%20F2.8%20Macro%20G%20OSS%201%3A1%20macro%20lens%20product%20white%20bg&image_size=square', NULL, NULL, NULL, 'DDSSM 直驱超声波马达',
 '镜片:11组15片(1非球+1ED) | 放大:1:1真微距 | 最近对焦:0.28m | 防抖:OSS | 重量:602g | 滤镜:62mm',
 5, 1350,
 '拍美食、拍珠宝、拍花花草草昆虫的必备；f/2.8 虚化 + 1:1 微距，拍菜和拍女朋友锁骨/指甲/发丝这种细节出片率 100%。', 93, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(26, 'Samyang', '14mm F2.8 AF 超广角定焦', 3, 3299.00,
 '性价比超广角星空神头；自动对焦 + F2.8 大光圈；彗差极小，星空银河边缘星点不脱线；风景/星空/建筑利器。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Samyang%2014mm%20F2.8%20AF%20ultra%20wide%20angle%20lens%20astro%20photo%20product&image_size=square', NULL, NULL, NULL, 'STM 步进马达',
 '镜片:10组14片(3非球+2低色散) | 最近对焦:0.28m | 视角:115.7°对角线 | 滤镜:灯泡不能拧 | 重量:565g',
 4, 1120,
 '星空党的入门首选；14mm F2.8 + 自动对焦 + 彗差控制好的自动头，这个价位没有第二个选择；A7 IV + 这支头，银河随便拍。', 89, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 三脚架 / 稳定器 4 款（id=27~30）-------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(27, 'Manfrotto', 'Element MII 铝合金三脚架', 4, 1299.00,
 '曼富图入门铝合金三脚架；自重 1.5kg 承重 8kg；360° 球形云台；中轴倒置拍微距；学生党和新手的第一支脚架。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Manfrotto%20Element%20MII%20aluminum%20tripod%20ball%20head%20product%20white&image_size=square', NULL, NULL, NULL, NULL,
 '节数:4节 | 材质:铝合金 | 承重:8kg | 自重:1.53kg | 最大高度:163cm | 折叠:43cm | 云台:球台',
 4, 2560,
 '入门三脚架的及格线；曼富图 Element 系列比杂牌稳定太多，用了就知道 1000 元以下的脚架是有多差；新手第一支脚架买它。', 86, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(28, 'Peak Design', 'Travel Tripod 碳纤维旅行脚架', 4, 4780.00,
 '美国 Peak Design 网红三脚架；折叠直径和矿泉水瓶一样；1.27kg 自重承重 9kg；快装板系统兼容 Arca-Swiss；旅拍神器。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Peak%20Design%20Travel%20Tripod%20carbon%20fiber%20compact%20photo%20tripod&image_size=square', NULL, NULL, NULL, NULL,
 '节数:5节 | 材质:碳纤维 | 承重:9.07kg | 自重:1.27kg | 最大高度:150cm | 折叠长度:31cm',
 5, 1890,
 '旅拍党和背包装不下大架子的风光摄影师；折叠长度只有 31cm，随便塞背包侧袋；Peak Design 工业设计的巅峰之作。', 91, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(29, 'DJI', 'RS 3 Mini 手持云台稳定器', 4, 1999.00,
 '大疆 2023 发布的微单稳定器；自重 795g 承重 2kg；原生竖拍 + 蓝牙快门联动 A7 系列；视频博主的单兵作业神器。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=DJI%20RS%203%20Mini%20gimbal%20stabilizer%20mirrorless%20camera%20product&image_size=square', NULL, NULL, NULL, NULL,
 '自重:795g | 承重:2kg(全画幅+24-70) | 续航:10小时 | 轴数:3轴 | 蓝牙快门:支持Sony/Canon/Fuji',
 5, 3240,
 '微单视频博主的第一件装备；A7C II + 28-75 + RS 3 Mini，这一套不到 1.5kg 直接出门拍 Vlog；盗梦空间、FPV 模式全都有。', 90, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(30, 'Benro', 'GD3WH 齿轮三维云台', 4, 899.00,
 '百诺建筑风光慢门专用齿轮云台球台；三向独立齿轮微调；精度 0.1mm 级；拍摄接片、建筑透视校正时 1 毫米不差。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Benro%20GD3WH%203-way%20geared%20tripod%20head%20panorama%20product%20white&image_size=square', NULL, NULL, NULL, NULL,
 '调节方式:三轴齿轮微调 | 承重:6kg | 自重:0.72kg | 快装:PU快装板兼容Arca-Swiss | 底座:360°全景旋转',
 4, 540,
 '拍建筑、全景接片、风光慢门的专业配件；齿轮微调比球形云台精准100倍，横平竖直构图一次到位，强迫症摄影师的最爱。', 87, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 闪光灯 / 常亮灯 4 款（id=31~34）-------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(31, 'Godox', 'AD200Pro II 双灯头口袋外拍灯', 6, 2480.00,
 '神牛口袋闪光灯王；200Ws 功率 + 直头/圆头双灯头；2900mAh 锂电 500 次全光；HSS 1/8000s + TTL；婚纱商拍外景首选。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Godox%20AD200Pro%20II%20pocket%20flash%20dual%20head%20strobe%20light%20product&image_size=square', NULL, NULL, NULL, 'TTL / M / MULTI',
 '功率:200Ws | 灯头:双灯头(直头/圆头可换) | 电池:2900mAh 锂电池 | 回电:0.01~1.8s | HSS:1/8000s | 触发:2.4G X',
 5, 2680,
 '外拍灯里的瑞士军刀；体积和可乐一样大；婚纱外景、人像户外、商务快拍一支搞定；配合 XPro II 引闪器离机闪玩法丰富。', 94, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(32, 'Godox', 'ML60 II Bi 双色温常亮补光灯', 6, 1680.00,
 '神牛二代便携 LED 常亮灯；70W 功率 + 2800K-6500K 双色温；CRI96 / TLCI97 高显色；直播、短视频、静物布光一把好手。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Godox%20ML60%20II%20Bi-color%20LED%20video%20light%20photo%20product%20white&image_size=square', NULL, NULL, NULL, NULL,
 '功率:70W | 色温:2800K~6500K可调 | CRI:96+ | TLCI:97+ | 供电:NP-F卡口电池/DC | 重量:700g | 静音风扇',
 5, 1420,
 '内容创作者的核心生产力工具；双色温可以模拟夕阳/冷色氛围；CRI96 拍产品拍美食不偏色；一个灯的钱解决直播+短视频+静物三件事。', 89, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(33, 'Yongnuo', 'YN650EX-RF 机顶闪光灯', 6, 599.00,
 '永诺入门闪光灯；GN60 功率 + TTL / HSS 1/8000s；支持 Canon RF 原生无线引闪；预算有限又想体验离机闪的首选。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=Yongnuo%20YN650EX-RF%20speedlite%20camera%20flash%20product%20white%20bg&image_size=square', NULL, NULL, NULL, 'TTL / M',
 '功率:GN60 | 变焦:20-200mm | TTL:支持 | HSS:1/8000s | 回电:0.1~3s | 供电:4节AA | 闪光次数:230次',
 4, 3890,
 '新手闪光灯的敲门砖；不到 600 块就能玩 TTL 和高速同步；先买它学习离机闪、跳闪、布光基础，等技术升级了再上神牛 V1 / AD200。', 82, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(34, 'SmallRig', 'RA-V1 五合一反光板 110cm', 6, 99.00,
 '斯莫格 110cm 圆形五合一反光板；金/银/白/黑/半透明柔光五种功能；折叠后直径仅 35cm；最便携最便宜的补光方案。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=SmallRig%205-in-1%20photography%20reflector%20disc%20110cm%20light%20modifier&image_size=square', NULL, NULL, NULL, NULL,
 '展开:110cm 圆形 | 收纳:35cm 便携袋 | 材质:银/金/白/黑半透五合一 | 重量:500g',
 5, 5670,
 '性价比最高的补光配件，没有之一；100 块钱的东西能解决 80% 的自然光补光问题；人像、美妆、美食、静物、户外通用。', 88, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ------- 滤镜系统 2 款（id=35~36）-------
INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(35, 'K&F Concept', '磁吸滤镜套装 67mm（CPL + ND8 + ND16）', 5, 399.00,
 'K&F 性价比磁吸滤镜套装；德国肖特玻璃 + 128 层镀膜；磁吸拆装只需 1 秒；新人入门风光摄影的一站式方案。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=K%26F%20Concept%2067mm%20magnetic%20filter%20kit%20CPL%20ND8%20ND16%20UV%20photo&image_size=square', NULL, NULL, NULL, NULL,
 '口径:67mm | 材质:德国肖特B270玻璃 | 套装:UV+CPL+ND8(3档)+ND16(4档)+磁吸盖 | 镀膜:128层双面 | 偏色:True Color低偏色',
 4, 2150,
 '磁吸滤镜真香警告；拆装快到爆炸，不用每次拧螺纹；风光新手买它直接出去拍水流、拍云、拍瀑布拍车流，CPL蓝天蓝得发黑。', 85, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT IGNORE INTO gear
  (id, brand, model, category_id, price, description, image_url, pixel, continuous_shooting, video, focus, specifications, rating, review_count, ai_review, ai_score, created_at, updated_at) VALUES
(36, 'NiSi', '黑柔焦滤镜 1/4 77mm 圆形', 5, 680.00,
 '耐司人像氛围感神器；黑柔 1/4 档位可软化皮肤瑕疵、让高对比度光线变柔；美妆博主、糖水片、复古电影感必入。',
 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=NiSi%20black%20pro%20mist%20diffusion%201%2F4%2077mm%20filter%20portrait%20cinematic&image_size=square', NULL, NULL, NULL, NULL,
 '口径:77mm | 档位:1/4(中度柔化) | 材质:日本东旭玻璃 | 镀膜:28层纳米 | 安装:螺纹拧入 | 透光率:~80%',
 5, 780,
 '人像氛围感神器；不用磨皮直接出片；美妆博主拍脸不反光，糖水片女朋友皮肤自带奶油光，一支小滤镜抵过后期半小时。', 90, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();


-- =====================================================================
-- 第 4 节 · 用户表不在这里手动插！
-- （密码是 BCrypt 哈希，写在 SQL 里是死的不优雅。
--  两个默认账号 demo/123456、tester_001/123456
--  由 SeedDataRunner.java 在用户表空的时候实时 passwordEncoder.encode 写入。）
-- =====================================================================
