package com.job_web.service.support.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.job_web.service.support.ReferenceService;

@Service
public class RefServiceImpl implements ReferenceService{

	@Override
	public String getRef(int numChar) {
		// TODO Auto-generated method stub
		return UUID.randomUUID().toString().replace("-", "").substring(0, numChar);
	}

	@Override
	public String getRef() {
		// TODO Auto-generated method stub
		return UUID.randomUUID().toString();
	}
	
}



