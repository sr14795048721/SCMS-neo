package com.scms.core.changelog.service;

import com.scms.core.changelog.domain.VersionChangelogEntity;
import com.scms.core.changelog.dto.VersionChangelogMutationRequest;
import com.scms.core.changelog.dto.VersionChangelogResponse;
import com.scms.core.changelog.repository.VersionChangelogRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VersionChangelogService {

    private final VersionChangelogRepository versionChangelogRepository;

    public VersionChangelogService(VersionChangelogRepository versionChangelogRepository) {
        this.versionChangelogRepository = versionChangelogRepository;
    }

    @Transactional(readOnly = true)
    public List<VersionChangelogResponse> listChangelogs() {
        return versionChangelogRepository.findAllByOrderByReleasedAtDescIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VersionChangelogResponse createChangelog(VersionChangelogMutationRequest request) {
        if (versionChangelogRepository.existsByVersion(request.version().trim())) {
            throw new BusinessException(ErrorCode.CONFLICT, "version already exists");
        }
        VersionChangelogEntity entity = new VersionChangelogEntity();
        apply(entity, request);
        return toResponse(versionChangelogRepository.save(entity));
    }

    @Transactional
    public VersionChangelogResponse updateChangelog(Long changelogId, VersionChangelogMutationRequest request) {
        VersionChangelogEntity entity = getRequiredChangelog(changelogId);
        apply(entity, request);
        return toResponse(versionChangelogRepository.save(entity));
    }

    @Transactional
    public void deleteChangelog(Long changelogId) {
        VersionChangelogEntity entity = getRequiredChangelog(changelogId);
        versionChangelogRepository.delete(entity);
    }

    private void apply(VersionChangelogEntity entity, VersionChangelogMutationRequest request) {
        entity.setVersion(request.version().trim());
        entity.setTitle(request.title().trim());
        entity.setContent(request.content().trim());
        entity.setReleasedAt(request.releasedAt());
    }

    private VersionChangelogEntity getRequiredChangelog(Long changelogId) {
        return versionChangelogRepository.findById(changelogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "version changelog not found"));
    }

    private VersionChangelogResponse toResponse(VersionChangelogEntity entity) {
        return new VersionChangelogResponse(
                entity.getId(),
                entity.getVersion(),
                entity.getTitle(),
                entity.getContent(),
                entity.getReleasedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
