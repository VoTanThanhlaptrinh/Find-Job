package com.job_web.controller.account;

import com.job_web.config.I18nConfig;
import com.job_web.dto.common.ApiResponse;
import com.job_web.utills.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/language", produces = "application/json")
@RequiredArgsConstructor
public class LanguageController {

    /**
     * Get current language and list of supported languages.
     * The response language is determined by the Accept-Language header.
     * 
     * Usage:
     * - To get Vietnamese response: Add header "Accept-Language: vi"
     * - To get English response: Add header "Accept-Language: en"
     * - Default language is Vietnamese if no header is provided
     *
     * @return current locale and supported locales
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLanguageInfo() {
        Locale currentLocale = LocaleContextHolder.getLocale();
        
        Map<String, Object> data = new HashMap<>();
        data.put("currentLanguage", currentLocale.getLanguage());
        data.put("currentLocale", currentLocale.toString());
        data.put("supportedLanguages", I18nConfig.SUPPORTED_LOCALES.stream()
                .map(locale -> Map.of(
                        "code", locale.getLanguage(),
                        "locale", locale.toString(),
                        "name", locale.getDisplayLanguage(locale)
                ))
                .toList());
        
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }
}

