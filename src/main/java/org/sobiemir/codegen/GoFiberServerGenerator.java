package org.sobiemir.codegen;

import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.AbstractGoCodegen;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

public class GoFiberServerGenerator extends AbstractGoCodegen {
    protected String apiVersion = "1.0.0";
    protected String projectName = "openapi-go-fiber-server";
    protected String sourceFolder = "";
    protected String apiFolder = "";
    protected String modelFolder = "";

    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    public String getName() {
        return "go-fiber-server";
    }

    public String getHelp() {
        return "Generates a go server files using fiber framework. (Experimental)";
    }

    public GoFiberServerGenerator() {
        super();

        generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
                .stability(Stability.EXPERIMENTAL)
                .build();

        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .wireFormatFeatures(EnumSet.of(WireFormatFeature.JSON, WireFormatFeature.XML))
                .securityFeatures(EnumSet.noneOf(
                        SecurityFeature.class))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling)
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism)
                .excludeParameterFeatures(
                        ParameterFeature.Cookie));

        outputFolder = "generated-code" + File.separator + "go-fiber-server";

        modelTemplateFiles.put("model.mustache", ".go");
        apiTemplateFiles.put("api.mustache", ".go");

        embeddedTemplateDir = templateDir = "go-fiber-server";
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        OperationMap operations = objs.getOperations();
        List<CodegenOperation> operationList = operations.getOperation();
        for (CodegenOperation op : operationList) {
            if (op.path != null) {
                op.path = op.path.replaceAll("\\{(.*?)\\}", ":$1");
            }
        }
        return objs;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        this.sourceFolder = getAndUseAdditionalProperty(CodegenConstants.SOURCE_FOLDER, "src");
        this.apiFolder = getAndUseAdditionalProperty(GoFiberServerConstants.API_FOLDER, "");
        this.modelFolder = getAndUseAdditionalProperty(GoFiberServerConstants.MODEL_FOLDER, "");
        this.packageName = getAndUseAdditionalProperty(CodegenConstants.PACKAGE_NAME, "openapi");
        this.modelPackage = getAndUseAdditionalProperty(CodegenConstants.MODEL_PACKAGE, packageName);
        this.apiPackage = getAndUseAdditionalProperty(CodegenConstants.API_PACKAGE, packageName);

        // /*
        // * Additional Properties. These values can be passed to the templates and
        // * are available in models, apis, and supporting files
        // */
        // if (additionalProperties.containsKey("apiVersion")) {
        // this.apiVersion = (String) additionalProperties.get("apiVersion");
        // } else {
        // additionalProperties.put("apiVersion", apiVersion);
        // }

        // if (additionalProperties.containsKey(CodegenConstants.ENUM_CLASS_PREFIX)) {
        // setEnumClassPrefix(
        // Boolean.parseBoolean(additionalProperties.get(CodegenConstants.ENUM_CLASS_PREFIX).toString()));
        // if (enumClassPrefix) {
        // additionalProperties.put(CodegenConstants.ENUM_CLASS_PREFIX, true);
        // }
        // }

        /*
         * set model and package names, this is mainly used inside the templates.
         */
        // modelPackage = MODEL_PACKAGE_NAME;
        // apiPackage = API_PACKAGE_NAME;

        /*
         * Supporting Files. You can write single files for the generator with the
         * entire object tree available. If the input file has a suffix of `.mustache
         * it will be processed by the template engine. Otherwise, it will be copied
         */
        // supportingFiles.add(new SupportingFile("openapi.mustache", ".docs/api",
        // "openapi.yaml"));
        // supportingFiles.add(new SupportingFile("hello-world.mustache", "models",
        // "hello-world.go"));
        // supportingFiles.add(new SupportingFile("go-mod.mustache", "", "go.mod"));
        // supportingFiles.add(new SupportingFile("handler-container.mustache",
        // "handlers", "container.go"));
        // supportingFiles.add(new SupportingFile("main.mustache", "", "main.go"));
        // supportingFiles.add(new SupportingFile("Dockerfile.mustache", "",
        // "Dockerfile"));
        // supportingFiles.add(new SupportingFile("README.mustache", "", "README.md")
        // .doNotOverwrite());
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + this.sourceFolder + File.separator + this.apiFolder;
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + this.sourceFolder + File.separator + this.modelFolder;
    }

    @SuppressWarnings("unchecked")
    private <T> T getAndUseAdditionalProperty(String propertyName, T defaultValue) {
        if (additionalProperties.containsKey(propertyName)) {
            return (T) additionalProperties.get(propertyName);
        } else {
            additionalProperties.put(propertyName, defaultValue);
        }
        return defaultValue;
    }
}
