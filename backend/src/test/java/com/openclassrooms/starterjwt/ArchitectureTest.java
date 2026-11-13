package com.openclassrooms.starterjwt;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {
    @Test
    void controllersDoNotAccessRepositories() {
        var classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.openclassrooms.starterjwt");
        noClasses().that().resideInAPackage("..controllers..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("controllers delegate persistence decisions to services")
                .check(classes);
    }
}
