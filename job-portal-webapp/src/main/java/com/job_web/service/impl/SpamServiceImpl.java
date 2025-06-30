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
        if(spamIp.opsForValue().get(String.format("ip_spam_login_%s", ip)) != null && Integer.parseInt(String.valueOf(spamIp.opsForValue().get(String.format("ip_spam_login_%s", ip)))) >= maxAttempts*2 ) {
            blockIP.opsForValue().set(String.format("block_ip_login_%s", ip),"Bạn bị chặn vì spam, thời hạn 1 ngày tính từ thông báo này", Duration.ofSeconds(timeoutSecond));
            return;
        }
        if(spamIp.opsForValue().get(String.format("ip_spam_login_%s", ip)) != null && Integer.parseInt(String.valueOf(spamIp.opsForValue().get(String.format("ip_spam_login_%s", ip)))) == maxAttempts) {
            blockIP.opsForValue().set(String.format("block_ip_login_%s", ip),"Bạn bị chặn vì spam, thời hạn 10 phút tính từ thông báo này", Duration.ofSeconds(timeoutFirst));
            return;
        }
        if(spamIp.hasKey(String.format("ip_spam_login_%s", ip))) {
            spamIp.opsForValue().set(String.format("ip_spam_login_%s", ip), Integer.parseInt(String.valueOf(spamIp.opsForValue().get(String.format("ip_spam_login_%s", ip))))+1);
            return;
        }
        spamIp.opsForValue().set(String.format("ip_spam_login_%s", ip), 1, Duration.ofSeconds(timeoutFirst));
    }

    @Override
    public boolean checkIpSpamLogin(String ip) {
        return blockIP.opsForValue().get(String.format("block_ip_login_%s", ip)) != null;
    }

    @Override
    public void deleteIpSpamLogin(String ip) {
        if(blockIP.opsForValue().get(String.format("block_ip_login_%s", ip)) != null) {
            spamIp.delete(String.format("ip_spam_login_%s", ip));
        }

    }

    @Override
    public void addIpSpamEmail(String ip) {
        if(spamIp.opsForValue().get(String.format("ip_spam_email_%s", ip)) != null && Integer.parseInt(String.valueOf(spamIp.opsForValue().get(String.format("ip_spam_email_%s", ip)))) >= maxAttempts*2 ) {
            blockIP.opsForValue().set(String.format("block_ip_email_%s", ip),"Bạn bị chặn vì spam, thời hạn 1 ngày tính từ thông báo này", Duration.ofSeconds(timeoutSecond));
            return;
        }
        if(spamIp.opsForValue().get(String.format("ip_spam_email_%s", ip)) != null && Integer.parseInt(String.valueOf(spamIp.opsForValue().get(String.format("ip_spam_email_%s", ip)))) == maxAttempts) {
            blockIP.opsForValue().set(String.format("block_ip_email_%s", ip),"Bạn bị chặn vì spam, thời hạn 10 phút tính từ thông báo này", Duration.ofSeconds(timeoutFirst));
            return;
        }

        if(spamIp.hasKey(String.format("ip_spam_login_%s", ip))) {
            spamIp.opsForValue().increment(String.format("ip_spam_login_%s", ip), 1);
            return;
        }
        spamIp.opsForValue().set(String.format("ip_spam_email_%s", ip), 1,Duration.ofSeconds(timeoutFirst));
    }

    @Override
    public boolean checkIpSpamEmail(String ip) {
        return blockIP.opsForValue().get(String.format("block_ip_email_%s", ip)) != null;
    }

    @Override
    public void deleteInSpamEmail(String ip) {
        spamIp.delete(String.format("ip_spam_email_%s", ip));
    }

    @Override
    public String getMessageLoginSpam(String ip) {
        return Objects.requireNonNull(blockIP.opsForValue().get(String.format("block_ip_login_%s", ip))).toString();
    }

    @Override
    public String getMessageEmailSpam(String ip) {
        return Objects.requireNonNull(blockIP.opsForValue().get(String.format("block_ip_email_%s", ip))).toString();
    }
}
