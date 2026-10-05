package com.scms.core.changelog;

import com.scms.core.changelog.domain.VersionChangelogEntity;
import com.scms.core.changelog.dto.VersionChangelogMutationRequest;
import com.scms.core.changelog.repository.VersionChangelogRepository;
import com.scms.core.changelog.service.VersionChangelogService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VersionChangelogServiceTest {

    private VersionChangelogRepository repository;
    private VersionChangelogService service;

    @BeforeEach
    void setUp() {
        repository = mock(VersionChangelogRepository.class);
        service = new VersionChangelogService(repository);
    }

    @Test
    void listShouldReturnChangelogsNewestFirst() {
        VersionChangelogEntity older = entity(1L, "v0.1.0", "旧版本");
        VersionChangelogEntity newer = entity(2L, "v0.2.0", "新版本");
        when(repository.findAllByOrderByReleasedAtDescIdDesc()).thenReturn(List.of(newer, older));

        var result = service.listChangelogs();

        assertEquals(2, result.size());
        assertEquals("v0.2.0", result.get(0).version());
        assertEquals("新版本", result.get(0).title());
        assertEquals("v0.1.0", result.get(1).version());
    }

    @Test
    void createShouldRejectDuplicateVersion() {
        when(repository.existsByVersion("v0.1.0")).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createChangelog(request("v0.1.0", "重复版本"))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldRejectMissingChangelog() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteChangelog(99L)
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    private VersionChangelogMutationRequest request(String version, String title) {
        return new VersionChangelogMutationRequest(version, title, "更新内容", Instant.parse("2026-10-05T00:00:00Z"));
    }

    private VersionChangelogEntity entity(Long id, String version, String title) {
        VersionChangelogEntity entity = new VersionChangelogEntity();
        setId(entity, id);
        entity.setVersion(version);
        entity.setTitle(title);
        entity.setContent("内容");
        entity.setReleasedAt(Instant.parse("2026-10-05T00:00:00Z"));
        return entity;
    }

    private void setId(Object target, Long id) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(target, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
        throw new IllegalStateException("id field not found");
    }
}
