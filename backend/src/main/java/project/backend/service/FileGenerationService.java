package project.backend.service;

import project.backend.entity.Project;

import java.util.List;

public interface FileGenerationService {

    String generateFileStructure(Project project);

    String updateFileStructure(Project project, long userId);

    List<String> performFileStructureCheck(Project project, long userId);
}
