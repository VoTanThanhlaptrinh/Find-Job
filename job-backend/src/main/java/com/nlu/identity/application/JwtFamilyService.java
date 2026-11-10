package com.nlu.identity.application;

public interface JwtFamilyService {
    void saveFamilyJti(String familyId, String jti);

    void deleteFamily(String familyId);

    String getFamilyJti(String familyId);

    void saveUserFamily(String username, String familyId);

    void revokeAllUserFamilies(String username);
}
