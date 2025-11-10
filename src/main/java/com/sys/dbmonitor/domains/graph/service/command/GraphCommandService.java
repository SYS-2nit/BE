package com.sys.dbmonitor.domains.graph.service.command;

import com.sys.dbmonitor.domains.graph.domain.Graph;
import com.sys.dbmonitor.domains.graph.dto.request.GraphCreateRequest;
import com.sys.dbmonitor.domains.graph.dto.request.GraphUpdateRequest;
import com.sys.dbmonitor.domains.graph.repository.GraphRepository;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GraphCommandService {

    private static final Logger log = LoggerFactory.getLogger(GraphCommandService.class);

    private final GraphRepository graphRepository;

    @Transactional
    public Graph createGraph(GraphCreateRequest request) {
        // 이름 중복 확인
        if (graphRepository.existsByName(request.name())) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 그래프 이름입니다.");
        }

        Graph graph = request.toEntity();
        Graph saved = graphRepository.save(graph);
        log.info("[Graph] 그래프 등록 완료: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public Graph updateGraph(Long id, GraphUpdateRequest request) {
        Graph graph = graphRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "그래프를 찾을 수 없습니다."));

        // 이름 중복 확인 (자신 제외)
        if (request.name() != null && !request.name().equals(graph.getName())) {
            if (graphRepository.existsByNameAndIdNot(request.name(), id)) {
                throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 그래프 이름입니다.");
            }
        }

        // 엔티티 업데이트
        graph.update(
                request.name(),
                request.category(),
                request.info(),
                request.type()
        );

        Graph saved = graphRepository.save(graph);
        log.info("[Graph] 그래프 수정 완료: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public void deleteGraph(Long id) {
        Graph graph = graphRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "그래프를 찾을 수 없습니다."));

        graphRepository.delete(graph);
        log.info("[Graph] 그래프 삭제 완료: id={}, name={}", id, graph.getName());
    }
}

