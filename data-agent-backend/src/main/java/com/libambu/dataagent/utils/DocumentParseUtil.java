package com.libambu.dataagent.utils;

import java.io.File;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DocumentParseUtil {

    /**
     * 根据文件路径解析文档内容。
     *
     * @param filePath 本地文件路径，例如：/Users/yulong/test.pdf
     * @return Spring AI 的 Document 列表，每个 Document 表示解析出来的一段文本内容
     */
    public List<Document> parse(String filePath) {
        // 根据传入的文件路径创建 File 对象，File 表示电脑磁盘上的一个具体文件
        File file = new File(filePath);

        // 获取文件后缀名，并统一转成小写，方便后面判断文件类型
        // 例如：test.PDF 会被处理成 pdf
        String suffix = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();

        // FileSystemResource 是 Spring 提供的资源包装类
        // 它可以把本地磁盘文件包装成 Spring 能识别的 Resource 对象
        Resource resource = new FileSystemResource(file);

        // DocumentReader 是 Spring AI 提供的文档读取器接口
        // 不同类型的文件要使用不同的 DocumentReader 实现类来解析
        DocumentReader documentReader = switch (suffix) {
            // pdf、doc、docx、txt、text 这些格式交给 TikaDocumentReader 解析
            // Tika 是 Apache 提供的文档解析工具，支持很多常见文件格式
            case "pdf", "doc", "docx", "txt", "text" -> new TikaDocumentReader(resource);

            // md、markdown 这类 Markdown 文件交给 MarkdownDocumentReader 解析
            // MarkdownDocumentReader 更适合处理 Markdown 的标题、段落等结构
            // 当前 Spring AI 版本要求 MarkdownDocumentReader 同时传入解析配置
            case "md", "markdown" -> new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.defaultConfig());

            // 如果传入的文件后缀不在上面支持的范围内，就主动抛出异常
            // 这样调用方可以明确知道：当前文件类型暂时不支持解析
            default -> throw new IllegalArgumentException("Unsupported file type: " + suffix);
        };

        // 调用具体读取器的 read 方法，把文件内容解析成 Document 列表并返回
        return documentReader.read();
    }

}
