package com.nlu.shared.application;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
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

    /**
     * Làm sạch HTML chống Stored XSS trước khi lưu DB
     * @param html Chuỗi HTML đầu vào
     * @return Chuỗi HTML an toàn
     */
    public String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, Safelist.relaxed());
    }
}
