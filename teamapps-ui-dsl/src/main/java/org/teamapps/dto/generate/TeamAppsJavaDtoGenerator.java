/*-
 * ========================LICENSE_START=================================
 * TeamApps
 * ---
 * Copyright (C) 2014 - 2025 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.dto.generate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;
import org.teamapps.dto.TeamAppsDtoParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class TeamAppsJavaDtoGenerator {
    private final static Logger logger = LoggerFactory.getLogger(TeamAppsJavaDtoGenerator.class);

    private final STGroupFile stGroup;
    private final String packageName;
    private final TeamAppsDtoModel model;

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: sourceDir targetDir packageName");
            System.exit(1);
        }

        File sourceDir = new File(args[0]);
        File targetDir = new File(args[1]);
        String packageName = args[2];

        System.out.println("Generating Java from " + sourceDir.getAbsolutePath() + " to " + targetDir.getAbsolutePath() + " with package name " + packageName);

        new TeamAppsJavaDtoGenerator(packageName, new TeamAppsDtoModel(TeamAppsGeneratorUtil.parseClassCollections(sourceDir)))
                .generate(targetDir);
    }

    public TeamAppsJavaDtoGenerator(TeamAppsDtoModel model) throws IOException {
        this("org.teamapps.dto", model);
    }

    public TeamAppsJavaDtoGenerator(String packageName, TeamAppsDtoModel model) throws IOException {
        this.packageName = packageName;
        this.model = model;
        stGroup = StGroupFactory.createStGroup("/org/teamapps/dto/TeamAppsJavaDtoGenerator.stg", this.model);
    }

    public void generate(File targetDir) throws IOException {
        FileUtils.deleteDirectory(targetDir);
        File parentDir = FileUtils.createDirectory(new File(targetDir, packageName.replace('.', '/')));

        for (TeamAppsDtoParser.ClassDeclarationContext clazzContext : model.getClassDeclarations()) {
            logger.info("Generating class: " + clazzContext.Identifier());
            generateClass(clazzContext, new FileWriter(new File(parentDir, clazzContext.Identifier() + ".java")));
            if (model.isReferenceableBaseClass(clazzContext)) {
                generateClassReference(clazzContext, new FileWriter(new File(parentDir, clazzContext.Identifier() + "Reference.java")));
            }
        }
        for (TeamAppsDtoParser.InterfaceDeclarationContext interfaceContext : model.getInterfaceDeclarations()) {
            logger.info("Generating interface: " + interfaceContext.Identifier());
            generateInterface(interfaceContext, new FileWriter(new File(parentDir, interfaceContext.Identifier() + ".java")));
        }
        for (TeamAppsDtoParser.EnumDeclarationContext enumContext : model.getEnumDeclarations()) {
            generateEnum(enumContext, new FileWriter(new File(parentDir, enumContext.Identifier() + ".java")));
        }
        generateObjectTypeEnum(new FileWriter(new File(parentDir, "UiObjectType.java")));
        generateUiObjectBaseClass(new FileWriter(new File(parentDir, "UiObject.java")));

        generateUiEventBaseClass(new FileWriter(new File(parentDir, "UiEvent.java")));
        generateEventEnum(new FileWriter(new File(parentDir, "UiEventType.java")));

        generateUiQueryBaseClass(new FileWriter(new File(parentDir, "UiQuery.java")));
        generateQueryEnum(new FileWriter(new File(parentDir, "UiQueryType.java")));

        generateComponentCommandBaseClass(new FileWriter(new File(parentDir, "UiCommand.java")));

        generateJacksonTypeIdMaps(new FileWriter(new File(parentDir, "UiObjectJacksonTypeIdMaps.java")));
    }

    void generateClass(TeamAppsDtoParser.ClassDeclarationContext clazzContext, Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("class")
                .add("package", packageName)
                .add("c", clazzContext);
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateClassReference(TeamAppsDtoParser.ClassDeclarationContext clazzContext, Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("classReference")
                .add("package", packageName)
                .add("c", clazzContext);
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateInterface(TeamAppsDtoParser.InterfaceDeclarationContext interfaceContext, Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("interface")
                .add("package", packageName)
                .add("i", interfaceContext);
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateEnum(TeamAppsDtoParser.EnumDeclarationContext enumContext, Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("enumClass")
                .add("package", packageName)
                .add("e", enumContext);
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateUiEventBaseClass(Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("uiEventBaseClass")
                .add("package", packageName)
                .add("allEventDeclarations", model.getEventDeclarations());
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateUiQueryBaseClass(Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("uiQueryBaseClass")
                .add("package", packageName)
                .add("allQueryDeclarations", model.getQueryDeclarations());
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateComponentCommandBaseClass(Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("uiCommandBaseClass")
                .add("package", packageName)
                .add("allCommandDeclarations", model.getCommandDeclarations());
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateObjectTypeEnum(Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("uiObjectTypeEnum")
                .add("package", packageName)
                .add("allClasses", model.getClassDeclarations());
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateUiObjectBaseClass(Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("uiObjectBaseClass")
                .add("package", packageName)
                .add("allClasses", model.getClassDeclarations());
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateEventEnum(Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("uiEventEnum")
                .add("package", packageName)
                .add("allEventDeclarations", model.getEventDeclarations());
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateQueryEnum(Writer writer) throws IOException {
        ST template = stGroup.getInstanceOf("uiQueryEnum")
                .add("package", packageName)
                .add("allQueryDeclarations", model.getQueryDeclarations());
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

    void generateJacksonTypeIdMaps(Writer writer) throws IOException {
        ArrayList<Object> allJsonSerializableClasses = new ArrayList<>();
        allJsonSerializableClasses.addAll(model.getClassDeclarations());
        allJsonSerializableClasses.addAll(model.getInterfaceDeclarations());
        allJsonSerializableClasses.addAll(model.getCommandDeclarations());
        allJsonSerializableClasses.addAll(model.getEventDeclarations());
        allJsonSerializableClasses.addAll(model.getQueryDeclarations());
        ST template = stGroup.getInstanceOf("jacksonTypeIdMaps")
                .add("package", packageName)
                .add("allJsonSerializableClasses", allJsonSerializableClasses);
        AutoIndentWriter out = new AutoIndentWriter(writer);
        template.write(out, new StringTemplatesErrorListener());
        writer.close();
    }

}
