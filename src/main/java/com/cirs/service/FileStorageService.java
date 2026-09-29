package com.cirs.service;

import com.cirs.entity.Incident;
import com.cirs.entity.IncidentAttachment;
import com.cirs.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    IncidentAttachment storeEvidence(MultipartFile file, Incident incident, User uploader);
    Resource loadFileAsResource(IncidentAttachment attachment);
}
