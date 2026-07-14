package com.sheyingtong.config;

import com.sheyingtong.entity.*;
import com.sheyingtong.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final PhotographyTermRepository termRepository;
    private final GearCategoryRepository categoryRepository;
    private final GearRepository gearRepository;
    private final PhotoAnalysisRepository photoAnalysisRepository;
    
    @Override
    public void run(String... args) {
        if (termRepository.count() == 0) {
            initTerms();
        }
        
        if (categoryRepository.count() == 0) {
            initCategories();
        }
        
        if (gearRepository.count() == 0) {
            initGear();
        }
        
        if (photoAnalysisRepository.count() == 0) {
            initPhotoAnalysis();
        }
    }
    
    private void initTerms() {
        PhotographyTerm[] terms = {
            PhotographyTerm.builder()
                .term("光圈")
                .category("曝光")
                .definition("光圈是镜头内部可以调节大小的孔径，控制进入相机的光线量。")
                .explanation("光圈值用f/表示，如f/1.4、f/2.8、f/5.6等。数值越小，光圈越大，进光量越多，景深越浅；数值越大，光圈越小，进光量越少，景深越深。")
                .examples("拍摄人像时常用大光圈（如f/1.8）来虚化背景；拍摄风光时常用小光圈（如f/11）来获得大景深。")
                .relatedTerms("景深,曝光,快门")
                .build(),
            PhotographyTerm.builder()
                .term("快门")
                .category("曝光")
                .definition("快门是相机中控制感光元件曝光时间的装置。")
                .explanation("快门速度表示感光元件暴露在光线下的时间长度，单位是秒。常见的快门速度有1/1000s、1/250s、1/60s、1/15s、1s等。")
                .examples("高速快门（1/500s以上）可以冻结运动物体；低速快门（1/30s以下）可以拍摄动态模糊效果，如流水、车流。")
                .relatedTerms("曝光,光圈,ISO")
                .build(),
            PhotographyTerm.builder()
                .term("ISO")
                .category("曝光")
                .definition("ISO是衡量相机感光元件对光线敏感程度的指标。")
                .explanation("ISO数值越高，感光元件对光线越敏感，可以在光线较暗的环境下拍摄，但同时会产生更多噪点。常见的ISO值有100、200、400、800、1600等。")
                .examples("在室内或低光环境下拍摄时，可以提高ISO来获得足够的曝光；在光线充足的情况下，应使用低ISO以保证画面质量。")
                .relatedTerms("曝光,噪点,光圈")
                .build(),
            PhotographyTerm.builder()
                .term("景深")
                .category("构图")
                .definition("景深是指照片中清晰的区域范围。")
                .explanation("景深由光圈、焦距和拍摄距离三个因素决定。光圈越大、焦距越长、拍摄距离越近，景深越浅；反之，景深越深。")
                .examples("浅景深常用于人像摄影，可以突出主体，虚化背景；深景深常用于风光摄影，可以让整个画面都保持清晰。")
                .relatedTerms("光圈,焦距,构图")
                .build(),
            PhotographyTerm.builder()
                .term("构图")
                .category("构图")
                .definition("构图是指在画面中安排和组织视觉元素的方式。")
                .explanation("好的构图可以引导观众的视线，突出主体，传达情感。常见的构图法则包括三分法、引导线、对称、框架式构图等。")
                .examples("使用三分法将主体放在画面的三分线交点上；利用道路、河流等作为引导线引导视线；寻找对称的景物进行对称构图。")
                .relatedTerms("三分法,引导线,对称")
                .build(),
            PhotographyTerm.builder()
                .term("焦距")
                .category("镜头")
                .definition("焦距是指镜头中心到成像平面的距离，决定了视角和放大倍数。")
                .explanation("焦距越短，视角越广，能容纳更多场景；焦距越长，视角越窄，放大倍数越大。常见的焦距有24mm、35mm、50mm、85mm、135mm等。")
                .examples("广角镜头（24mm以下）适合拍摄风光和建筑；标准镜头（50mm左右）接近人眼视角，适合日常拍摄；长焦镜头（85mm以上）适合拍摄人像和野生动物。")
                .relatedTerms("镜头,视角,景深")
                .build(),
            PhotographyTerm.builder()
                .term("白平衡")
                .category("色彩")
                .definition("白平衡是调整相机色彩还原的功能，确保在不同光源下白色看起来都是白色。")
                .explanation("不同光源有不同的色温，如日光偏蓝、灯光偏黄。白平衡功能可以补偿这些色彩偏差，使照片色彩更加准确。")
                .examples("在室内白炽灯下拍摄时，若不调整白平衡，照片会偏黄；选择\"白炽灯\"白平衡模式可以纠正这种偏色。")
                .relatedTerms("色温,色彩,曝光")
                .build(),
            PhotographyTerm.builder()
                .term("RAW")
                .category("格式")
                .definition("RAW是一种无损的图像格式，记录了相机感光元件捕获的原始数据。")
                .explanation("RAW格式保留了更多的图像信息和动态范围，后期处理时可以进行更大幅度的调整而不会损失画质。")
                .examples("专业摄影师通常使用RAW格式拍摄，以便在后期进行精细的曝光、白平衡和色彩调整。")
                .relatedTerms("JPEG,后期,格式")
                .build(),
            PhotographyTerm.builder()
                .term("三分法")
                .category("构图")
                .definition("三分法是将画面分成三个等份的水平线和垂直线，将主体放在交点上的构图方法。")
                .explanation("三分法是最常用的构图法则之一，可以使画面更加平衡和美观。大多数相机都有网格线功能来帮助使用三分法。")
                .examples("拍摄人像时，将眼睛放在上三分线交点；拍摄风光时，将地平线放在下三分线。")
                .relatedTerms("构图,网格线,黄金分割")
                .build(),
            PhotographyTerm.builder()
                .term("引导线")
                .category("构图")
                .definition("引导线是指画面中引导观众视线的线条元素。")
                .explanation("引导线可以是道路、河流、栏杆、阴影等，它们可以引导观众的视线指向主体，增加画面的深度感和层次感。")
                .examples("利用道路或河流作为引导线，引导视线向画面深处延伸；使用对角线构图创造动感。")
                .relatedTerms("构图,透视,三分法")
                .build(),
            PhotographyTerm.builder()
                .term("对称")
                .category("构图")
                .definition("对称构图是指画面中的元素在中心线两侧呈对称分布。")
                .explanation("对称构图可以传达稳定、平衡、庄严的感觉，常用于建筑摄影和对称景物的拍摄。")
                .examples("拍摄对称的建筑、桥梁、倒影时，可以使用对称构图；利用水面倒影创造对称效果。")
                .relatedTerms("构图,平衡,建筑摄影")
                .build(),
            PhotographyTerm.builder()
                .term("噪点")
                .category("画质")
                .definition("噪点是指照片中出现的彩色或黑白斑点，是图像传感器在低光环境下产生的干扰。")
                .explanation("噪点会降低照片的清晰度和细节表现。高ISO设置、长曝光时间和低光环境都会增加噪点。")
                .examples("在ISO 3200以上拍摄时，照片会明显出现噪点；可以使用降噪软件在后期处理中减少噪点。")
                .relatedTerms("ISO,画质,后期")
                .build(),
            PhotographyTerm.builder()
                .term("动态范围")
                .category("画质")
                .definition("动态范围是指相机能够同时捕捉到的最亮和最暗区域的范围。")
                .explanation("动态范围越大，相机在高对比度场景中保留的细节越多。RAW格式比JPEG格式有更大的动态范围。")
                .examples("在逆光场景中，动态范围大的相机可以同时保留天空的高光细节和地面的暗部细节。")
                .relatedTerms("RAW,曝光,HDR")
                .build(),
            PhotographyTerm.builder()
                .term("HDR")
                .category("技术")
                .definition("HDR（高动态范围）是一种通过合成多张不同曝光照片来扩展动态范围的技术。")
                .explanation("HDR技术可以捕捉到比单张照片更广的亮度范围，特别适合高对比度场景的拍摄。")
                .examples("拍摄大光比的风光照片时，可以使用HDR模式或手动包围曝光，然后在后期合成HDR照片。")
                .relatedTerms("动态范围,包围曝光,后期")
                .build(),
            PhotographyTerm.builder()
                .term("包围曝光")
                .category("技术")
                .definition("包围曝光是指连续拍摄多张不同曝光值的照片，以确保至少有一张曝光正确。")
                .explanation("包围曝光通常包括正常曝光、欠曝光和过曝光三张照片，用于HDR合成或在不确定曝光时保证成功率。")
                .examples("在光线复杂的场景中，可以设置±1或±2档的包围曝光，确保获得满意的曝光效果。")
                .relatedTerms("HDR,曝光补偿,动态范围")
                .build(),
            PhotographyTerm.builder()
                .term("微距摄影")
                .category("题材")
                .definition("微距摄影是指拍摄物体细节的摄影方式，通常放大倍率在1:1或更大。")
                .explanation("微距摄影可以展现肉眼难以看到的细节之美，常用于拍摄花卉、昆虫、珠宝等微小物体。")
                .examples("使用微距镜头或微距模式拍摄花朵的纹理、昆虫的翅膀细节。")
                .relatedTerms("微距镜头,放大倍率,题材")
                .build(),
            PhotographyTerm.builder()
                .term("人像摄影")
                .category("题材")
                .definition("人像摄影是以人物为主体的摄影类型，注重表现人物的外貌、表情和气质。")
                .explanation("人像摄影讲究用光、构图和与模特的沟通，常见的有人像写真、商业人像、纪实人像等。")
                .examples("使用大光圈镜头拍摄人像，虚化背景突出人物；利用自然光创造柔和的光影效果。")
                .relatedTerms("光圈,用光,构图")
                .build(),
            PhotographyTerm.builder()
                .term("风光摄影")
                .category("题材")
                .definition("风光摄影是以自然景观为主体的摄影类型，注重表现大自然的壮美和宁静。")
                .explanation("风光摄影讲究光线时机、构图和后期处理，常见的有山川、湖泊、星空、城市风光等。")
                .examples("在日出或日落时分拍摄风光，利用黄金时刻的柔和光线；使用小光圈获得大景深。")
                .relatedTerms("构图,光线,景深")
                .build(),
            PhotographyTerm.builder()
                .term("长焦镜头")
                .category("镜头")
                .definition("长焦镜头是指焦距较长（通常85mm以上）的镜头，具有窄视角和高放大倍数。")
                .explanation("长焦镜头适合拍摄远处的物体，如野生动物、体育赛事、人像特写等。")
                .examples("使用200mm长焦镜头拍摄野生动物；使用85mm镜头拍摄人像特写。")
                .relatedTerms("镜头,焦距,视角")
                .build(),
            PhotographyTerm.builder()
                .term("广角镜头")
                .category("镜头")
                .definition("广角镜头是指焦距较短（通常35mm以下）的镜头，具有宽广的视角。")
                .explanation("广角镜头适合拍摄风光、建筑和室内场景，可以容纳更多的场景元素。")
                .examples("使用16-35mm广角镜头拍摄壮丽的风光；使用24mm镜头拍摄建筑的宏伟气势。")
                .relatedTerms("镜头,焦距,视角")
                .build(),
            PhotographyTerm.builder()
                .term("标准镜头")
                .category("镜头")
                .definition("标准镜头是指焦距接近人眼视角（约50mm）的镜头。")
                .explanation("标准镜头的视角和透视效果最接近人眼，适合日常拍摄、纪实摄影和人像摄影。")
                .examples("使用50mm标准镜头拍摄街头纪实照片；使用35mm镜头进行日常抓拍。")
                .relatedTerms("镜头,焦距,视角")
                .build(),
            PhotographyTerm.builder()
                .term("JPEG")
                .category("格式")
                .definition("JPEG是一种有损压缩的图像格式，是最常用的照片存储格式之一。")
                .explanation("JPEG格式通过压缩算法减小文件大小，但会损失部分图像细节。适合日常拍摄和分享。")
                .examples("在相机中选择JPEG格式进行日常拍摄；将照片导出为JPEG格式用于网络分享。")
                .relatedTerms("RAW,格式,压缩")
                .build(),
            PhotographyTerm.builder()
                .term("色温")
                .category("色彩")
                .definition("色温是表示光源颜色的指标，单位是开尔文（K）。")
                .explanation("不同光源有不同的色温，低色温偏暖（红色），高色温偏冷（蓝色）。常见的色温值：日出约2000K，日光约5500K，阴天约6500K。")
                .examples("在日落时分拍摄，光线色温较低，画面呈暖黄色；在阴天拍摄，光线色温较高，画面偏冷蓝色。")
                .relatedTerms("白平衡,色彩,光线")
                .build(),
            PhotographyTerm.builder()
                .term("曝光补偿")
                .category("曝光")
                .definition("曝光补偿是指在自动曝光模式下，手动调整曝光值的功能。")
                .explanation("曝光补偿用EV表示，+EV增加曝光，-EV减少曝光。常用于调整拍摄主体的亮度。")
                .examples("拍摄白色物体时，增加+1~+2EV曝光补偿避免欠曝；拍摄黑色物体时，减少-1~-2EV避免过曝。")
                .relatedTerms("曝光,自动曝光,测光")
                .build(),
            PhotographyTerm.builder()
                .term("测光")
                .category("曝光")
                .definition("测光是指相机测量场景亮度以确定正确曝光参数的过程。")
                .explanation("常见的测光模式有点测光、中央重点测光和评价测光。选择合适的测光模式可以获得准确的曝光。")
                .examples("拍摄人像时使用点测光对准人脸；拍摄风光时使用评价测光获得整体平衡的曝光。")
                .relatedTerms("曝光,曝光补偿,自动曝光")
                .build(),
            PhotographyTerm.builder()
                .term("景深预览")
                .category("功能")
                .definition("景深预览是指通过取景器或显示屏查看当前光圈设置下实际景深效果的功能。")
                .explanation("景深预览功能可以帮助摄影师判断当前光圈设置下的背景虚化效果是否符合预期。")
                .examples("在拍摄人像前，使用景深预览功能确认背景虚化程度；在拍摄风光时，确认前景和背景是否都清晰。")
                .relatedTerms("景深,光圈,预览")
                .build(),
            PhotographyTerm.builder()
                .term("反光板")
                .category("配件")
                .definition("反光板是摄影中用于反射光线的工具，用于补光和控制光影。")
                .explanation("反光板通常有金色、银色、白色等不同颜色，可以反射不同色调的光线，常用于人像摄影的补光。")
                .examples("在户外拍摄人像时，使用银色反光板反射阳光照亮人物面部阴影；使用金色反光板增加暖色调。")
                .relatedTerms("用光,补光,配件")
                .build()
        };
        termRepository.saveAll(java.util.Arrays.asList(terms));
    }
    
    private void initCategories() {
        GearCategory[] categories = {
            GearCategory.builder().name("相机").description("各类数码相机").icon("📷").build(),
            GearCategory.builder().name("镜头").description("各种焦距镜头").icon("🔍").build(),
            GearCategory.builder().name("配件").description("相机配件").icon("🔌").build()
        };
        categoryRepository.saveAll(java.util.Arrays.asList(categories));
    }
    
    private void initGear() {
        GearCategory cameraCat = categoryRepository.findByName("相机").orElse(null);
        GearCategory lensCat = categoryRepository.findByName("镜头").orElse(null);
        
        if (cameraCat != null) {
            gearRepository.save(Gear.builder()
                .brand("索尼")
                .model("Alpha 7 IV")
                .category(cameraCat)
                .price(BigDecimal.valueOf(16999))
                .description("索尼Alpha 7 IV是一款全画幅微单相机，搭载3300万像素背照式CMOS传感器，支持10fps连拍和4K 60p视频录制。")
                .imageUrl("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=sony%20alpha%207%20iv%20mirrorless%20camera&image_size=square")
                .pixel("3300万像素")
                .continuousShooting("10fps")
                .video("4K 60p")
                .focus("实时眼部对焦")
                .rating(48)
                .reviewCount(1256)
                .aiScore(92)
                .build());
            
            gearRepository.save(Gear.builder()
                .brand("佳能")
                .model("EOS R6")
                .category(cameraCat)
                .price(BigDecimal.valueOf(15999))
                .description("佳能EOS R6是一款高性能全画幅微单相机，适合专业摄影师和摄影爱好者。")
                .imageUrl("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=canon%20eos%20r6%20mirrorless%20camera&image_size=square")
                .pixel("2010万像素")
                .continuousShooting("20fps")
                .video("4K 60p")
                .focus("全像素双核对焦")
                .rating(47)
                .reviewCount(892)
                .aiScore(90)
                .build());
            
            gearRepository.save(Gear.builder()
                .brand("尼康")
                .model("Z 8")
                .category(cameraCat)
                .price(BigDecimal.valueOf(24999))
                .description("尼康Z 8是一款旗舰级全画幅微单相机，具有出色的画质和性能。")
                .imageUrl("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=nikon%20z8%20mirrorless%20camera&image_size=square")
                .pixel("4570万像素")
                .continuousShooting("30fps")
                .video("8K 60p")
                .focus("自动对焦系统")
                .rating(49)
                .reviewCount(567)
                .aiScore(95)
                .build());
            
            gearRepository.save(Gear.builder()
                .brand("索尼")
                .model("ZV-E10")
                .category(cameraCat)
                .price(BigDecimal.valueOf(5999))
                .description("索尼ZV-E10是一款适合vlog和入门用户的APS-C画幅微单相机。")
                .imageUrl("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=sony%20zv-e10%20camera&image_size=square")
                .pixel("2420万像素")
                .continuousShooting("11fps")
                .video("4K 30p")
                .focus("实时眼部对焦")
                .rating(46)
                .reviewCount(2341)
                .aiScore(85)
                .build());
        }
        
        if (lensCat != null) {
            gearRepository.save(Gear.builder()
                .brand("索尼")
                .model("85mm f/1.4 GM")
                .category(lensCat)
                .price(BigDecimal.valueOf(11999))
                .description("索尼85mm f/1.4 GM是一款专业级人像镜头，具有出色的虚化效果。")
                .imageUrl("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=sony%2085mm%20f1.4%20gm%20lens&image_size=square")
                .rating(49)
                .reviewCount(876)
                .aiScore(94)
                .build());
            
            gearRepository.save(Gear.builder()
                .brand("佳能")
                .model("50mm f/1.8 STM")
                .category(lensCat)
                .price(BigDecimal.valueOf(699))
                .description("佳能50mm f/1.8 STM是一款性价比极高的标准定焦镜头。")
                .imageUrl("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=canon%2050mm%20f1.8%20lens&image_size=square")
                .rating(45)
                .reviewCount(5432)
                .aiScore(82)
                .build());
            
            gearRepository.save(Gear.builder()
                .brand("尼康")
                .model("24-70mm f/2.8 S")
                .category(lensCat)
                .price(BigDecimal.valueOf(14999))
                .description("尼康24-70mm f/2.8 S是一款专业级标准变焦镜头。")
                .imageUrl("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=nikon%2024-70mm%20f2.8%20lens&image_size=square")
                .rating(48)
                .reviewCount(678)
                .aiScore(91)
                .build());
        }
    }
    
    private void initPhotoAnalysis() {
        photoAnalysisRepository.save(PhotoAnalysis.builder()
            .filePath("uploads/sample1.jpg")
            .fileName("日落风景.jpg")
            .description("海边日落风景")
            .score(85)
            .analysis("{\"composition\":{\"score\":88,\"comment\":\"构图运用了三分法，地平线位置恰当，视觉引导良好。\"},\"lighting\":{\"score\":82,\"comment\":\"光线柔和，色彩丰富，但高光区域略有过曝。\"},\"color\":{\"score\":86,\"comment\":\"色彩饱和度适中，白平衡准确，整体色调和谐。\"},\"focus\":{\"score\":84,\"comment\":\"主体对焦清晰，景深控制得当。\"}}")
            .suggestions("1.尝试使用渐变滤镜控制天空曝光\n2.适当增加对比度增强画面层次感\n3.可以尝试拍摄RAW格式方便后期调整")
            .build());
        
        photoAnalysisRepository.save(PhotoAnalysis.builder()
            .filePath("uploads/sample2.jpg")
            .fileName("人像写真.jpg")
            .description("室内人像拍摄")
            .score(78)
            .analysis("{\"composition\":{\"score\":75,\"comment\":\"人物位置略偏，可以尝试将人物放在右侧三分线。\"},\"lighting\":{\"score\":72,\"comment\":\"光线分布不均匀，面部阴影较重。\"},\"color\":{\"score\":80,\"comment\":\"肤色还原较好，背景色彩略显杂乱。\"},\"focus\":{\"score\":85,\"comment\":\"眼睛对焦清晰，睫毛细节可见。\"}}")
            .suggestions("1.使用反光板补光减少面部阴影\n2.调整人物位置到三分线交点\n3.简化背景元素")
            .build());
        
        photoAnalysisRepository.save(PhotoAnalysis.builder()
            .filePath("uploads/sample3.jpg")
            .fileName("城市夜景.jpg")
            .description("城市夜景")
            .score(72)
            .analysis("{\"composition\":{\"score\":78,\"comment\":\"构图较杂乱，主体不够突出。\"},\"lighting\":{\"score\":65,\"comment\":\"暗部细节丢失较多，噪点明显。\"},\"color\":{\"score\":75,\"comment\":\"色彩尚可，但白平衡偏暖。\"},\"focus\":{\"score\":70,\"comment\":\"部分区域对焦模糊。\"}}")
            .suggestions("1.使用三脚架稳定相机\n2.降低ISO减少噪点\n3.开启长时间曝光降噪\n4.寻找更简洁的构图")
            .build());
        
        photoAnalysisRepository.save(PhotoAnalysis.builder()
            .filePath("uploads/sample4.jpg")
            .fileName("微距花卉.jpg")
            .description("花卉微距")
            .score(88)
            .analysis("{\"composition\":{\"score\":90,\"comment\":\"构图精美，花朵位于画面黄金分割点。\"},\"lighting\":{\"score\":85,\"comment\":\"光线柔和均匀，花瓣质感表现出色。\"},\"color\":{\"score\":88,\"comment\":\"色彩鲜艳饱和，层次感强。\"},\"focus\":{\"score\":90,\"comment\":\"焦点精准，花瓣细节清晰可见。\"}}")
            .suggestions("1.可以尝试不同角度拍摄\n2.适当增加一点景深\n3.作品已经很出色，继续保持！")
            .build());
        
        photoAnalysisRepository.save(PhotoAnalysis.builder()
            .filePath("uploads/sample5.jpg")
            .fileName("运动抓拍.jpg")
            .description("足球比赛")
            .score(75)
            .analysis("{\"composition\":{\"score\":70,\"comment\":\"主体位置较偏，可以提前预判构图。\"},\"lighting\":{\"score\":78,\"comment\":\"光线充足，色彩还原较好。\"},\"color\":{\"score\":75,\"comment\":\"色彩对比鲜明，但略有过曝。\"},\"focus\":{\"score\":72,\"comment\":\"部分动作略显模糊，可以提高快门速度。\"}}")
            .suggestions("1.使用更快的快门速度（1/1000s以上）\n2.提前预判运动员的运动轨迹\n3.使用连拍模式提高成功率")
            .build());
    }
}
