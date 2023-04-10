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

import io.swagger.v3.oas.models.media.Schema;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class GoFiberServerGenerator extends GoFiberServerBase {
    protected String apiVersion = "1.0.0";
    protected String projectName = "openapi-go-fiber-server";
    protected String sourceFolder = "";
    protected String apiFolder = "";
    protected String modelFolder = "";

    public GoFiberServerGenerator() {
        super();

        this.generatorMetadata = GeneratorMetadata.newBuilder(this.generatorMetadata)
                .stability(Stability.EXPERIMENTAL)
                .build();

        this.modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .wireFormatFeatures(EnumSet.of(
                        WireFormatFeature.JSON,
                        WireFormatFeature.XML))
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

        this.outputFolder = "generated-code" + File.separator + "go-fiber-server";

        this.apiTemplateFiles.put("api.hbs", ".go");
        this.modelTemplateFiles.put("model.hbs", ".go");

        this.embeddedTemplateDir = this.templateDir = "go-fiber-server";
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
    public CodegenProperty fromProperty(String name, Schema p, boolean required, boolean isFromAdditionalProperties) {
        CodegenProperty property = super.fromProperty(name, p, required, isFromAdditionalProperties);
        String propertyRef = property.getRef();

        if (property.isEnumRef && !StringExtensions.isNullOrWhiteSpace(propertyRef)) {
            String simpleRef = ModelUtils.getSimpleRef(propertyRef);
            Schema refSchema = ModelUtils.getSchema(this.openAPI, simpleRef);

            if (StringExtensions.isNullOrWhiteSpace(property.description)) {
                property.description = this.escapeText(refSchema.getDescription());
                property.unescapedDescription = refSchema.getDescription();
            }

            if (refSchema.getDefault() != null) {
                property.defaultValue = this.toDefaultValue(refSchema);
            }
        }

        if (!StringExtensions.isNullOrWhiteSpace(property.unescapedDescription)) {
            property.vendorExtensions.put(
                    VendorConstants.X_EXPLODED_DESCRIPTION,
                    StringExtensions.lines(property.unescapedDescription));
        }

        return property;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public String toDefaultValue(Schema p) {
        if (ModelUtils.isBooleanSchema(p) || ModelUtils.isNumberSchema(p) || ModelUtils.isIntegerSchema(p)) {
            if (p.getDefault() != null) {
                return p.getDefault().toString();
            }
        } else if (ModelUtils.isDateSchema(p) || ModelUtils.isDateTimeSchema(p) || ModelUtils.isStringSchema(p)) {
            if (p.getDefault() != null) {
                return "\"" + p.getDefault().toString() + "\"";
            }
        }

        return null;
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        if (this.enumClassPrefix) {
            return datatype + "_" + value;
        }
        return value;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        String varName = super.toEnumVarName(name, datatype);
        return StringExtensions.camelize(varName.toLowerCase());
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationMap operationMap = objs.getOperations();
        List<CodegenOperation> operations = operationMap.getOperation();

        this.normalizeRouterData(operations);

        List<Map<String, String>> imports = objs.getImports();
        if (imports == null)
            return objs;

        this.removeModelImports(imports);

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

                this.setExportParameterName(param);
            }
        }

        return this.addRecursiveImports(objs, imports);
    }

    @Override
    public void processOpts() {
        this.setLegacyDiscriminatorBehavior(false);

        super.processOpts();

        this.sourceFolder = this.getAndUseAdditionalProperty(CodegenConstants.SOURCE_FOLDER, "src");
        this.apiFolder = this.getAndUseAdditionalProperty(OptionsConstants.API_FOLDER, "");
        this.modelFolder = this.getAndUseAdditionalProperty(OptionsConstants.MODEL_FOLDER, "");
        this.packageName = this.getAndUseAdditionalProperty(CodegenConstants.PACKAGE_NAME, "openapi");
        this.modelPackage = this.getAndUseAdditionalProperty(CodegenConstants.MODEL_PACKAGE, packageName);
        this.apiPackage = this.getAndUseAdditionalProperty(CodegenConstants.API_PACKAGE, packageName);
        this.enumClassPrefix = this.getAndUseAdditionalProperty(CodegenConstants.ENUM_CLASS_PREFIX, true);

        TemplatingEngineAdapter templatingEngine = this.getTemplatingEngine();
        if (!(templatingEngine instanceof HandlebarsEngineAdapter)) {
            throw new RuntimeException("Only the HandlebarsEngineAdapter is supported for this generator");
        }

        SupportingFile apiImpl = new SupportingFile("api-impl.hbs", this.sourceFolder, "api.go");
        SupportingFile modelImpl = new SupportingFile("model-impl.hbs", this.sourceFolder, "model.go");

        this.supportingFiles.add(apiImpl);
        this.supportingFiles.add(modelImpl);
    }

    @Override
    public String apiFileFolder() {
        return this.outputFolder + File.separator + this.sourceFolder + File.separator + this.apiFolder;
    }

    @Override
    public String modelFileFolder() {
        return this.outputFolder + File.separator + this.sourceFolder + File.separator + this.modelFolder;
    }
}
