package com.job_web.service.impl;

import com.job_web.service.SpamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpamServiceImpl implements SpamService {
    private final RedisTemplate<String, Object> spamIp;
    private final RedisTemplate<String, Object> blockIP;

    @Value("${application.service.impl.time-out-first}")
    private long timeoutFirst;

    @Value("${application.service.impl.time-out-second}")
    private long timeoutSecond;

    @Value("${application.service.impl.max-attemps}")
    private int maxAttempts;

    @Override
    public void addIpSpamLogin(String ip) {
        String key = String.format("ip_spam_login_%s", ip);
        String blockKey = String.format("block_ip_login_%s", ip);

        int attempts = getAttempts(key);

        if (attempts >= maxAttempts * 2) {
            blockIP.opsForValue().set(blockKey, "Bạn bị chặn vì spam, thời hạn 1 ngày tính từ thông báo này", Duration.ofSeconds(timeoutSecond));
            return;
        }

        if (attempts >= maxAttempts) {
            blockIP.opsForValue().set(blockKey, "Bạn bị chặn vì spam, thời hạn 10 phút tính từ thông báo này", Duration.ofSeconds(timeoutFirst));
            return;
        }

       spamIp.opsForValue().set(key, attempts + 1, Duration.ofSeconds(timeoutFirst));
    }

    @Override
    public boolean checkIpSpamLogin(String ip) {
        return blockIP.hasKey(String.format("block_ip_login_%s", ip));
    }

    @Override
    public void deleteIpSpamLogin(String ip) {
        String blockKey = String.format("block_ip_login_%s", ip);
        if (blockIP.hasKey(blockKey)) {
            spamIp.delete(String.format("ip_spam_login_%s", ip));
        }
    }

    @Override
    public void addIpSpamEmail(String ip) {
        String key = String.format("ip_spam_email_%s", ip);
        String blockKey = String.format("block_ip_email_%s", ip);

        int attempts = getAttempts(key);
        log.info("Email spam attempts for {}: {}", ip, attempts);

        if (attempts >= maxAttempts * 2) {
            blockIP.opsForValue().set(blockKey, "Bạn bị chặn vì spam, thời hạn 1 ngày tính từ thông báo này", Duration.ofSeconds(timeoutSecond));
            return;
        }

        if (attempts >= maxAttempts) {
            blockIP.opsForValue().set(blockKey, "Bạn bị chặn vì spam, thời hạn 10 phút tính từ thông báo này", Duration.ofSeconds(timeoutFirst));
            return;
        }

        spamIp.opsForValue().set(key, attempts + 1, Duration.ofSeconds(timeoutFirst));
    }

    @Override
    public boolean checkIpSpamEmail(String ip) {
        return blockIP.hasKey(String.format("block_ip_email_%s", ip));
    }

    @Override
    public void deleteInSpamEmail(String ip) {
        spamIp.delete(String.format("ip_spam_email_%s", ip));
    }

    @Override
    public String getMessageLoginSpam(String ip) {
        return Objects.toString(blockIP.opsForValue().get(String.format("block_ip_login_%s", ip)), "");
    }

    @Override
    public String getMessageEmailSpam(String ip) {
        return Objects.toString(blockIP.opsForValue().get(String.format("block_ip_email_%s", ip)), "");
    }

    private Integer getAttempts(String key) {
        Object value = spamIp.opsForValue().get(key);
        return value != null ? Integer.parseInt(value.toString()) : 0;
    }
}
