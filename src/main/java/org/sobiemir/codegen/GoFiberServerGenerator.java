package org.sobiemir.codegen;

import org.openapitools.codegen.*;
import org.openapitools.codegen.api.TemplatingEngineAdapter;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.templating.HandlebarsEngineAdapter;
import org.openapitools.codegen.utils.ModelUtils;

import com.github.jknack.handlebars.internal.lang3.StringUtils;

import io.swagger.v3.oas.models.media.Schema;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.openapitools.codegen.utils.StringUtils.camelize;

public class GoFiberServerGenerator extends GoFiberServerBase {
    protected String apiVersion = "1.0.0";
    protected String projectName = "openapi-go-fiber-server";
    protected String sourceFolder = "";
    protected String apiFolder = "";
    protected String modelFolder = "";

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

        apiTemplateFiles.put("api.hbs", ".go");
        modelTemplateFiles.put("model.hbs", ".go");

        embeddedTemplateDir = templateDir = "go-fiber-server";
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "go-fiber-server";
    }

    @Override
    public String getHelp() {
        return "Generates a go server files using fiber framework. (Experimental)";
    }

    @Override
    @SuppressWarnings("rawtypes")
    public CodegenProperty fromProperty(String name, Schema p, boolean required,
            boolean schemaIsFromAdditionalProperties) {
        CodegenProperty property = super.fromProperty(name, p, required, schemaIsFromAdditionalProperties);

        if (property.isEnumRef && StringUtils.isNotBlank(property.getRef())) {
            String simpleRef = ModelUtils.getSimpleRef(property.getRef());
            Schema refSchema = ModelUtils.getSchema(openAPI, simpleRef);

            if (StringUtils.isBlank(property.description)) {
                property.setDescription(refSchema.getDescription());
            }
            if (refSchema.getDefault() != null) {
                property.setDefaultValue(toDefaultValue(refSchema));
            }
        }

        return property;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public String toDefaultValue(Schema p) {
        boolean isBoolean = ModelUtils.isBooleanSchema(p);
        boolean isNumber = ModelUtils.isNumberSchema(p);
        boolean isInteger = ModelUtils.isIntegerSchema(p);
        boolean isDate = ModelUtils.isDateSchema(p);
        boolean isDateTime = ModelUtils.isDateTimeSchema(p);
        boolean isString = ModelUtils.isStringSchema(p);

        if (isBoolean || isNumber || isInteger) {
            if (p.getDefault() != null) {
                return p.getDefault().toString();
            }
        } else if (isDate || isDateTime || isString) {
            if (p.getDefault() != null) {
                return "\"" + p.getDefault().toString() + "\"";
            }
        }

        return null;
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        if (enumClassPrefix) {
            return datatype + value;
        }
        return value;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        String varName = super.toEnumVarName(name, datatype);
        return camelize(varName.toLowerCase());
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationMap operationMap = objs.getOperations();

        operationMap.put("variableName", toParamName(operationMap.getPathPrefix()));

        List<CodegenOperation> operations = operationMap.getOperation();

        normalizeRouterData(operations);

        List<Map<String, String>> imports = objs.getImports();
        if (imports == null)
            return objs;

        removeModelImports(imports);

        boolean addedTimeImport = false;
        boolean addedOSImport = false;

        for (CodegenOperation operation : operations) {
            // import "os" if the operation uses files
            if (!addedOSImport && "*os.File".equals(operation.returnType)) {
                imports.add(createMapping("import", "os"));
                addedOSImport = true;
            }

            for (CodegenParameter param : operation.allParams) {
                // import "os" if the operation uses files
                if (!addedOSImport && "*os.File".equals(param.dataType)) {
                    imports.add(createMapping("import", "os"));
                    addedOSImport = true;
                }

                // import "time" if the operation has a required time parameter.
                if (param.required || !usesOptionals) {
                    if (!addedTimeImport && "time.Time".equals(param.dataType)) {
                        imports.add(createMapping("import", "time"));
                        addedTimeImport = true;
                    }
                }

                setExportParameterName(param);
            }
        }

        return addRecursiveImports(objs, imports);
    }

    @Override
    public void processOpts() {
        setLegacyDiscriminatorBehavior(false);

        super.processOpts();

        sourceFolder = getAndUseAdditionalProperty(CodegenConstants.SOURCE_FOLDER, "src");
        apiFolder = getAndUseAdditionalProperty(GoFiberServerConstants.API_FOLDER, "");
        modelFolder = getAndUseAdditionalProperty(GoFiberServerConstants.MODEL_FOLDER, "");
        packageName = getAndUseAdditionalProperty(CodegenConstants.PACKAGE_NAME, "openapi");
        modelPackage = getAndUseAdditionalProperty(CodegenConstants.MODEL_PACKAGE, packageName);
        apiPackage = getAndUseAdditionalProperty(CodegenConstants.API_PACKAGE, packageName);
        enumClassPrefix = getAndUseAdditionalProperty(CodegenConstants.ENUM_CLASS_PREFIX, true);

        TemplatingEngineAdapter templatingEngine = getTemplatingEngine();
        if (!(templatingEngine instanceof HandlebarsEngineAdapter)) {
            throw new RuntimeException("Only the HandlebarsEngineAdapter is supported for this generator");
        }

        supportingFiles.add(new SupportingFile("api-container.hbs",
                sourceFolder, "api.go"));
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + sourceFolder + File.separator + apiFolder;
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + sourceFolder + File.separator + modelFolder;
    }
}
