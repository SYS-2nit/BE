package com.sys.dbmonitor.domains.dashboard.service.mapping;
import java.util.List;

public record GraphRule(int graphId, int categoryId, String name, List<String> columns) {}