package com.job_web.utills;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Utility class for accessing i18n messages.
 * This component provides methods to retrieve localized messages
 * using the current locale from LocaleContextHolder.
 */
@Component
public class MessageUtils {

    private static MessageSource messageSource;

    public MessageUtils(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }

    /**
     * Get a localized message for the given key using the current locale.
     *
     * @param key the message key
     * @return the localized message
     */
    public static String getMessage(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }

    /**
     * Get a localized message for the given key with arguments using the current locale.
     *
     * @param key  the message key
     * @param args the arguments to be used in the message
     * @return the localized message
     */
    public static String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    /**
     * Get a localized message for the given key using a specific locale.
     *
     * @param key    the message key
     * @param locale the locale to use
     * @return the localized message
     */
    public static String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    /**
     * Get a localized message for the given key with arguments using a specific locale.
     *
     * @param key    the message key
     * @param locale the locale to use
     * @param args   the arguments to be used in the message
     * @return the localized message
     */
    public static String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }

    /**
     * Get the current locale from LocaleContextHolder.
     *
     * @return the current locale
     */
    public static Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }
}
