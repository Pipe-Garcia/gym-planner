package com.gymplanner.template.dto;

import java.util.List;

public record TemplateDayResponse(Long id, int orderIndex, String name, String notes, List<TemplateBlockResponse> blocks) {}
