package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParameterUtils {

    public static List<Parameter> merge(List<Parameter> first, List<Parameter> second) {
        Map<String, Parameter> result = new LinkedHashMap<String, Parameter>();

        for (Parameter parameter : first) {
            result.put(parameter.getParameterKey(), parameter);
        }

        for (Parameter parameter : second) {
            result.put(parameter.getParameterKey(), parameter);
        }

        return new ArrayList<Parameter>(result.values());
    }

    public static List<Parameter> convert(Map<String, String> parameters) {
        if (parameters == null) {
            return null;
        }

        List<Parameter> result = Lists.newArrayList();
        for (String name : parameters.keySet()) {
            Parameter parameter = new Parameter();
            parameter.setParameterKey(name);
            parameter.setParameterValue(parameters.get(name));
            result.add(parameter);
        }

        return result;
    }

    public static List<Parameter> parse(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().
            constructCollectionType(ArrayList.class, Parameter.class);
        return mapper.readValue(jsonString, type);
    }
}
