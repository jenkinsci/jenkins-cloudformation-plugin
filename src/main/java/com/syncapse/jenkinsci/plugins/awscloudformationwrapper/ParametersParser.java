package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

class ParametersParser {

    public List<Parameter> parse(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().
            constructCollectionType(ArrayList.class, Parameter.class);
        return mapper.readValue(jsonString, type);
    }
}
