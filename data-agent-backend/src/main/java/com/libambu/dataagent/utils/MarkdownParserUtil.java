package com.libambu.dataagent.utils;

/**
 * Markdown 代码块解析工具。
 * <p>
 * 从 LLM 返回的 Markdown 格式文本中提取代码块内容。
 */
public final class MarkdownParserUtil {

    private MarkdownParserUtil() {
    }

    /**
     * 提取 Markdown 代码块中的文本，并将换行替换为空格。
     */
    public static String extractText(String markdownCode) {
        String code = extractRawText(markdownCode);
        return code.replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll("\r", " ");
    }

    /**
     * 提取 Markdown 代码块中的原始文本（保留换行）。
     * <p>
     * 如果没有找到代码块标记，则返回原始文本。
     */
    public static String extractRawText(String markdownCode) {
        // 查找代码块起始标记（3个或更多反引号）
        int startIndex = -1;
        int delimiterLength = 0;

        for (int i = 0; i <= markdownCode.length() - 3; i++) {
            if (markdownCode.substring(i, i + 3).equals("```")) {
                startIndex = i;
                delimiterLength = 3;
                // 计算额外的反引号
                while (i + delimiterLength < markdownCode.length()
                        && markdownCode.charAt(i + delimiterLength) == '`') {
                    delimiterLength++;
                }
                break;
            }
        }

        if (startIndex == -1) {
            return markdownCode; // 没有找到代码块
        }

        // 跳过开头的分隔符和可选的语言标识
        int contentStart = startIndex + delimiterLength;
        while (contentStart < markdownCode.length() && markdownCode.charAt(contentStart) != '\n') {
            contentStart++;
        }
        if (contentStart < markdownCode.length() && markdownCode.charAt(contentStart) == '\n') {
            contentStart++; // 跳过语言标识后的换行
        }

        // 查找结束分隔符
        String closingDelimiter = "`".repeat(delimiterLength);
        int endIndex = markdownCode.indexOf(closingDelimiter, contentStart);

        if (endIndex == -1) {
            // 没有找到结束分隔符，返回从内容开始到末尾的文本
            return markdownCode.substring(contentStart);
        }

        // 提取分隔符之间的内容
        return markdownCode.substring(contentStart, endIndex);
    }
}
