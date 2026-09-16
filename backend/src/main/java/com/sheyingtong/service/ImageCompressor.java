package com.sheyingtong.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 图片压缩（发给 AI 之前把图压到阈值以内）
 * ----------------------------------------------------------------
 * 抽出来的理由：改造前 PhotoService 与 RetouchService 各有一份**逐字节相同**的实现
 * （PhotoService:502-535、RetouchService:361-394），异步化后两个消费者都要用它，
 * 再复制第三份没有意义。
 *
 * ⚠️ 抽取时顺带修了原实现的一个真实 bug：
 *   原代码写了 `while (quality > 0.1f) { ... ImageIO.write(...); quality -= 0.1f; }`
 *   的降质量循环，但 **quality 从来没有传给 ImageIO.write**（用的是静态无参重载），
 *   循环每一轮写出的 JPEG 完全一样 → 只有当"按尺寸缩放后仍然超标"时才会靠循环
 *   空转到 quality <= 0.1 退出，质量参数实际上一点作用都没起。
 *   这里改用 ImageWriter + ImageWriteParam.setCompressionQuality(quality) 让它真正生效。
 *
 * 压缩结果只用于发给大模型，不落盘、不影响前端展示，所以修正它是安全的。
 */
@Component
@Slf4j
public class ImageCompressor {

    /** DashScope 单张图片建议 &lt; 2MB，这里取原实现沿用的 983616 字节（≈960KB） */
    private static final int MAX_BYTES = 983616;

    private static final float START_QUALITY = 0.9f;
    private static final float MIN_QUALITY = 0.1f;
    private static final float QUALITY_STEP = 0.1f;

    /**
     * 压缩图片到 {@link #MAX_BYTES} 以内。
     *
     * @param imageData 原图字节
     * @return 压缩后的 JPEG 字节；原图已足够小则原样返回
     */
    public byte[] compress(byte[] imageData) throws IOException {
        if (imageData.length <= MAX_BYTES) {
            return imageData;
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            // ImageIO 认不出的格式（如 HEIC）：原样返回，让模型侧去报错，别在这里假装成功
            log.warn("[image] 无法解码图片（可能是 HEIC 等非常规格式），跳过压缩，原样发送 {} bytes", imageData.length);
            return imageData;
        }

        // 按面积等比缩放：目标字节数 / 当前字节数 开平方 ≈ 每边要缩的比例
        double scale = Math.sqrt((double) MAX_BYTES / imageData.length) * 0.9;
        int newWidth = Math.max(1, (int) (image.getWidth() * scale));
        int newHeight = Math.max(1, (int) (image.getHeight() * scale));

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        } finally {
            // 原实现没有 dispose，AWT 的图形上下文会泄漏（高频上传下累积）
            graphics.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (float quality = START_QUALITY; quality > MIN_QUALITY; quality -= QUALITY_STEP) {
            baos.reset();
            if (writeJpeg(resized, baos, quality)) {
                if (baos.size() <= MAX_BYTES) {
                    log.info("[image] 压缩完成: {}x{} → {}x{}, quality={}, {} bytes → {} bytes",
                            image.getWidth(), image.getHeight(), newWidth, newHeight,
                            String.format("%.1f", quality), imageData.length, baos.size());
                    return baos.toByteArray();
                }
            } else {
                // 拿不到 JPEG writer（JDK 环境异常）：原样返回，不阻断主流程
                return imageData;
            }
        }

        log.warn("[image] 质量降到 {} 仍未压到 {} bytes 以内，按当前结果发送 {} bytes",
                MIN_QUALITY, MAX_BYTES, baos.size());
        return baos.toByteArray();
    }

    /** 用指定质量写一次 JPEG；返回 false 表示当前 JVM 没有可用的 JPEG 编码器 */
    private boolean writeJpeg(BufferedImage image, ByteArrayOutputStream baos, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            log.warn("[image] 当前 JVM 没有可用的 JPEG ImageWriter");
            return false;
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos)) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
        return true;
    }
}
