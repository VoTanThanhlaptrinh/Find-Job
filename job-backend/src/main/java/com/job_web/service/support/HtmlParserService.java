package com.job_web.service.support;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Service
public class HtmlParserService {

    /**
     * Trích xuất text thuần từ HTML
     * @param html Chuỗi HTML cần parse
     * @return Text đã được trích xuất
     */
    public String parseHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parse(html).wholeText();
    }
}
