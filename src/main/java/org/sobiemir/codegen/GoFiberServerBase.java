package org.sobiemir.codegen;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.languages.AbstractGoCodegen;
import org.openapitools.codegen.model.OperationsMap;

import static org.openapitools.codegen.utils.StringUtils.camelize;

public class GoFiberServerBase extends AbstractGoCodegen {
    @SuppressWarnings("unchecked")
    protected <T> T getAndUseAdditionalProperty(String propertyName, T defaultValue) {
        if (additionalProperties.containsKey(propertyName)) {
            if (defaultValue instanceof Boolean) {
                return (T) (Object) Boolean.parseBoolean((String) additionalProperties.get(propertyName));
            } else if (defaultValue instanceof Integer) {
                return (T) (Object) Integer.parseInt((String) additionalProperties.get(propertyName));
            }
            return (T) additionalProperties.get(propertyName);
        } else {
            additionalProperties.put(propertyName, defaultValue);
        }
        return defaultValue;
    }

    protected OperationsMap addRecursiveImports(OperationsMap objs, List<Map<String, String>> imports) {
        // recursively add import for mapping one type to multiple imports
        List<Map<String, String>> recursiveImports = objs.getImports();
        if (recursiveImports == null)
            return objs;

        ListIterator<Map<String, String>> listIterator = imports.listIterator();
        while (listIterator.hasNext()) {
            String _import = listIterator.next().get("import");
            // if the import package happens to be found in the importMapping (key)
            // add the corresponding import package to the list
            if (importMapping.containsKey(_import)) {
                listIterator.add(createMapping("import", importMapping.get(_import)));
            }
        }

        return objs;
    }

    protected void removeModelImports(List<Map<String, String>> imports) {
        Iterator<Map<String, String>> iterator = imports.iterator();
        while (iterator.hasNext()) {
            String _import = iterator.next().get("import");
            if (_import.startsWith(apiPackage()))
                iterator.remove();
        }
    }

    protected void normalizeRouterData(List<CodegenOperation> operations) {
        for (CodegenOperation operation : operations) {
            // http method verb conversion (e.g. PUT => Put)
            operation.httpMethod = camelize(operation.httpMethod.toLowerCase(Locale.ROOT));
            // replace {param} with :param
            if (operation.path != null) {
                operation.path = operation.path.replaceAll("\\{(.*?)\\}", ":$1");
            }
        }
    }

    protected void setExportParameterName(CodegenParameter param) {
        char nameFirstChar = param.paramName.charAt(0);
        if (Character.isUpperCase(nameFirstChar)) {
            // First char is already uppercase, just use paramName.
            param.vendorExtensions.put("x-export-param-name", param.paramName);
        } else {
            // It's a lowercase first char, let's convert it to uppercase
            StringBuilder sb = new StringBuilder(param.paramName);
            sb.setCharAt(0, Character.toUpperCase(nameFirstChar));
            param.vendorExtensions.put("x-export-param-name", sb.toString());
        }
    }
}
