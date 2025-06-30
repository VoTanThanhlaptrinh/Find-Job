package com.job_web.service;

public interface SpamService {
    void addIpSpamLogin(String ip);
    boolean checkIpSpamLogin(String ip);
    void deleteIpSpamLogin(String ip);
    void addIpSpamEmail(String ip);
    boolean checkIpSpamEmail(String ip);
    void deleteInSpamEmail(String ip);
    String getMessageLoginSpam(String ip);
    String getMessageEmailSpam(String ip);
}
