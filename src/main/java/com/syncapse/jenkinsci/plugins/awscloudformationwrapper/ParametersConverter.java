package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.common.collect.Lists;

class ParametersConverter {

    public List<Parameter> convert(Map<String, String> parameters) {
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
}
