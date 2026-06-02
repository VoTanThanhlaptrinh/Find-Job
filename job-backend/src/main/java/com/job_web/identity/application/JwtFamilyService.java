package com.job_web.identity.application;

public interface JwtFamilyService {
    void saveFamilyJti(String familyId, String jti);

    void deleteFamily(String familyId);

    String getFamilyJti(String familyId);
}
