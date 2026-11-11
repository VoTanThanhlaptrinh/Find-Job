package com.nlu.shared.application.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nlu.shared.application.ReferenceService;

@Service
public class ReferenceServiceImpl implements ReferenceService {

	@Override
	public String getRef(int numChar) {
		return UUID.randomUUID().toString().replace("-", "").substring(0, numChar);
	}
}
